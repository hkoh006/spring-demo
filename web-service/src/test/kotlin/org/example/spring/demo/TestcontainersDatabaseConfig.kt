package org.example.spring.demo

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.utility.DockerImageName

@Configuration
class TestcontainersDatabaseConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): org.testcontainers.postgresql.PostgreSQLContainer =
        org.testcontainers.postgresql
            .PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withDatabaseName("spring_demo")
            .withUsername("spring")
}
