package com.anhui.fabricbaascommon.aspect;

import com.anhui.fabricbaascommon.annotation.CacheClean;
import com.anhui.fabricbaascommon.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class CacheCleanerAspect {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterReturning("@annotation(com.anhui.fabricbaascommon.annotation.CacheClean)")
    public void clean(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        CacheClean cacheClean = method.getAnnotation(CacheClean.class);
        Class<?> declaringClass = method.getDeclaringClass();
        CacheConfig cacheConfig = declaringClass.getAnnotation(CacheConfig.class);
        String[] classCacheNames = cacheConfig.cacheNames();
        String[] patterns = cacheClean.patterns();
        for (String cacheName : classCacheNames) {
            for (String pattern : patterns) {
                RedisUtils.deleteAll(redisTemplate, cacheName, pattern);
            }
        }
    }
}
