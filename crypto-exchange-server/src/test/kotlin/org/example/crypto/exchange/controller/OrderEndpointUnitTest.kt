package org.example.crypto.exchange.controller

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.OrderStatus
import org.example.crypto.exchange.model.AmendOrderRequest
import org.example.crypto.exchange.model.OrderDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

/**
 * Pure unit tests for [OrderEndpoint] — no Spring context loaded.
 *
 * HTTP wiring is tested at the integration level in [EndpointTest].
 */
@ExtendWith(MockKExtension::class)
class OrderEndpointUnitTest {
    @MockK
    private lateinit var orderRepository: OrderRepository

    @MockK
    private lateinit var exchange: Exchange

    private lateinit var endpoint: OrderEndpoint

    @BeforeEach
    fun setUp() {
        endpoint = OrderEndpoint(orderRepository, exchange)
    }

    // -------------------------------------------------------------------------
    // getAllOrders
    // -------------------------------------------------------------------------

    @Test
    fun `getAllOrders should return 200 with empty list when no orders exist`() {
        every { orderRepository.findAll() } returns emptyList()

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
        every { orderRepository.findAll() } returns listOf(entity)

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
        every { orderRepository.findAll() } returns listOf(entity)

        val body = endpoint.getAllOrders().body!!
        assertThat(body[0].isFilled).isTrue()
    }

    @Test
    fun `getAllOrders should map SELL side correctly`() {
        val entity = orderEntity(side = OrderSide.SELL)
        every { orderRepository.findAll() } returns listOf(entity)

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
        every { orderRepository.findAll() } returns orders

        val body = endpoint.getAllOrders().body!!
        assertThat(body.map { it.id }).containsExactly("o1", "o2", "o3")
    }

    // -------------------------------------------------------------------------
    // getOrderById
    // -------------------------------------------------------------------------

    @Test
    fun `getOrderById should return 200 with order when found`() {
        val entity = orderEntity(id = "order-42", userId = "user-Z")
        every { orderRepository.findById("order-42") } returns Optional.of(entity)

        val response = endpoint.getOrderById("order-42")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.id).isEqualTo("order-42")
        assertThat(response.body!!.userId).isEqualTo("user-Z")
    }

    @Test
    fun `getOrderById should return 404 when order does not exist`() {
        every { orderRepository.findById("missing") } returns Optional.empty()

        val response = endpoint.getOrderById("missing")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).isNull()
    }

    @Test
    fun `getOrderById should map timestamp to UTC OffsetDateTime`() {
        val instant = Instant.parse("2024-06-15T12:00:00Z")
        val entity = orderEntity(id = "t-order", timestamp = instant)
        every { orderRepository.findById("t-order") } returns Optional.of(entity)

        val dto = endpoint.getOrderById("t-order").body!!
        assertThat(dto.timestamp.toInstant()).isEqualTo(instant)
    }

    // -------------------------------------------------------------------------
    // cancelOrder
    // -------------------------------------------------------------------------

    @Test
    fun `cancelOrder should return 200 with cancelled order when order is in the book`() {
        val entity = orderEntity(id = "cancel-me", status = OrderStatus.CANCELLED)
        every { exchange.cancelOrder("cancel-me") } returns entity

        val response = endpoint.cancelOrder("cancel-me")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo(OrderDto.Status.CANCELLED)
    }

    @Test
    fun `cancelOrder should return 404 when order is not in the book`() {
        every { exchange.cancelOrder("missing") } returns null

        val response = endpoint.cancelOrder("missing")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // -------------------------------------------------------------------------
    // amendOrder
    // -------------------------------------------------------------------------

    @Test
    fun `amendOrder should return 200 with updated order when order is in the book`() {
        val amended = orderEntity(id = "amend-me", price = BigDecimal("110"), quantity = BigDecimal("2.0"))
        every { exchange.amendOrder("amend-me", BigDecimal("110"), BigDecimal("2.0")) } returns amended

        val response = endpoint.amendOrder("amend-me", AmendOrderRequest(BigDecimal("110"), BigDecimal("2.0")))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.price).isEqualByComparingTo("110")
        assertThat(response.body!!.quantity).isEqualByComparingTo("2.0")
    }

    @Test
    fun `amendOrder should return 404 when order is not in the book`() {
        every { exchange.amendOrder("missing", any(), any()) } returns null

        val response = endpoint.amendOrder("missing", AmendOrderRequest(BigDecimal("100"), BigDecimal("1.0")))

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
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
        status: OrderStatus = OrderStatus.OPEN,
    ) = OrderEntity(
        id = id,
        userId = userId,
        side = side,
        price = price,
        quantity = quantity,
        remainingQuantity = remainingQuantity,
        timestamp = timestamp,
        status = status,
    )
}
