package com.sky.interceptor;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
                    log.warn("Slow SQL detected, mapper={}, cost={}ms, sql={}",
                            mappedStatement.getId(), cost, sql);
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
}
