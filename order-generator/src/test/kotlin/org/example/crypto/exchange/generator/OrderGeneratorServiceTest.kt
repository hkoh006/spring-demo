package org.example.crypto.exchange.generator

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.messaging.proto.OrderActionProto
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.example.crypto.exchange.messaging.proto.OrderTypeProto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

/**
 * Pure unit tests for [OrderGeneratorService].
 *
 * Verifies that generated events respect configured bounds and that
 * the enabled flag is honoured — no Spring context, no Kafka.
 */
@ExtendWith(MockKExtension::class)
class OrderGeneratorServiceTest {
    @RelaxedMockK
    private lateinit var publisher: OrderPublisher

    private val defaultProps =
        OrderGeneratorProperties(
            enabled = true,
            intervalMs = 500,
            users = listOf("alice", "bob"),
            minPrice = BigDecimal("90.00"),
            maxPrice = BigDecimal("110.00"),
            minQuantity = BigDecimal("1.00"),
            maxQuantity = BigDecimal("10.00"),
            topic = "orders",
        )

    // Single-user props: ensures the second generateAndPublish() acts on the same user
    private val singleUserProps = defaultProps.copy(users = listOf("solo"))

    private lateinit var service: OrderGeneratorService

    @BeforeEach
    fun setUp() {
        service = OrderGeneratorService(publisher, defaultProps)
    }

    @Test
    fun `should not publish when enabled is false`() {
        val disabledService = OrderGeneratorService(publisher, defaultProps.copy(enabled = false))

        disabledService.generateAndPublish()

        verify(exactly = 0) { publisher.publish(any()) }
    }

    @Test
    fun `should publish exactly one event when enabled`() {
        service.generateAndPublish()

        verify(exactly = 1) { publisher.publish(any()) }
    }

    // -------------------------------------------------------------------------
    // PLACE — first call for a user
    // -------------------------------------------------------------------------

    @Nested
    inner class PlaceOrder {
        @RepeatedTest(20)
        fun `generated userId should be from the configured user pool`() {
            service.generateAndPublish()
            assertThat(capturePublishedEvent().userId).isIn("alice", "bob")
        }

        @RepeatedTest(20)
        fun `generated side should be BUY or SELL`() {
            service.generateAndPublish()
            assertThat(capturePublishedEvent().side).isIn(OrderSideProto.BUY, OrderSideProto.SELL)
        }

        @RepeatedTest(20)
        fun `generated price should be within configured bounds`() {
            service.generateAndPublish()
            val price = BigDecimal(capturePublishedEvent().price)
            assertThat(price)
                .isGreaterThanOrEqualTo(defaultProps.minPrice)
                .isLessThanOrEqualTo(defaultProps.maxPrice)
        }

        @RepeatedTest(20)
        fun `generated quantity should be within configured bounds`() {
            service.generateAndPublish()
            val quantity = BigDecimal(capturePublishedEvent().quantity)
            assertThat(quantity)
                .isGreaterThanOrEqualTo(defaultProps.minQuantity)
                .isLessThanOrEqualTo(defaultProps.maxQuantity)
        }

        @RepeatedTest(20)
        fun `generated order type should be MARKET or LIMIT`() {
            service.generateAndPublish()
            assertThat(capturePublishedEvent().orderType).isIn(OrderTypeProto.MARKET, OrderTypeProto.LIMIT)
        }

        @Test
        fun `PLACE event should carry a non-blank orderId`() {
            service.generateAndPublish()
            assertThat(capturePublishedEvent().orderId).isNotBlank()
        }

        @Test
        fun `PLACE event should have action PLACE`() {
            service.generateAndPublish()
            assertThat(capturePublishedEvent().action).isEqualTo(OrderActionProto.PLACE)
        }

        @Test
        fun `PLACE event should register the order in activeOrders`() {
            val svc = OrderGeneratorService(publisher, singleUserProps)
            svc.generateAndPublish()

            assertThat(svc.activeOrders).containsKey("solo")
            assertThat(svc.activeOrders["solo"]).isNotBlank()
        }
    }

    // -------------------------------------------------------------------------
    // CANCEL / AMEND — second call for same user
    // -------------------------------------------------------------------------

    @Nested
    inner class CancelOrAmend {
        private lateinit var svc: OrderGeneratorService

        @BeforeEach
        fun setUpSingleUser() {
            svc = OrderGeneratorService(publisher, singleUserProps)
            svc.generateAndPublish() // always PLACE for "solo"
        }

        @RepeatedTest(30)
        fun `second call for same user should emit CANCEL or AMEND`() {
            svc.generateAndPublish()
            assertThat(captureLastPublishedEvent().action).isIn(OrderActionProto.CANCEL, OrderActionProto.AMEND)
        }

        @RepeatedTest(30)
        fun `CANCEL or AMEND event should carry the same orderId as the original PLACE`() {
            val placeOrderId = capturePublishedEvent().orderId

            svc.generateAndPublish()

            assertThat(captureLastPublishedEvent().orderId).isEqualTo(placeOrderId)
        }

        @Test
        fun `CANCEL removes the user from activeOrders`() {
            // Force a CANCEL by injecting state and checking outcome over many tries
            // (Since CANCEL/AMEND is 50/50, run enough times that at least one CANCEL occurs)
            repeat(20) {
                if (svc.activeOrders.containsKey("solo")) svc.generateAndPublish()
            }
            // After enough iterations the user should eventually have been cancelled
            // (probability of never cancelling in 20 tries = 0.5^20 ≈ 0.0001%)
            assertThat(svc.activeOrders).doesNotContainKey("solo")
        }

        @RepeatedTest(20)
        fun `AMEND event should carry price within configured bounds`() {
            svc.generateAndPublish()
            val event = captureLastPublishedEvent()
            if (event.action == OrderActionProto.AMEND) {
                val price = BigDecimal(event.price)
                assertThat(price)
                    .isGreaterThanOrEqualTo(singleUserProps.minPrice)
                    .isLessThanOrEqualTo(singleUserProps.maxPrice)
            }
        }

        @RepeatedTest(20)
        fun `AMEND event should carry quantity within configured bounds`() {
            svc.generateAndPublish()
            val event = captureLastPublishedEvent()
            if (event.action == OrderActionProto.AMEND) {
                val quantity = BigDecimal(event.quantity)
                assertThat(quantity)
                    .isGreaterThanOrEqualTo(singleUserProps.minQuantity)
                    .isLessThanOrEqualTo(singleUserProps.maxQuantity)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun capturePublishedEvent(): OrderEventProto {
        val slot = slot<OrderEventProto>()
        verify { publisher.publish(capture(slot)) }
        return slot.captured
    }

    /** Captures the most recently published event (last call to publisher.publish). */
    private fun captureLastPublishedEvent(): OrderEventProto {
        val slots = mutableListOf<OrderEventProto>()
        verify(atLeast = 1) { publisher.publish(capture(slots)) }
        return slots.last()
    }
}
