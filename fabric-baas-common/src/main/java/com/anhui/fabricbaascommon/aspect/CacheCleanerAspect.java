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
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class CacheCleanerAspect {
    private static final LocalVariableTableParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();
    private static final ExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 解析SpEL表达式（主要作用是把参数填进表达式中）
    private String parsePattern(String key, Method method, Object[] args) {
        String[] argNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (argNames != null) {
            assert argNames.length == args.length;
            for (int i = 0; i < argNames.length; i++) {
                context.setVariable(argNames[i], args[i]);
            }
        }
        Expression expression = SPEL_EXPRESSION_PARSER.parseExpression(key);
        return expression.getValue(context, String.class);
    }

    @AfterReturning("@annotation(com.anhui.fabricbaascommon.annotation.CacheClean)")
    public void clean(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        Object[] args = point.getArgs();
        CacheClean cacheClean = method.getAnnotation(CacheClean.class);
        Class<?> declaringClass = method.getDeclaringClass();
        CacheConfig cacheConfig = declaringClass.getAnnotation(CacheConfig.class);
        String[] classCacheNames = cacheConfig.cacheNames();
        String[] patterns = cacheClean.patterns();
        for (String pattern : patterns) {
            pattern = parsePattern(pattern, method, args);
            for (String cacheName : classCacheNames) {
                RedisUtils.deleteAll(redisTemplate, cacheName, pattern);
            }
        }
    }
}
