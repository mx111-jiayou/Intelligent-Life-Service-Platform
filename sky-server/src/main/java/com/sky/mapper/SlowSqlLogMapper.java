package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.SlowSqlLogPageQueryDTO;
import com.sky.entity.SlowSqlLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SlowSqlLogMapper {

    void insert(SlowSqlLog slowSqlLog);

    Page<SlowSqlLog> pageQuery(SlowSqlLogPageQueryDTO slowSqlLogPageQueryDTO);

    @Select("select * from slow_sql_log where sql_signature = #{sqlSignature} order by create_time desc limit 20")
    List<SlowSqlLog> listBySignature(String sqlSignature);
}
