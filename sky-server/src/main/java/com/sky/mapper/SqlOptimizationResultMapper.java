package com.sky.mapper;

import com.sky.entity.SqlOptimizationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SqlOptimizationResultMapper {

    void insert(SqlOptimizationResult sqlOptimizationResult);

    void update(SqlOptimizationResult sqlOptimizationResult);

    @Select("select * from sql_optimization_result where sql_signature = #{sqlSignature} limit 1")
    SqlOptimizationResult getBySignature(String sqlSignature);
}
