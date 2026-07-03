package com.sky.service;

import com.sky.dto.SqlOptimizationResultDTO;
import com.sky.entity.SqlOptimizationResult;

public interface SqlOptimizationResultService {

    void saveOrUpdate(SqlOptimizationResultDTO sqlOptimizationResultDTO);

    SqlOptimizationResult getBySignature(String sqlSignature);
}
