package org.example.spring.demo.controller

import org.example.spring.demo.config.TestcontainersDatabaseConfig
import org.example.spring.demo.dao.OrderJpaRepository
import org.example.spring.demo.dao.model.Allocation
import org.example.spring.demo.dao.model.Market
import org.example.spring.demo.dao.model.OrderDetails
import org.example.spring.demo.dao.model.OrderEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class OrderControllerTest {

    @Autowired
    lateinit var client: RestTestClient

    @Autowired
    lateinit var orderJpaRepository: OrderJpaRepository

    @BeforeEach
    fun setup() {
        orderJpaRepository.deleteAll()
    }

    private fun createOrderEntity(id: Long) = OrderEntity(
        id = id,
        orderDetails = OrderDetails(
            allocations = listOf(Allocation("a1", 10)),
            orderType = Market()
        )
    )

    @Test
    fun `getById should return order when exists`() {
        val order = createOrderEntity(1L)
        orderJpaRepository.save(order)

        client.get()
            .uri("/api/v1/orders/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.orderDetails.allocations[0].id").isEqualTo("a1")
    }

    @Test
    fun `getById should return 404 when not exists`() {
        client.get()
            .uri("/api/v1/orders/999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `create should save and return 201 when order is valid`() {
        val order = createOrderEntity(1L)

        client.post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(order)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectHeader().valueMatches("Location", ".*/api/v1/orders/1$")
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)

        assert(orderJpaRepository.existsById(1L))
    }

    @Test
    fun `create should return 400 when id is missing`() {
        val order = OrderEntity(
            id = null,
            orderDetails = OrderDetails(listOf(), Market())
        )

        client.post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(order)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `create should return 409 when order already exists`() {
        val order = createOrderEntity(1L)
        orderJpaRepository.save(order)

        client.post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(order)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    fun `update should return 200 and updated order`() {
        val order = createOrderEntity(1L)
        orderJpaRepository.save(order)

        val updatedOrder = OrderEntity(
            id = 1L,
            orderDetails = OrderDetails(
                allocations = listOf(Allocation("a2", 20)),
                orderType = Market()
            )
        )

        client.put()
            .uri("/api/v1/orders/1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(updatedOrder)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.orderDetails.allocations[0].id").isEqualTo("a2")
            .jsonPath("$.orderDetails.allocations[0].quantity").isEqualTo(20)
    }

    @Test
    fun `update should return 404 when order does not exist`() {
        val order = createOrderEntity(1L)

        client.put()
            .uri("/api/v1/orders/1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(order)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `delete should return 204 and remove order`() {
        val order = createOrderEntity(1L)
        orderJpaRepository.save(order)

        client.delete()
            .uri("/api/v1/orders/1")
            .exchange()
            .expectStatus().isNoContent

        assert(!orderJpaRepository.existsById(1L))
    }

    @Test
    fun `delete should return 404 when order does not exist`() {
        client.delete()
            .uri("/api/v1/orders/1")
            .exchange()
            .expectStatus().isNotFound
    }
}
