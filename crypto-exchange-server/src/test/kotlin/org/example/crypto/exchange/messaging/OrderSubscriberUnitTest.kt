package org.example.crypto.exchange.messaging

import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

/**
 * Pure unit tests for [OrderSubscriber] — verifies that Kafka proto events are
 * correctly translated into [OrderEntity] instances and forwarded to [Exchange].
 *
 * Full Kafka round-trip tests live in [OrderSubscriberTest] (Testcontainers).
 */
@ExtendWith(MockitoExtension::class)
class OrderSubscriberUnitTest {
    @Mock
    private lateinit var exchange: Exchange

    private lateinit var subscriber: OrderSubscriber

    @BeforeEach
    fun setUp() {
        subscriber = OrderSubscriber(exchange)
    }

    // -------------------------------------------------------------------------
    // BUY side
    // -------------------------------------------------------------------------

    @Test
    fun `should translate BUY proto event to BUY OrderEntity and forward to exchange`() {
        val event = orderEvent(userId = "user-1", side = OrderSideProto.BUY, price = "100.50", quantity = "2.5")

        subscriber.consume(event)

        val captor = ArgumentCaptor.forClass(OrderEntity::class.java)
        verify(exchange).placeOrder(captor.capture())

        with(captor.value) {
            assertThat(userId).isEqualTo("user-1")
            assertThat(side).isEqualTo(OrderSide.BUY)
            assertThat(price).isEqualByComparingTo(BigDecimal("100.50"))
            assertThat(quantity).isEqualByComparingTo(BigDecimal("2.5"))
        }
    }

    // -------------------------------------------------------------------------
    // SELL side
    // -------------------------------------------------------------------------

    @Test
    fun `should translate SELL proto event to SELL OrderEntity and forward to exchange`() {
        val event = orderEvent(userId = "user-2", side = OrderSideProto.SELL, price = "200.00", quantity = "1.0")

        subscriber.consume(event)

        val captor = ArgumentCaptor.forClass(OrderEntity::class.java)
        verify(exchange).placeOrder(captor.capture())

        with(captor.value) {
            assertThat(userId).isEqualTo("user-2")
            assertThat(side).isEqualTo(OrderSide.SELL)
            assertThat(price).isEqualByComparingTo(BigDecimal("200.00"))
            assertThat(quantity).isEqualByComparingTo(BigDecimal("1.0"))
        }
    }

    // -------------------------------------------------------------------------
    // Unknown side
    // -------------------------------------------------------------------------

    @Test
    fun `should throw IllegalArgumentException when proto side is UNRECOGNIZED`() {
        val event = orderEvent(side = OrderSideProto.UNRECOGNIZED)

        assertThrows<IllegalArgumentException> {
            subscriber.consume(event)
        }
    }

    // -------------------------------------------------------------------------
    // Field mapping — decimal precision
    // -------------------------------------------------------------------------

    @Test
    fun `should preserve price decimal precision from proto event`() {
        val event = orderEvent(price = "12345.6789", quantity = "0.00000001")

        subscriber.consume(event)

        val captor = ArgumentCaptor.forClass(OrderEntity::class.java)
        verify(exchange).placeOrder(captor.capture())

        assertThat(captor.value.price).isEqualByComparingTo("12345.6789")
        assertThat(captor.value.quantity).isEqualByComparingTo("0.00000001")
    }

    @Test
    fun `should assign a non-blank id to the constructed OrderEntity`() {
        subscriber.consume(orderEvent())

        val captor = ArgumentCaptor.forClass(OrderEntity::class.java)
        verify(exchange).placeOrder(captor.capture())

        assertThat(captor.value.id).isNotBlank()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun orderEvent(
        userId: String = "user",
        side: OrderSideProto = OrderSideProto.BUY,
        price: String = "100",
        quantity: String = "1",
    ): OrderEventProto =
        OrderEventProto
            .newBuilder()
            .setUserId(userId)
            .setSide(side)
            .setPrice(price)
            .setQuantity(quantity)
            .build()
}
