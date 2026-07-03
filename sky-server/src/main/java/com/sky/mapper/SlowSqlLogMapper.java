package com.sky.mapper;

import com.sky.entity.SlowSqlLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlowSqlLogMapper {

    void insert(SlowSqlLog slowSqlLog);
}
