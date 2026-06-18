package com.interview_platform_backend.interview_platform_backend.codeexecution.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DockerConfig {

    @Value("${code-execution.docker.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    @Value("${code-execution.docker.connection-timeout-ms:5000}")
    private int connectionTimeout;

    @Value("${code-execution.docker.response-timeout-ms:30000}")
    private int responseTimeout;

    @Bean
    public DockerClient dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofMillis(connectionTimeout))
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .maxConnections(100)
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
