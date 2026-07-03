package com.sky.service;

public interface AiToolCallLogService {

    void save(Long userId, String sessionId, String toolName, String toolParams, String toolResult, boolean success);
}
