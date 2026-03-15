package org.example.crypto.exchange.messaging

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.messaging.proto.OrderActionProto
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

/**
 * Pure unit tests for [OrderSubscriber] — verifies that Kafka proto events are
 * correctly dispatched to [Exchange] based on the action field.
 *
 * Full Kafka round-trip tests live in [OrderSubscriberTest] (Testcontainers).
 */
@ExtendWith(MockKExtension::class)
class OrderSubscriberUnitTest {
    @RelaxedMockK
    private lateinit var exchange: Exchange

    private lateinit var subscriber: OrderSubscriber

    @BeforeEach
    fun setUp() {
        subscriber = OrderSubscriber(exchange)
    }

    // -------------------------------------------------------------------------
    // PLACE — action = PLACE (or default 0)
    // -------------------------------------------------------------------------

    @Nested
    inner class PlaceAction {
        @Test
        fun `should translate BUY proto event to BUY OrderEntity and forward to exchange`() {
            val event = placeEvent(userId = "user-1", side = OrderSideProto.BUY, price = "100.50", quantity = "2.5")

            subscriber.consume(event)

            val slot = slot<OrderEntity>()
            verify { exchange.placeOrder(capture(slot)) }
            with(slot.captured) {
                assertThat(userId).isEqualTo("user-1")
                assertThat(side).isEqualTo(OrderSide.BUY)
                assertThat(price).isEqualByComparingTo(BigDecimal("100.50"))
                assertThat(quantity).isEqualByComparingTo(BigDecimal("2.5"))
            }
        }

        @Test
        fun `should translate SELL proto event to SELL OrderEntity and forward to exchange`() {
            val event = placeEvent(userId = "user-2", side = OrderSideProto.SELL, price = "200.00", quantity = "1.0")

            subscriber.consume(event)

            val slot = slot<OrderEntity>()
            verify { exchange.placeOrder(capture(slot)) }
            with(slot.captured) {
                assertThat(userId).isEqualTo("user-2")
                assertThat(side).isEqualTo(OrderSide.SELL)
            }
        }

        @Test
        fun `PLACE event with orderId should use that id for the OrderEntity`() {
            val event = placeEvent(orderId = "fixed-uuid-123")

            subscriber.consume(event)

            val slot = slot<OrderEntity>()
            verify { exchange.placeOrder(capture(slot)) }
            assertThat(slot.captured.id).isEqualTo("fixed-uuid-123")
        }

        @Test
        fun `PLACE event without orderId should assign a generated non-blank id`() {
            val event = placeEvent(orderId = "") // no orderId set

            subscriber.consume(event)

            val slot = slot<OrderEntity>()
            verify { exchange.placeOrder(capture(slot)) }
            assertThat(slot.captured.id).isNotBlank()
        }

        @Test
        fun `should preserve price decimal precision from proto event`() {
            val event = placeEvent(price = "12345.6789", quantity = "0.00000001")

            subscriber.consume(event)

            val slot = slot<OrderEntity>()
            verify { exchange.placeOrder(capture(slot)) }
            assertThat(slot.captured.price).isEqualByComparingTo("12345.6789")
            assertThat(slot.captured.quantity).isEqualByComparingTo("0.00000001")
        }
    }

    // -------------------------------------------------------------------------
    // CANCEL action
    // -------------------------------------------------------------------------

    @Nested
    inner class CancelAction {
        @Test
        fun `should call cancelOrder with the orderId from the event`() {
            val event = cancelEvent(orderId = "order-to-cancel")

            subscriber.consume(event)

            verify { exchange.cancelOrder("order-to-cancel") }
        }

        @Test
        fun `should not call placeOrder for a CANCEL event`() {
            subscriber.consume(cancelEvent(orderId = "some-order"))

            verify(exactly = 0) { exchange.placeOrder(any()) }
        }
    }

    // -------------------------------------------------------------------------
    // AMEND action
    // -------------------------------------------------------------------------

    @Nested
    inner class AmendAction {
        @Test
        fun `should call amendOrder with orderId, new price, and new quantity`() {
            val event = amendEvent(orderId = "order-to-amend", price = "105.00", quantity = "3.0")

            subscriber.consume(event)

            verify { exchange.amendOrder("order-to-amend", BigDecimal("105.00"), BigDecimal("3.0")) }
        }

        @Test
        fun `should not call placeOrder for an AMEND event`() {
            subscriber.consume(amendEvent(orderId = "some-order", price = "100", quantity = "1"))

            verify(exactly = 0) { exchange.placeOrder(any()) }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun placeEvent(
        userId: String = "user",
        side: OrderSideProto = OrderSideProto.BUY,
        price: String = "100",
        quantity: String = "1",
        orderId: String = "test-order-id",
    ): OrderEventProto =
        OrderEventProto
            .newBuilder()
            .setAction(OrderActionProto.PLACE)
            .setOrderId(orderId)
            .setUserId(userId)
            .setSide(side)
            .setPrice(price)
            .setQuantity(quantity)
            .build()

    private fun cancelEvent(orderId: String): OrderEventProto =
        OrderEventProto
            .newBuilder()
            .setAction(OrderActionProto.CANCEL)
            .setOrderId(orderId)
            .setUserId("user")
            .build()

    private fun amendEvent(orderId: String, price: String, quantity: String): OrderEventProto =
        OrderEventProto
            .newBuilder()
            .setAction(OrderActionProto.AMEND)
            .setOrderId(orderId)
            .setUserId("user")
            .setPrice(price)
            .setQuantity(quantity)
            .build()
}
