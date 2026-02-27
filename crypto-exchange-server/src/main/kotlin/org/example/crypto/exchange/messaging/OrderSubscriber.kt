package org.example.crypto.exchange.messaging

import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.Order
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OrderSubscriber(
    private val exchange: Exchange,
) {
    @KafkaListener(
        topics = ["orders"],
        groupId = "crypto-exchange-consumer-group-id",
    )
    fun consume(event: OrderEventProto) {
        val order =
            Order(
                userId = event.userId,
                side =
                    when (event.side) {
                        OrderSideProto.BUY -> OrderSide.BUY
                        OrderSideProto.SELL -> OrderSide.SELL
                        else -> throw IllegalArgumentException("Unknown order side: ${event.side}")
                    },
                price = BigDecimal(event.price),
                quantity = BigDecimal(event.quantity),
            )
        exchange.placeOrder(order)
    }
}
