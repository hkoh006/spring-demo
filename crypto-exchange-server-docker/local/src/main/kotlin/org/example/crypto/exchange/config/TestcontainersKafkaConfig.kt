package org.example.crypto.exchange.config

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.kafka.KafkaContainer

@Configuration
class TestcontainersKafkaConfig {
    @Bean
    @ServiceConnection
    fun kafkaContainer() = KafkaContainer("apache/kafka-native:3.8.0")
}
