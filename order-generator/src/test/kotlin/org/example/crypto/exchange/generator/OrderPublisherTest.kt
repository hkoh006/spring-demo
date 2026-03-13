package org.example.crypto.exchange.generator

import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate

/**
 * Pure unit tests for [OrderPublisher].
 *
 * Verifies that [KafkaTemplate.send] is called with the correct topic,
 * key (userId), and event payload.
 */
@ExtendWith(MockitoExtension::class)
class OrderPublisherTest {
    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderEventProto>

    private val props = OrderGeneratorProperties(topic = "orders")

    private lateinit var publisher: OrderPublisher

    @BeforeEach
    fun setUp() {
        publisher = OrderPublisher(kafkaTemplate, props)
    }

    @Test
    fun `should send event to configured topic with userId as key`() {
        val event =
            OrderEventProto
                .newBuilder()
                .setUserId("alice")
                .setSide(OrderSideProto.BUY)
                .setPrice("100.00")
                .setQuantity("5.00")
                .build()

        publisher.publish(event)

        verify(kafkaTemplate).send("orders", "alice", event)
    }

    @Test
    fun `should use topic from properties`() {
        val customProps = OrderGeneratorProperties(topic = "custom-orders")
        val customPublisher = OrderPublisher(kafkaTemplate, customProps)
        val event =
            OrderEventProto
                .newBuilder()
                .setUserId("bob")
                .setSide(OrderSideProto.SELL)
                .setPrice("99.00")
                .setQuantity("2.00")
                .build()

        customPublisher.publish(event)

        verify(kafkaTemplate).send("custom-orders", "bob", event)
    }
}
