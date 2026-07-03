package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.CacheConstant;
import com.sky.dto.SqlOptimizationResultDTO;
import com.sky.entity.SqlOptimizationResult;
import com.sky.mapper.SqlOptimizationResultMapper;
import com.sky.service.SqlOptimizationResultService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class SqlOptimizationResultServiceImpl implements SqlOptimizationResultService {

    @Autowired
    private SqlOptimizationResultMapper sqlOptimizationResultMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveOrUpdate(SqlOptimizationResultDTO sqlOptimizationResultDTO) {
        SqlOptimizationResult sqlOptimizationResult = new SqlOptimizationResult();
        BeanUtils.copyProperties(sqlOptimizationResultDTO, sqlOptimizationResult);
        LocalDateTime now = LocalDateTime.now();
        SqlOptimizationResult exists = sqlOptimizationResultMapper.getBySignature(sqlOptimizationResult.getSqlSignature());
        if (exists == null) {
            sqlOptimizationResult.setCreateTime(now);
            sqlOptimizationResult.setUpdateTime(now);
            sqlOptimizationResultMapper.insert(sqlOptimizationResult);
        } else {
            sqlOptimizationResult.setUpdateTime(now);
            sqlOptimizationResultMapper.update(sqlOptimizationResult);
        }
        cacheResult(sqlOptimizationResult);
    }

    @Override
    public SqlOptimizationResult getBySignature(String sqlSignature) {
        String key = CacheConstant.SQL_OPTIMIZATION_PREFIX + sqlSignature;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return JSON.parseObject(cached, SqlOptimizationResult.class);
        }
        SqlOptimizationResult sqlOptimizationResult = sqlOptimizationResultMapper.getBySignature(sqlSignature);
        if (sqlOptimizationResult != null) {
            cacheResult(sqlOptimizationResult);
        }
        return sqlOptimizationResult;
    }

    private void cacheResult(SqlOptimizationResult sqlOptimizationResult) {
        String key = CacheConstant.SQL_OPTIMIZATION_PREFIX + sqlOptimizationResult.getSqlSignature();
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(sqlOptimizationResult), 12, TimeUnit.HOURS);
    }
}
