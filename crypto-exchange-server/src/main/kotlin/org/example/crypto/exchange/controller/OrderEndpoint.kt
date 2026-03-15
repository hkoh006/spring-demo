package org.example.crypto.exchange.controller

import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.api.OrderEndpointApi
import org.example.crypto.exchange.model.AmendOrderRequest
import org.example.crypto.exchange.model.OrderDto
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@Component
class OrderEndpoint(
    private val orderRepository: OrderRepository,
    private val exchange: Exchange,
) : OrderEndpointApi {
    override fun getAllOrders(): ResponseEntity<List<OrderDto>> =
        ResponseEntity.ok(orderRepository.findAll().map { it.toDto() })

    override fun getOrderById(id: String): ResponseEntity<OrderDto> =
        orderRepository.findById(id).getOrNull()?.let { ResponseEntity.ok(it.toDto()) }
            ?: ResponseEntity.notFound().build()

    override fun cancelOrder(id: String): ResponseEntity<OrderDto> =
        exchange.cancelOrder(id)?.let { ResponseEntity.ok(it.toDto()) }
            ?: ResponseEntity.notFound().build()

    override fun amendOrder(id: String, amendOrderRequest: AmendOrderRequest): ResponseEntity<OrderDto> =
        exchange.amendOrder(id, amendOrderRequest.price, amendOrderRequest.quantity)?.let { ResponseEntity.ok(it.toDto()) }
            ?: ResponseEntity.notFound().build()

    private fun OrderEntity.toDto() = OrderDto(
        id = id,
        userId = userId,
        side = OrderDto.Side.valueOf(side.name),
        price = price,
        quantity = quantity,
        remainingQuantity = remainingQuantity,
        timestamp = timestamp.atOffset(ZoneOffset.UTC),
        isFilled = isFilled(),
        status = OrderDto.Status.valueOf(status.name),
    )
}
