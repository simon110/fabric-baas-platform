package com.anhui.fabricbaasweb.configuration;

import com.anhui.fabricbaasweb.util.JwtUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @PropertySource("classpath:fabricbaasweb.properties")
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtUtilsConfiguration {
    private String header;
    private String secret;
    private Long expiration;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(expiration, secret, header);
    }
}
