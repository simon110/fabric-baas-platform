package com.anhui.fabricbaascommon.configuration;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:fabricbaascommon.properties")
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfiguration {
    private String username;
    private String password;
    private String host;
    private int port;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(host, port, false)
                .credentials(username, password)
                .build();
    }
}
