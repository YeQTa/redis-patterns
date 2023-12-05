package com.aksoy.redispatterns.bulk;

import com.aksoy.redispatterns.configuration.IT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.testcontainers.shaded.com.google.common.base.Stopwatch;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
@IT
class RedisBulkOperationIT {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final int NUM_OF_OPERATION = 50000;
    private static final String KEY = "KEY-1";
    private static final String VALUE = "VALUE-1";

    private RedisSerializer<String> serializer;

    @PostConstruct
    void setup() {
        serializer = redisTemplate.getStringSerializer();
    }

    // ~709 ms
    @Test
    void bulk_insert_with_multi_exec_transaction() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        redisTemplate.execute((RedisCallback<List<Object>>) connection -> {
            connection.multi();
            IntStream.range(0, NUM_OF_OPERATION).forEach(i -> connection.stringCommands().setEx(
                    Objects.requireNonNull(serializer.serialize(KEY)),
                    60,
                    Objects.requireNonNull(serializer.serialize(VALUE))));
            return connection.exec();
        });

        log.info("Elapsed Time: {}", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        assertEquals(VALUE, redisTemplate.opsForValue().get(KEY));
    }

    // ~430 ms
    @Test
    void bulk_insert_with_pipeline_strategy() {
        log.info("Current redis db size: {}", Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().dbSize());
        Stopwatch stopwatch = Stopwatch.createStarted();

        redisTemplate.executePipelined((RedisConnection connection) -> {
            IntStream.range(0, NUM_OF_OPERATION).forEach(i -> connection.stringCommands().setEx(
                    Objects.requireNonNull(serializer.serialize(KEY)),
                    60,
                    Objects.requireNonNull(serializer.serialize(VALUE))));
            return null;
        });

        log.info("Elapsed Time: {}", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        assertEquals(VALUE, redisTemplate.opsForValue().get(KEY));
    }

    //~17149 ms
    @Test
    void sequential_insert() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        IntStream.range(0, NUM_OF_OPERATION).forEach(i -> redisTemplate.opsForValue().set(KEY, VALUE, Duration.ofMinutes(1)));

        log.info("Elapsed Time: {}", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        assertEquals(VALUE, redisTemplate.opsForValue().get(KEY));
    }
}
