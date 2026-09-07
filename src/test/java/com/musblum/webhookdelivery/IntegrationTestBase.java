package com.musblum.webhookdelivery;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class IntegrationTestBase {

    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    static final GenericContainer<?> valkey =
            new GenericContainer<>("valkey/valkey:8")
                    .withExposedPorts(6379);

    static {
        postgres.start();
        valkey.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add(
                "spring.data.redis.port",
                () -> valkey.getMappedPort(6379)
        );
    }
}