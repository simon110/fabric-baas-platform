package com.anhui.fabricbaasttp.configuration;

import com.anhui.fabricbaasweb.bean.AntMatcherCollection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AntMatcherConfiguration {

    @Bean
    public AntMatcherCollection antMatcherCollection() {
        String[] matchers = {
                "/api/v1/organization/login",
                "/api/v1/organization/applyRegistration",
                "/download/**",
        };
        return new AntMatcherCollection(matchers);
    }
}
