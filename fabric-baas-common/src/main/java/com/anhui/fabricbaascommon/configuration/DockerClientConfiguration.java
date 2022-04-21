package com.anhui.fabricbaascommon.configuration;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class DockerClientConfiguration {
    @Bean
    public DockerClient dockerClient() throws DockerException, InterruptedException {
        DockerClient dockerClient = new DefaultDockerClient("unix:///var/run/docker.sock");
        log.info("正在检查当前服务器的容器状态...");
        List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers());
        containers.forEach(container -> log.info(container.toString()));
        return dockerClient;
    }
}

