package com.aksoy.redispatterns.configuration;

import com.redis.testcontainers.RedisContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@TestConfiguration
@TestExecutionListeners(listeners = RedisCleanupListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class RedisTestContainer {

    private static RedisContainer redisContainer;
    private static final int PORT = 6379;

    @PostConstruct
    public void startRedis() {
        redisContainer = new RedisContainer(DockerImageName.parse("redis:latest"))
                .withExposedPorts(PORT);
        redisContainer.start();
    }

    @PreDestroy
    public void stopRedis() {
        redisContainer.stop();
    }

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(
                redisContainer.getHost(),
                redisContainer.getMappedPort(PORT)
        );

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfig);
        lettuceConnectionFactory.setPipeliningFlushPolicy(LettuceConnection.PipeliningFlushPolicy.flushOnClose());
        return lettuceConnectionFactory;
    }


    @Bean
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setDefaultSerializer(redisTemplate.getStringSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
