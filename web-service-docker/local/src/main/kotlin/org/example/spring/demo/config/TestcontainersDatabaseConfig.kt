package org.example.spring.demo.config

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@Configuration
class TestcontainersDatabaseConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName("test")
            .withUsername("test")
}
