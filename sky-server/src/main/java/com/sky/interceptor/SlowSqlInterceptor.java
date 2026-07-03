package com.sky.interceptor;

import com.sky.entity.SlowSqlLog;
import com.sky.service.SlowSqlLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Properties;

@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        })
})
public class SlowSqlInterceptor implements Interceptor {

    @Value("${sky.sql-monitor.slow-threshold-ms:500}")
    private long slowThresholdMs;
    @Autowired
    private SlowSqlLogService slowSqlLogService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost >= slowThresholdMs) {
                MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
                Object parameter = invocation.getArgs()[1];
                BoundSql boundSql = mappedStatement.getBoundSql(parameter);
                String sql = normalizeSql(boundSql.getSql());
                if (sql.regionMatches(true, 0, "select", 0, "select".length())) {
                    String normalizedSql = normalizeLiterals(sql);
                    String sqlSignature = sha256(normalizedSql);
                    String mapperId = mappedStatement.getId();
                    log.warn("Slow SQL detected, mapper={}, cost={}ms, signature={}, sql={}",
                            mapperId, cost, sqlSignature, sql);
                    slowSqlLogService.saveAsync(SlowSqlLog.builder()
                            .mapperId(mapperId)
                            .sqlText(sql)
                            .normalizedSql(normalizedSql)
                            .sqlSignature(sqlSignature)
                            .costTime(cost)
                            .thresholdTime(slowThresholdMs)
                            .createTime(LocalDateTime.now())
                            .build());
                }
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    private String normalizeLiterals(String sql) {
        return sql
                .replaceAll("'[^']*'", "?")
                .replaceAll("\\b\\d+\\b", "?")
                .toLowerCase();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SQL signature", e);
        }
    }
}
