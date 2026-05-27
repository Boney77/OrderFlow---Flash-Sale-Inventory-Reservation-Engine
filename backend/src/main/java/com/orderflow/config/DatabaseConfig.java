package com.orderflow.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "orderflow.datasource.cloud-configured", havingValue = "true")
    public DataSource cloudDataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException(
                    "Database URL is missing. On Railway: open your backend service → Variables → "
                            + "add DATABASE_PRIVATE_URL=${{Postgres.DATABASE_PRIVATE_URL}} "
                            + "(or DATABASE_URL=${{Postgres.DATABASE_URL}})."
            );
        }

        config.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        return new HikariDataSource(config);
    }

    static String toJdbcUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:")) {
            return databaseUrl;
        }
        if (databaseUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
        }
        if (databaseUrl.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + databaseUrl.substring("postgresql://".length());
        }
        throw new IllegalArgumentException("Unsupported database URL format: " + databaseUrl);
    }
}
