package org.example.crypto.exchange.controller

import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.model.OrderDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

/**
 * Pure unit tests for [OrderEndpoint] — no Spring context loaded.
 *
 * HTTP wiring is tested at the integration level in [EndpointTest].
 */
@ExtendWith(MockitoExtension::class)
class OrderEndpointUnitTest {
    @Mock
    private lateinit var orderRepository: OrderRepository

    @InjectMocks
    private lateinit var endpoint: OrderEndpoint

    // -------------------------------------------------------------------------
    // getAllOrders
    // -------------------------------------------------------------------------

    @Test
    fun `getAllOrders should return 200 with empty list when no orders exist`() {
        `when`(orderRepository.findAll()).thenReturn(emptyList())

        val response = endpoint.getAllOrders()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `getAllOrders should map all order fields correctly`() {
        val entity =
            orderEntity(
                id = "order-1",
                userId = "user-A",
                side = OrderSide.BUY,
                price = BigDecimal("100.50"),
                quantity = BigDecimal("2.0"),
                remainingQuantity = BigDecimal("1.0"),
            )
        `when`(orderRepository.findAll()).thenReturn(listOf(entity))

        val body = endpoint.getAllOrders().body!!

        assertThat(body).hasSize(1)
        with(body[0]) {
            assertThat(id).isEqualTo("order-1")
            assertThat(userId).isEqualTo("user-A")
            assertThat(side).isEqualTo(OrderDto.Side.BUY)
            assertThat(price).isEqualByComparingTo("100.50")
            assertThat(quantity).isEqualByComparingTo("2.0")
            assertThat(remainingQuantity).isEqualByComparingTo("1.0")
            assertThat(isFilled).isFalse()
        }
    }

    @Test
    fun `getAllOrders should mark order as filled when remainingQuantity is zero`() {
        val entity = orderEntity(quantity = BigDecimal("1.0"), remainingQuantity = BigDecimal.ZERO)
        `when`(orderRepository.findAll()).thenReturn(listOf(entity))

        val body = endpoint.getAllOrders().body!!
        assertThat(body[0].isFilled).isTrue()
    }

    @Test
    fun `getAllOrders should map SELL side correctly`() {
        val entity = orderEntity(side = OrderSide.SELL)
        `when`(orderRepository.findAll()).thenReturn(listOf(entity))

        val body = endpoint.getAllOrders().body!!
        assertThat(body[0].side).isEqualTo(OrderDto.Side.SELL)
    }

    @Test
    fun `getAllOrders should return all orders when multiple exist`() {
        val orders =
            listOf(
                orderEntity(id = "o1"),
                orderEntity(id = "o2"),
                orderEntity(id = "o3"),
            )
        `when`(orderRepository.findAll()).thenReturn(orders)

        val body = endpoint.getAllOrders().body!!
        assertThat(body.map { it.id }).containsExactly("o1", "o2", "o3")
    }

    // -------------------------------------------------------------------------
    // getOrderById
    // -------------------------------------------------------------------------

    @Test
    fun `getOrderById should return 200 with order when found`() {
        val entity = orderEntity(id = "order-42", userId = "user-Z")
        `when`(orderRepository.findById("order-42")).thenReturn(Optional.of(entity))

        val response = endpoint.getOrderById("order-42")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo("order-42")
        assertThat(response.body!!.userId).isEqualTo("user-Z")
    }

    @Test
    fun `getOrderById should return 404 when order does not exist`() {
        `when`(orderRepository.findById("missing")).thenReturn(Optional.empty())

        val response = endpoint.getOrderById("missing")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).isNull()
    }

    @Test
    fun `getOrderById should map timestamp to UTC OffsetDateTime`() {
        val instant = Instant.parse("2024-06-15T12:00:00Z")
        val entity = orderEntity(id = "t-order", timestamp = instant)
        `when`(orderRepository.findById("t-order")).thenReturn(Optional.of(entity))

        val dto = endpoint.getOrderById("t-order").body!!
        assertThat(dto.timestamp.toInstant()).isEqualTo(instant)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun orderEntity(
        id: String = "test-id",
        userId: String = "user-1",
        side: OrderSide = OrderSide.BUY,
        price: BigDecimal = BigDecimal("100"),
        quantity: BigDecimal = BigDecimal("1.0"),
        remainingQuantity: BigDecimal = quantity,
        timestamp: Instant = Instant.now(),
    ) = OrderEntity(
        id = id,
        userId = userId,
        side = side,
        price = price,
        quantity = quantity,
        remainingQuantity = remainingQuantity,
        timestamp = timestamp,
    )
}
