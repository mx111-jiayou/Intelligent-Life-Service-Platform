package com.sky.controller.user;

import com.sky.dto.AiChatDTO;
import com.sky.result.Result;
import com.sky.service.AiChatService;
import com.sky.vo.AiChatVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/ai")
@Api(tags = "AI客服接口")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    @ApiOperation("AI客服对话")
    public Result<AiChatVO> chat(@RequestBody AiChatDTO aiChatDTO) {
        return Result.success(aiChatService.chat(aiChatDTO));
    }
}
