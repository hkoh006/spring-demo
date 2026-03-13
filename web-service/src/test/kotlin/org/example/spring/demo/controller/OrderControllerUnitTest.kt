package org.example.spring.demo.controller

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
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.util.Optional

/**
 * Pure unit tests for [OrderController] using Mockito — no Spring context, no database.
 *
 * Full HTTP integration tests (status codes, serialization, Location headers)
 * live in [OrderControllerTest].
 */
@ExtendWith(MockitoExtension::class)
class OrderControllerUnitTest {
    @Mock
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
            `when`(repository.findById(1L)).thenReturn(Optional.of(entity))

            val result = controller.getById(1L)

            assertThat(result.id).isEqualTo(1L)
        }

        @Test
        fun `should throw 404 when order does not exist`() {
            `when`(repository.findById(anyLong())).thenReturn(Optional.empty())

            val ex = assertThrows<ResponseStatusException> { controller.getById(999L) }
            assertThat(ex.statusCode.value()).isEqualTo(404)
        }

        @Test
        fun `should include order id in the 404 message`() {
            `when`(repository.findById(42L)).thenReturn(Optional.empty())

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
            `when`(repository.existsById(10L)).thenReturn(false)
            `when`(repository.save(entity)).thenReturn(entity)

            val response = controller.create(entity, uriBuilder)

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.id).isEqualTo(10L)
        }

        @Test
        fun `should set Location header pointing to the new order`() {
            val entity = orderEntity(id = 10L)
            `when`(repository.existsById(10L)).thenReturn(false)
            `when`(repository.save(entity)).thenReturn(entity)

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
            `when`(repository.existsById(5L)).thenReturn(true)

            val ex = assertThrows<ResponseStatusException> { controller.create(entity, uriBuilder) }
            assertThat(ex.statusCode.value()).isEqualTo(409)
        }

        @Test
        fun `should not call save when order id is null`() {
            val entity = OrderEntity(id = null, orderDetails = OrderDetails(emptyList(), Market()))

            runCatching { controller.create(entity, uriBuilder) }

            verify(repository, never()).save(entity)
        }

        @Test
        fun `should not call save when order already exists`() {
            val entity = orderEntity(id = 5L)
            `when`(repository.existsById(5L)).thenReturn(true)

            runCatching { controller.create(entity, uriBuilder) }

            verify(repository, never()).save(entity)
        }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Nested
    inner class Update {
        @Test
        fun `should save and return updated order`() {
            val existing = orderEntity(id = 1L)
            val incoming =
                orderEntity(
                    id = 1L,
                    allocations = listOf(Allocation("b1", 50)),
                )
            `when`(repository.existsById(1L)).thenReturn(true)
            `when`(repository.save(org.mockito.ArgumentMatchers.any())).thenReturn(incoming)

            val result = controller.update(1L, incoming)

            assertThat(result.id).isEqualTo(1L)
        }

        @Test
        fun `should enforce path id over body id`() {
            // Body carries id = 999 but path is 1 — the saved entity must use id = 1
            val incoming = orderEntity(id = 999L)
            `when`(repository.existsById(1L)).thenReturn(true)

            val savedCaptor = org.mockito.ArgumentCaptor.forClass(OrderEntity::class.java)
            `when`(repository.save(savedCaptor.capture())).thenAnswer { savedCaptor.value }

            controller.update(1L, incoming)

            assertThat(savedCaptor.value.id).isEqualTo(1L)
        }

        @Test
        fun `should throw 404 when order to update does not exist`() {
            `when`(repository.existsById(anyLong())).thenReturn(false)

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
            `when`(repository.existsById(1L)).thenReturn(true)

            val response = controller.delete(1L)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
            verify(repository).deleteById(1L)
        }

        @Test
        fun `should throw 404 when order to delete does not exist`() {
            `when`(repository.existsById(anyLong())).thenReturn(false)

            val ex = assertThrows<ResponseStatusException> { controller.delete(1L) }
            assertThat(ex.statusCode.value()).isEqualTo(404)
        }

        @Test
        fun `should not call deleteById when order does not exist`() {
            `when`(repository.existsById(anyLong())).thenReturn(false)

            runCatching { controller.delete(1L) }

            verify(repository, never()).deleteById(anyLong())
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
