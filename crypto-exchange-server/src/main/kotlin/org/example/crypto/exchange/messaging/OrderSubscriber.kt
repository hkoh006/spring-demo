package org.example.crypto.exchange.messaging

import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.Order
import org.example.crypto.exchange.OrderEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderSubscriber(
    private val exchange: Exchange,
) {
    @KafkaListener(
        topics = ["orders"],
        groupId = "crypto-exchange-consumer-group-id",
    )
    fun consume(event: OrderEvent) {
        val order =
            Order(
                userId = event.userId,
                side = event.side,
                price = event.price,
                quantity = event.quantity,
            )
        exchange.placeOrder(order)
    }
}
