package com.sky.utils;

import com.sky.constant.CacheConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1704067200L;
    private static final int COUNT_BITS = 32;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd");

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public long nextOrderId() {
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.of("+8"));
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        String date = now.format(DATE_FORMATTER);
        Long count = stringRedisTemplate.opsForValue().increment(CacheConstant.ORDER_ID_PREFIX + date);
        return timestamp << COUNT_BITS | count;
    }
}
