package org.example.spring.demo.controller

import org.example.spring.demo.dao.OrderJpaRepository
import org.example.spring.demo.dao.model.OrderEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val repository: OrderJpaRepository,
) {
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
    ): OrderEntity = repository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Order $id not found") }

    @PostMapping
    fun create(
        @RequestBody order: OrderEntity,
        uriBuilder: UriComponentsBuilder,
    ): ResponseEntity<OrderEntity> {
        val id = order.id ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be provided for new Order")
        if (repository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Order $id already exists")
        }
        val saved = repository.save(order)
        val location = uriBuilder.path("/api/v1/orders/{id}").buildAndExpand(saved.id).toUri()
        return ResponseEntity.created(location).body(saved)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody order: OrderEntity,
    ): OrderEntity {
        if (!repository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order $id not found")
        }
        // Enforce path ID
        val toSave =
            OrderEntity(
                id = id,
                orderDetails = order.orderDetails,
            )
        return repository.save(toSave)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        if (!repository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order $id not found")
        }
        repository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
