package org.example.demowithnativeimage

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    private val orderRepository: OrderRepository
) {

    @GetMapping("/api/v1/ping")
    fun ping() = "pong"

    @GetMapping("/api/v1/orders-blaze")
    fun ordersViaBlaze(): List<OrderEntity> = orderRepository.findAllWithBlaze()
}
