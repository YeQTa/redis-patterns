package com.aksoy.redispatterns.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.Objects;

@Slf4j
@Component
public class RedisCleanupListener implements TestExecutionListener {


    @Override
    public void afterTestMethod(TestContext testContext) {
        // Clear Redis cache after each test
        RedisTemplate<String, String> redisTemplate = testContext.getApplicationContext().getBean(StringRedisTemplate.class);
        final RedisConnection redisConnection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection();
        log.debug("Current redis db size: {}", redisConnection.serverCommands().dbSize());
        log.info("Cleaning redis for: {}", testContext.getTestMethod().getName());
        redisConnection.serverCommands().flushDb();
        log.debug("After cleaning, redis db size: {}", redisConnection.serverCommands().dbSize());
    }

}