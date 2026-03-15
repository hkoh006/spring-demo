package org.example.crypto.exchange.messaging

import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.UserAlreadyHasActiveOrderException
import org.example.crypto.exchange.messaging.proto.OrderActionProto
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class OrderSubscriber(
    private val exchange: Exchange,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["orders"],
        groupId = "crypto-exchange-consumer-group-id",
    )
    fun consume(event: OrderEventProto) {
        when (event.action) {
            OrderActionProto.CANCEL -> {
                val result = exchange.cancelOrder(event.orderId)
                if (result == null) log.debug("Cancel of '${event.orderId}' had no effect — order not in book")
            }
            OrderActionProto.AMEND -> {
                val result = exchange.amendOrder(event.orderId, BigDecimal(event.price), BigDecimal(event.quantity))
                if (result == null) log.debug("Amend of '${event.orderId}' had no effect — order not in book")
            }
            else -> place(event)
        }
    }

    private fun place(event: OrderEventProto) {
        val order = OrderEntity(
            id = event.orderId.ifBlank { UUID.randomUUID().toString() },
            userId = event.userId,
            side = when (event.side) {
                OrderSideProto.BUY -> OrderSide.BUY
                OrderSideProto.SELL -> OrderSide.SELL
                else -> throw IllegalArgumentException("Unknown order side: ${event.side}")
            },
            price = BigDecimal(event.price),
            quantity = BigDecimal(event.quantity),
        )
        try {
            exchange.placeOrder(order)
        } catch (e: UserAlreadyHasActiveOrderException) {
            log.debug("Order rejected for user '${event.userId}': ${e.message}")
        }
    }
}
