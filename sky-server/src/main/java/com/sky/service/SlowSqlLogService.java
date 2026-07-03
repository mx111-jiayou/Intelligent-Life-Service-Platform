package com.sky.service;

import com.sky.entity.SlowSqlLog;

public interface SlowSqlLogService {

    void saveAsync(SlowSqlLog slowSqlLog);
}
