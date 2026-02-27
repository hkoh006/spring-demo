package org.example.crypto.exchange

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.PostgreSQLContainer

@Configuration
class TestcontainersPostgresConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer() = PostgreSQLContainer("postgres:17-alpine")
}
