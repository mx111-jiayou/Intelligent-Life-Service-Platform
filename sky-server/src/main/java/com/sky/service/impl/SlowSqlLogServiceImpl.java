package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SlowSqlLogPageQueryDTO;
import com.sky.entity.SlowSqlLog;
import com.sky.mapper.SlowSqlLogMapper;
import com.sky.result.PageResult;
import com.sky.service.SlowSqlLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public PageResult pageQuery(SlowSqlLogPageQueryDTO slowSqlLogPageQueryDTO) {
        PageHelper.startPage(slowSqlLogPageQueryDTO.getPage(), slowSqlLogPageQueryDTO.getPageSize());
        Page<SlowSqlLog> page = slowSqlLogMapper.pageQuery(slowSqlLogPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public List<SlowSqlLog> listBySignature(String sqlSignature) {
        return slowSqlLogMapper.listBySignature(sqlSignature);
    }
}
