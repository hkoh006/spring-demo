package org.example.crypto.exchange.generator

import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.example.crypto.exchange.messaging.proto.OrderTypeProto
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

@Component
class OrderGeneratorService(
    private val publisher: OrderPublisher,
    private val props: OrderGeneratorProperties,
) {
    @Scheduled(fixedDelayString = "\${order-generator.interval-ms:500}")
    fun generateAndPublish() {
        if (!props.enabled) return

        val orderType = if (Random.nextBoolean()) OrderTypeProto.MARKET else OrderTypeProto.LIMIT
        val event =
            OrderEventProto
                .newBuilder()
                .setUserId(props.users.random())
                .setSide(if (Random.nextBoolean()) OrderSideProto.BUY else OrderSideProto.SELL)
                .setPrice(randomInRange(props.minPrice, props.maxPrice).toPlainString())
                .setQuantity(randomInRange(props.minQuantity, props.maxQuantity).toPlainString())
                .setOrderType(orderType)
                .build()

        publisher.publish(event)
    }

    private fun randomInRange(
        min: BigDecimal,
        max: BigDecimal,
    ): BigDecimal =
        min
            .add(BigDecimal(Random.nextDouble()).multiply(max.subtract(min)))
            .setScale(2, RoundingMode.HALF_UP)
}
