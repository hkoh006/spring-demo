package org.example.crypto.exchange.controller

import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.api.OrderEndpointApi
import org.example.crypto.exchange.model.OrderDto
import org.example.crypto.exchange.model.OrderEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@Component
class OrderEndpoint(
    private val orderRepository: OrderRepository,
) : OrderEndpointApi {
    override fun getAllOrders(): ResponseEntity<List<OrderDto>> =
        ResponseEntity.ok(
            orderRepository.findAll().map {
                OrderDto(
                    id = it.id,
                    userId = it.userId,
                    side = OrderDto.Side.valueOf(it.side.name),
                    price = it.price,
                    quantity = it.quantity,
                    remainingQuantity = it.remainingQuantity,
                    timestamp = it.timestamp.atOffset(ZoneOffset.UTC),
                    isFilled = it.isFilled(),
                )
            },
        )

    override fun getOrderById(id: String): ResponseEntity<OrderDto> =
        orderRepository
            .findById(id)
            .getOrNull()
            ?.let {
                ResponseEntity.ok(
                    OrderDto(
                        id = it.id,
                        userId = it.userId,
                        side = OrderDto.Side.valueOf(it.side.name),
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
