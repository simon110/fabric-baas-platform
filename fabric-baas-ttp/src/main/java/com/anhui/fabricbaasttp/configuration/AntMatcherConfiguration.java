package com.anhui.fabricbaasttp.configuration;

import com.anhui.fabricbaasweb.bean.AntMatchers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AntMatcherConfiguration {

    @Bean
    public AntMatchers antMatcherCollection() {
        String[] matchers = {
                "/api/v1/organization/login",
                "/api/v1/organization/applyRegistration",
                "/download/**",
        };
        return new AntMatchers(matchers);
    }
}
