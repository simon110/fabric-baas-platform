package com.anhui.fabricbaasorg.configuration;

import com.anhui.fabricbaasweb.bean.AntMatchers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AntMatcherConfiguration {

    @Bean
    public AntMatchers antMatcherCollection() {
        String[] matchers = {
                "/api/v1/user/login",
                "/download/**",
        };
        return new AntMatchers(matchers);
    }
}
