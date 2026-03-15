package org.example.crypto.exchange.generator

import org.example.crypto.exchange.messaging.proto.OrderActionProto
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.example.crypto.exchange.messaging.proto.OrderTypeProto
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@Component
class OrderGeneratorService(
    private val publisher: OrderPublisher,
    private val props: OrderGeneratorProperties,
) {
    // Tracks userId → orderId for users whose orders are resting in the book.
    // Cleared on CANCEL; retained on AMEND (same orderId, new price/qty).
    internal val activeOrders: MutableMap<String, String> = ConcurrentHashMap()

    val paused = AtomicBoolean(false)

    @Scheduled(fixedDelayString = "\${order-generator.interval-ms:500}")
    fun generateAndPublish() {
        if (!props.enabled || paused.get()) return

        val userId = props.users.random()
        val existingOrderId = activeOrders[userId]

        val event = if (existingOrderId == null) {
            place(userId)
        } else {
            if (Random.nextBoolean()) cancel(userId, existingOrderId) else amend(userId, existingOrderId)
        }

        publisher.publish(event)
    }

    private fun place(userId: String): OrderEventProto {
        val orderId = UUID.randomUUID().toString()
        activeOrders[userId] = orderId
        return OrderEventProto
            .newBuilder()
            .setAction(OrderActionProto.PLACE)
            .setOrderId(orderId)
            .setUserId(userId)
            .setSide(if (Random.nextBoolean()) OrderSideProto.BUY else OrderSideProto.SELL)
            .setPrice(randomInRange(props.minPrice, props.maxPrice).toPlainString())
            .setQuantity(randomInRange(props.minQuantity, props.maxQuantity).toPlainString())
            .setOrderType(if (Random.nextBoolean()) OrderTypeProto.MARKET else OrderTypeProto.LIMIT)
            .build()
    }

    private fun cancel(userId: String, orderId: String): OrderEventProto {
        activeOrders.remove(userId)
        return OrderEventProto
            .newBuilder()
            .setAction(OrderActionProto.CANCEL)
            .setOrderId(orderId)
            .setUserId(userId)
            .build()
    }

    private fun amend(userId: String, orderId: String): OrderEventProto =
        OrderEventProto
            .newBuilder()
            .setAction(OrderActionProto.AMEND)
            .setOrderId(orderId)
            .setUserId(userId)
            .setPrice(randomInRange(props.minPrice, props.maxPrice).toPlainString())
            .setQuantity(randomInRange(props.minQuantity, props.maxQuantity).toPlainString())
            .build()

    private fun randomInRange(min: BigDecimal, max: BigDecimal): BigDecimal =
        min
            .add(BigDecimal(Random.nextDouble()).multiply(max.subtract(min)))
            .setScale(2, RoundingMode.HALF_UP)
}
