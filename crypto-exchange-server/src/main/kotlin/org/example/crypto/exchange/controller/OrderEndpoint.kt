package org.example.crypto.exchange.controller

import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.api.OrderEndpointApi
import org.example.crypto.exchange.model.OrderEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@Component
class OrderEndpoint(
    private val orderRepository: OrderRepository,
) : OrderEndpointApi {
    override fun getAllOrders(): ResponseEntity<List<OrderEntity>> =
        ResponseEntity.ok(
            orderRepository.findAll().map {
                OrderEntity(
                    id = it.id,
                    userId = it.userId,
                    side = OrderEntity.Side.valueOf(it.side.name),
                    price = it.price,
                    quantity = it.quantity,
                    remainingQuantity = it.remainingQuantity,
                    timestamp = it.timestamp.atOffset(ZoneOffset.UTC),
                    isFilled = it.isFilled(),
                )
            },
        )

    override fun getOrderById(id: String): ResponseEntity<OrderEntity> =
        orderRepository
            .findById(id)
            .getOrNull()
            ?.let {
                ResponseEntity.ok(
                    OrderEntity(
                        id = it.id,
                        userId = it.userId,
                        side = OrderEntity.Side.valueOf(it.side.name),
                        price = it.price,
                        quantity = it.quantity,
                        remainingQuantity = it.remainingQuantity,
                        timestamp = it.timestamp.atOffset(ZoneOffset.UTC),
                        isFilled = it.isFilled(),
                    ),
                )
            }
            ?: ResponseEntity.notFound().build()
}
