package com.orderflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;

@Configuration
public class RedisUrlConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactoryFromUrl(
            @Value("${REDIS_URL:}") String redisUrl,
            Environment environment) {
        if (redisUrl == null || redisUrl.isBlank() || redisUrl.contains("{{")) {
            return fallbackRedisConnectionFactory(environment);
        }

        URI uri = URI.create(redisUrl);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(uri.getHost());
        redisConfig.setPort(uri.getPort() == -1 ? 6379 : uri.getPort());

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            if (parts.length == 2 && !parts[0].isBlank()) {
                redisConfig.setUsername(parts[0]);
            }
            redisConfig.setPassword(RedisPassword.of(parts.length == 2 ? parts[1] : parts[0]));
        }

        String path = uri.getPath();
        if (path != null && path.length() > 1) {
            redisConfig.setDatabase(Integer.parseInt(path.substring(1)));
        }

        LettuceClientConfiguration clientConfig = "rediss".equalsIgnoreCase(uri.getScheme())
                ? LettuceClientConfiguration.builder().useSsl().build()
                : LettuceClientConfiguration.defaultConfiguration();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    private RedisConnectionFactory fallbackRedisConnectionFactory(Environment environment) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(environment.getProperty("spring.data.redis.host", "localhost"));
        redisConfig.setPort(environment.getProperty("spring.data.redis.port", Integer.class, 6379));

        String username = environment.getProperty("spring.data.redis.username");
        if (username != null && !username.isBlank()) {
            redisConfig.setUsername(username);
        }

        String password = environment.getProperty("spring.data.redis.password");
        if (password != null && !password.isBlank()) {
            redisConfig.setPassword(RedisPassword.of(password));
        }

        Integer database = environment.getProperty("spring.data.redis.database", Integer.class);
        if (database != null) {
            redisConfig.setDatabase(database);
        }

        boolean useSsl = environment.getProperty("spring.data.redis.ssl.enabled", Boolean.class, false);
        LettuceClientConfiguration clientConfig = useSsl
                ? LettuceClientConfiguration.builder().useSsl().build()
                : LettuceClientConfiguration.defaultConfiguration();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
}
