package com.anhui.fabricbaascommon.util;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

public class RedisUtils {
    public static void deleteAll(RedisTemplate<String, Object> redisTemplate, String cacheName, String pattern) {
        String actualPattern = cacheName + ':' + pattern;
        System.out.println(actualPattern);
        Set<String> keys = redisTemplate.keys(actualPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
