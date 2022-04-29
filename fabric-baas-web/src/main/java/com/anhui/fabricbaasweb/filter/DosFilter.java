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
@ConfigurationProperties(prefix = "dos")
public class DosFilter extends OncePerRequestFilter {
    private int requestLimit;
    private long resetInterval;
    private long lastResetTime = 0;

    private final HashMap<String, Integer> counter = new HashMap<>();

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime > resetInterval) {
            lastResetTime = now;
            counter.clear();
            log.info("请求限制已重置");
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        refresh();
        String addr = request.getRemoteAddr();
        int count = counter.getOrDefault(addr, 0);
        if (count < requestLimit) {
            counter.put(addr, count + 1);
            super.doFilter(request, response, filterChain);
        }
    }
}
