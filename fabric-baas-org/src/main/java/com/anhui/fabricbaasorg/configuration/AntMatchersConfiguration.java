package com.anhui.fabricbaasorg.configuration;

import com.anhui.fabricbaasweb.bean.AntMatchers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AntMatchersConfiguration {

    @Bean
    public AntMatchers antMatchers() {
        String[] matchers = {
                "/api/v1/user/login",
                "/download/**",
        };
        return new AntMatchers(matchers);
    }
}
