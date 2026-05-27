package com.orderflow.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Railway / cloud Postgres env vars to Spring datasource properties before auto-config runs.
 */
public class RailwayEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE = "orderflowCloudDatasource";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isRailwayRuntime(environment)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", firstNonBlank(environment.getProperty("PORT"), "8080"));

        String jdbcUrl = resolveJdbcUrl(environment);
        if (jdbcUrl == null) {
            throw new IllegalStateException(
                    "Running on Railway but PostgreSQL is not linked to this backend service. "
                            + "Open backend → Variables → Add Reference → Postgres, then add: "
                            + "DATABASE_PRIVATE_URL=${{Postgres.DATABASE_PRIVATE_URL}} "
                            + "(or POSTGRES_HOST=${{Postgres.PGHOST}} plus POSTGRES_PORT/USER/PASSWORD/DB)."
            );
        }

        properties.put("spring.datasource.url", jdbcUrl);
        properties.put("orderflow.datasource.cloud-configured", "true");

        String username = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("PGUSER"),
                environment.getProperty("POSTGRES_USER")
        );
        String password = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("PGPASSWORD"),
                environment.getProperty("POSTGRES_PASSWORD")
        );

        if (username != null) {
            properties.put("spring.datasource.username", username);
        }
        if (password != null) {
            properties.put("spring.datasource.password", password);
        }

        if (!hasRedisConfig(environment)) {
            throw new IllegalStateException(
                    "Running on Railway but Redis is not linked. "
                            + "Add Redis, then set REDIS_URL=${{Redis.REDIS_URL}} on the backend."
            );
        }

        System.out.println("[OrderFlow] Railway datasource configured (host from env, not localhost)");
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE, properties));
    }

    private static boolean hasRedisConfig(ConfigurableEnvironment environment) {
        if (isSet(environment.getProperty("REDIS_URL"))) {
            return true;
        }
        String redisHost = environment.getProperty("REDISHOST");
        if (isSet(redisHost) && !"localhost".equalsIgnoreCase(redisHost)) {
            return true;
        }
        String redisHostLegacy = environment.getProperty("REDIS_HOST");
        return isSet(redisHostLegacy) && !"localhost".equalsIgnoreCase(redisHostLegacy);
    }

    private static boolean isRailwayRuntime(ConfigurableEnvironment environment) {
        return isSet(environment.getProperty("RAILWAY_ENVIRONMENT"))
                || isSet(environment.getProperty("RAILWAY_PROJECT_ID"))
                || isSet(environment.getProperty("RAILWAY_SERVICE_ID"))
                || isSet(environment.getProperty("RAILWAY_REPLICA_ID"));
    }

    private static String resolveJdbcUrl(ConfigurableEnvironment environment) {
        String explicitJdbc = environment.getProperty("SPRING_DATASOURCE_URL");
        if (isSet(explicitJdbc) && !explicitJdbc.contains("localhost")) {
            return toJdbcUrlIfNeeded(explicitJdbc);
        }

        String privateUrl = environment.getProperty("DATABASE_PRIVATE_URL");
        if (isSet(privateUrl)) {
            return DatabaseConfig.toJdbcUrl(privateUrl);
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (isSet(databaseUrl)) {
            return DatabaseConfig.toJdbcUrl(databaseUrl);
        }

        String pgHost = firstNonBlank(
                environment.getProperty("PGHOST"),
                environment.getProperty("POSTGRES_HOST")
        );
        if (isSet(pgHost) && !"localhost".equalsIgnoreCase(pgHost)) {
            String pgPort = defaultIfBlank(
                    firstNonBlank(environment.getProperty("PGPORT"), environment.getProperty("POSTGRES_PORT")),
                    "5432"
            );
            String pgDatabase = defaultIfBlank(
                    firstNonBlank(environment.getProperty("PGDATABASE"), environment.getProperty("POSTGRES_DB")),
                    "railway"
            );
            return "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase;
        }

        return null;
    }

    private static String toJdbcUrlIfNeeded(String url) {
        if (url.startsWith("jdbc:")) {
            return url;
        }
        return DatabaseConfig.toJdbcUrl(url);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isSet(value)) {
                return value;
            }
        }
        return null;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return isSet(value) ? value : defaultValue;
    }
}
