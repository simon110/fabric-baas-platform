package com.anhui.fabricbaasweb.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

@Component
@Slf4j
@Setter
@Getter
@ConfigurationProperties(prefix = "denial")
public class DenialFilter extends OncePerRequestFilter {
    private int requestLimit;
    private long resetInterval;
    private long lastResetTime = 0;

    private final HashMap<String, Integer> recorder = new HashMap<>();

    @SuppressWarnings("NullableProblems")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (now - lastResetTime > resetInterval) {
            lastResetTime = now;
            recorder.clear();
            log.info("请求限制已重置");
        }

        String addr = request.getRemoteAddr();
        int count = recorder.getOrDefault(addr, 0);
        if (count < requestLimit) {
            recorder.put(addr, count + 1);
            super.doFilter(request, response, filterChain);
        } else {
            log.info("请求次数超过限制：" + addr);
        }
    }
}
