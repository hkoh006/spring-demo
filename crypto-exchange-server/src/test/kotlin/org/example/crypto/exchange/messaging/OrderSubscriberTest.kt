package org.example.crypto.exchange.messaging

import org.assertj.core.api.Assertions
import org.awaitility.Awaitility.await
import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.Order
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.TestApplication
import org.example.crypto.exchange.TestcontainersPostgresConfig
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@SpringBootTest(
    classes = [TestApplication::class],
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer",
        "spring.kafka.consumer.properties.schema.registry.url=mock://order_subscriber_test",
        "spring.kafka.consumer.properties.auto.register.schemas=true",
        "spring.kafka.consumer.properties.specific.protobuf.value.type=org.example.crypto.exchange.messaging.proto.OrderEventProto",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer",
        "spring.kafka.producer.properties.schema.registry.url=mock://order_subscriber_test",
        "spring.kafka.producer.properties.auto.register.schemas=true",
    ],
)
@Import(TestcontainersPostgresConfig::class)
class OrderSubscriberTest {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var exchange: Exchange

    @Test
    fun `should place order when OrderEvent is received from Kafka`() {
        // Given
        val event =
            OrderEventProto
                .newBuilder()
                .setUserId("user-123")
                .setSide(OrderSideProto.BUY)
                .setPrice("100.50")
                .setQuantity("1.5")
                .build()

        // When
        kafkaTemplate.send("orders", event)

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            val orderBook = exchange.getOrderBook()
            val bids = orderBook.bids
            Assertions.assertThat(bids.size).isEqualTo(1)

            Assertions
                .assertThat(bids.first())
                .usingRecursiveComparison()
                .ignoringFields(Order::id.name, Order::timestamp.name)
                .isEqualTo(
                    Order(
                        id = "IGNORED",
                        userId = "user-123",
                        side = OrderSide.BUY,
                        price = BigDecimal("100.50"),
                        quantity = BigDecimal("1.5"),
                    ),
                )
        }
    }
}
