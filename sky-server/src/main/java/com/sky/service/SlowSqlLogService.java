package com.sky.service;

import com.sky.dto.SlowSqlLogPageQueryDTO;
import com.sky.entity.SlowSqlLog;
import com.sky.result.PageResult;

import java.util.List;

public interface SlowSqlLogService {

    void saveAsync(SlowSqlLog slowSqlLog);

    PageResult pageQuery(SlowSqlLogPageQueryDTO slowSqlLogPageQueryDTO);

    List<SlowSqlLog> listBySignature(String sqlSignature);
}
