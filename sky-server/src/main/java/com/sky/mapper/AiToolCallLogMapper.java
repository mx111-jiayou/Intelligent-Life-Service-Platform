package com.sky.mapper;

import com.sky.entity.AiToolCallLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiToolCallLogMapper {

    void insert(AiToolCallLog aiToolCallLog);
}
