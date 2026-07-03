package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiChatDTO implements Serializable {

    private String sessionId;

    private String message;
}
