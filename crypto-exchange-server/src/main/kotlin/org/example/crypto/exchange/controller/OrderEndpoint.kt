package org.example.crypto.exchange.controller

import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.api.OrderEndpointApi
import org.example.crypto.exchange.model.OrderEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api/orders")
class OrderEndpoint(
    private val orderRepository: OrderRepository,
) : OrderEndpointApi {
    @GetMapping
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

    @GetMapping("/{id}")
    override fun getOrderById(
        @PathVariable id: String,
    ): ResponseEntity<OrderEntity> =
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
