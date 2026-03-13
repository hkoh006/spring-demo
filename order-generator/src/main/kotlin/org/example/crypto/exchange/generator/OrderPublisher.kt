package org.example.crypto.exchange.generator

import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderPublisher(
    @Qualifier("orderKafkaTemplate")
    private val kafkaTemplate: KafkaTemplate<String, OrderEventProto>,
    private val props: OrderGeneratorProperties,
) {
    fun publish(event: OrderEventProto) {
        kafkaTemplate.send(props.topic, event.userId, event)
    }
}
