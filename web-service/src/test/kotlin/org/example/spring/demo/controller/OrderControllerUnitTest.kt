package org.example.spring.demo.controller

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.example.spring.demo.dao.OrderJpaRepository
import org.example.spring.demo.dao.model.Allocation
import org.example.spring.demo.dao.model.Market
import org.example.spring.demo.dao.model.OrderDetails
import org.example.spring.demo.dao.model.OrderEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.util.Optional

/**
 * Pure unit tests for [OrderController] using MockK — no Spring context, no database.
 *
 * Full HTTP integration tests (status codes, serialization, Location headers)
 * live in [OrderControllerTest].
 */
@ExtendWith(MockKExtension::class)
class OrderControllerUnitTest {
    @MockK
    private lateinit var repository: OrderJpaRepository

    private lateinit var controller: OrderController
    private lateinit var uriBuilder: UriComponentsBuilder

    @BeforeEach
    fun setUp() {
        controller = OrderController(repository)
        // Provide a realistic UriComponentsBuilder so create() can build Location URIs
        val request = MockHttpServletRequest()
        request.serverName = "localhost"
        request.serverPort = 8080
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        uriBuilder =
            UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host("localhost")
                .port(8080)
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Nested
    inner class GetById {
        @Test
        fun `should return order when it exists`() {
            val entity = orderEntity(id = 1L)
            every { repository.findById(1L) } returns Optional.of(entity)

            val result = controller.getById(1L)

            assertThat(result.id).isEqualTo(1L)
        }

        @Test
        fun `should throw 404 when order does not exist`() {
            every { repository.findById(any()) } returns Optional.empty()

            val ex = assertThrows<ResponseStatusException> { controller.getById(999L) }
            assertThat(ex.statusCode.value()).isEqualTo(404)
        }

        @Test
        fun `should include order id in the 404 message`() {
            every { repository.findById(42L) } returns Optional.empty()

            val ex = assertThrows<ResponseStatusException> { controller.getById(42L) }
            assertThat(ex.reason).contains("42")
        }
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Nested
    inner class Create {
        @Test
        fun `should save and return 201 when order with valid id is provided`() {
            val entity = orderEntity(id = 10L)
            every { repository.existsById(10L) } returns false
            every { repository.save(entity) } returns entity

            val response = controller.create(entity, uriBuilder)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.id).isEqualTo(10L)
        }

        @Test
        fun `should set Location header pointing to the new order`() {
            val entity = orderEntity(id = 10L)
            every { repository.existsById(10L) } returns false
            every { repository.save(entity) } returns entity

            val response = controller.create(entity, uriBuilder)

            assertThat(response.headers.location.toString()).endsWith("/api/v1/orders/10")
        }

        @Test
        fun `should throw 400 when order id is null`() {
            val entity = OrderEntity(id = null, orderDetails = OrderDetails(emptyList(), Market()))

            val ex = assertThrows<ResponseStatusException> { controller.create(entity, uriBuilder) }
            assertThat(ex.statusCode.value()).isEqualTo(400)
        }

        @Test
        fun `should throw 409 when order with same id already exists`() {
            val entity = orderEntity(id = 5L)
            every { repository.existsById(5L) } returns true

            val ex = assertThrows<ResponseStatusException> { controller.create(entity, uriBuilder) }
            assertThat(ex.statusCode.value()).isEqualTo(409)
        }

        @Test
        fun `should not call save when order id is null`() {
            val entity = OrderEntity(id = null, orderDetails = OrderDetails(emptyList(), Market()))

            runCatching { controller.create(entity, uriBuilder) }

            verify(exactly = 0) { repository.save(entity) }
        }

        @Test
        fun `should not call save when order already exists`() {
            val entity = orderEntity(id = 5L)
            every { repository.existsById(5L) } returns true

            runCatching { controller.create(entity, uriBuilder) }

            verify(exactly = 0) { repository.save(entity) }
        }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Nested
    inner class Update {
        @Test
        fun `should save and return updated order`() {
            val incoming =
                orderEntity(
                    id = 1L,
                    allocations = listOf(Allocation("b1", 50)),
                )
            every { repository.existsById(1L) } returns true
            every { repository.save(any()) } returns incoming

            val result = controller.update(1L, incoming)

            assertThat(result.id).isEqualTo(1L)
        }

        @Test
        fun `should enforce path id over body id`() {
            // Body carries id = 999 but path is 1 — the saved entity must use id = 1
            val incoming = orderEntity(id = 999L)
            every { repository.existsById(1L) } returns true

            val slot = slot<OrderEntity>()
            every { repository.save(capture(slot)) } answers { slot.captured }

            controller.update(1L, incoming)

            assertThat(slot.captured.id).isEqualTo(1L)
        }

        @Test
        fun `should throw 404 when order to update does not exist`() {
            every { repository.existsById(any()) } returns false

            val ex =
                assertThrows<ResponseStatusException> {
                    controller.update(1L, orderEntity(id = 1L))
                }
            assertThat(ex.statusCode.value()).isEqualTo(404)
        }
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Nested
    inner class Delete {
        @Test
        fun `should return 204 and delete order when it exists`() {
            every { repository.existsById(1L) } returns true
            every { repository.deleteById(1L) } just Runs

            val response = controller.delete(1L)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            verify { repository.deleteById(1L) }
        }

        @Test
        fun `should throw 404 when order to delete does not exist`() {
            every { repository.existsById(any()) } returns false

            val ex = assertThrows<ResponseStatusException> { controller.delete(1L) }
            assertThat(ex.statusCode.value()).isEqualTo(404)
        }

        @Test
        fun `should not call deleteById when order does not exist`() {
            every { repository.existsById(any()) } returns false

            runCatching { controller.delete(1L) }

            verify(exactly = 0) { repository.deleteById(any()) }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun orderEntity(
        id: Long? = 1L,
        allocations: List<Allocation> = listOf(Allocation("a1", 10)),
        orderType: org.example.spring.demo.dao.model.OrderType = Market(),
    ) = OrderEntity(
        id = id,
        orderDetails = OrderDetails(allocations = allocations, orderType = orderType),
    )
}
