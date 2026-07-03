package com.sky.service.impl;

import com.sky.entity.AiToolCallLog;
import com.sky.mapper.AiToolCallLogMapper;
import com.sky.service.AiToolCallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiToolCallLogServiceImpl implements AiToolCallLogService {

    @Autowired
    private AiToolCallLogMapper aiToolCallLogMapper;

    @Override
    public void save(Long userId, String sessionId, String toolName, String toolParams, String toolResult, boolean success) {
        AiToolCallLog aiToolCallLog = AiToolCallLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .toolName(toolName)
                .toolParams(toolParams)
                .toolResult(toolResult)
                .success(success ? 1 : 0)
                .createTime(LocalDateTime.now())
                .build();
        aiToolCallLogMapper.insert(aiToolCallLog);
    }
}
