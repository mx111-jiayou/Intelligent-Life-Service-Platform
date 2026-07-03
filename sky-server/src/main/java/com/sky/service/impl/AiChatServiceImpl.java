package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.CacheConstant;
import com.sky.context.BaseContext;
import com.sky.dto.AiChatDTO;
import com.sky.entity.ChatMessage;
import com.sky.entity.KnowledgeDoc;
import com.sky.mapper.ChatMessageMapper;
import com.sky.service.AiChatService;
import com.sky.service.KnowledgeDocService;
import com.sky.service.OrderService;
import com.sky.vo.AiChatVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final int MEMORY_LIMIT = 10;

    @Autowired
    private KnowledgeDocService knowledgeDocService;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private OrderService orderService;

    @Override
    public AiChatVO chat(AiChatDTO aiChatDTO) {
        Long userId = BaseContext.getCurrentId();
        String sessionId = StringUtils.isBlank(aiChatDTO.getSessionId())
                ? UUID.randomUUID().toString()
                : aiChatDTO.getSessionId();
        String message = StringUtils.trimToEmpty(aiChatDTO.getMessage());

        saveMessage(userId, sessionId, "user", message);
        String toolReply = tryCallOrderTool(message);
        List<KnowledgeDoc> docs = knowledgeDocService.searchEnabled(message, 3);
        List<String> references = docs.stream()
                .map(KnowledgeDoc::getTitle)
                .collect(Collectors.toList());
        String reply = StringUtils.isNotBlank(toolReply) ? toolReply : buildReply(docs);
        saveMessage(userId, sessionId, "assistant", reply);

        return AiChatVO.builder()
                .sessionId(sessionId)
                .reply(reply)
                .references(references)
                .build();
    }

    private String tryCallOrderTool(String message) {
        if (StringUtils.contains(message, "最近") && StringUtils.contains(message, "订单")) {
            return orderService.queryLatestOrderSummary();
        }
        if (StringUtils.contains(message, "取消") && StringUtils.contains(message, "订单")) {
            Long orderId = extractFirstNumber(message);
            if (orderId == null) {
                return "请提供需要取消的订单ID，例如：取消订单 123。";
            }
            return orderService.cancelOrderForAi(orderId, "AI客服用户申请取消");
        }
        return null;
    }

    private Long extractFirstNumber(String message) {
        Matcher matcher = Pattern.compile("\\d+").matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group());
    }

    private String buildReply(List<KnowledgeDoc> docs) {
        if (docs == null || docs.isEmpty()) {
            return "我暂时没有在客服知识库中找到匹配规则，已记录你的问题，后续可接入大模型进一步生成回答。";
        }
        StringBuilder builder = new StringBuilder("根据客服知识库，参考以下规则：");
        for (KnowledgeDoc doc : docs) {
            builder.append("\n【").append(doc.getTitle()).append("】")
                    .append(abbreviate(doc.getContent(), 160));
        }
        return builder.toString();
    }

    private void saveMessage(Long userId, String sessionId, String role, String content) {
        ChatMessage chatMessage = ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .createTime(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(chatMessage);

        String key = CacheConstant.CHAT_MEMORY_PREFIX + userId + ":" + sessionId;
        stringRedisTemplate.opsForList().rightPush(key, JSON.toJSONString(chatMessage));
        stringRedisTemplate.opsForList().trim(key, -MEMORY_LIMIT, -1);
        stringRedisTemplate.expire(key, 2, TimeUnit.HOURS);
    }

    private String abbreviate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
