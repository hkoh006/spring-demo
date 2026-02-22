package org.example.crypto.exchange.messaging

import org.awaitility.Awaitility.await
import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEvent
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.TestApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.testcontainers.kafka.KafkaContainer
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@SpringBootTest(
    classes = [TestApplication::class],
    properties = [
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    ],
)
class OrderSubscriberTest {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var exchange: Exchange

    @Test
    fun `should place order when OrderEvent is received from Kafka`() {
        // Given
        val userId = "user-123"
        val side = OrderSide.BUY
        val price = BigDecimal("100.50")
        val quantity = BigDecimal("1.5")
        val event = OrderEvent(userId, side, price, quantity)

        // When
        kafkaTemplate.send("orders", event)

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            val orderBook = exchange.getOrderBook()
            val bids = orderBook.bids
            assertEquals(1, bids.size, "Should have 1 bid in the order book")
            val placedOrder = bids.first()
            assertEquals(userId, placedOrder.userId)
            assertEquals(side, placedOrder.side)
            assertEquals(price, placedOrder.price)
            assertEquals(quantity, placedOrder.quantity)
        }
    }
}

@Configuration
class TestcontainersKafkaConfig {
    @Bean
    @ServiceConnection
    fun kafkaContainer() = KafkaContainer("apache/kafka-native:3.8.0")
}
