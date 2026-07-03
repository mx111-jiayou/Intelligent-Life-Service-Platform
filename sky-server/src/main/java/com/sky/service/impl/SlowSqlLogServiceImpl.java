package com.sky.service.impl;

import com.sky.entity.SlowSqlLog;
import com.sky.mapper.SlowSqlLogMapper;
import com.sky.service.SlowSqlLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SlowSqlLogServiceImpl implements SlowSqlLogService {

    @Autowired
    private SlowSqlLogMapper slowSqlLogMapper;

    @Async("slowSqlTaskExecutor")
    @Override
    public void saveAsync(SlowSqlLog slowSqlLog) {
        try {
            slowSqlLogMapper.insert(slowSqlLog);
        } catch (Exception e) {
            log.warn("Save slow SQL log failed, mapper={}, cost={}ms",
                    slowSqlLog.getMapperId(), slowSqlLog.getCostTime(), e);
        }
    }
}
