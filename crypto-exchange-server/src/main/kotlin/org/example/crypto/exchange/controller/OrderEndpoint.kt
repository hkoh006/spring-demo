package org.example.crypto.exchange.controller

import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderEndpoint(
    private val orderRepository: OrderRepository,
) {
    @GetMapping
    fun getAllOrders(): List<OrderEntity> = orderRepository.findAll()

    @GetMapping("/{id}")
    fun getOrderById(
        @PathVariable id: String,
    ): ResponseEntity<OrderEntity> =
        orderRepository
            .findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
}
