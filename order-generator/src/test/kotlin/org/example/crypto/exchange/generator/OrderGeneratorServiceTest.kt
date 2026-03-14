package org.example.crypto.exchange.generator

import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.example.crypto.exchange.messaging.proto.OrderSideProto
import org.example.crypto.exchange.messaging.proto.OrderTypeProto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

/**
 * Pure unit tests for [OrderGeneratorService].
 *
 * Verifies that generated events respect configured bounds and that
 * the enabled flag is honoured — no Spring context, no Kafka.
 */
@ExtendWith(MockitoExtension::class)
class OrderGeneratorServiceTest {
    @Mock
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

    private lateinit var service: OrderGeneratorService

    @BeforeEach
    fun setUp() {
        service = OrderGeneratorService(publisher, defaultProps)
    }

    @Test
    fun `should not publish when enabled is false`() {
        val disabledService = OrderGeneratorService(publisher, defaultProps.copy(enabled = false))

        disabledService.generateAndPublish()

        verify(publisher, never()).publish(any())
    }

    @Test
    fun `should publish exactly one event when enabled`() {
        service.generateAndPublish()

        verify(publisher).publish(any())
    }

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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun capturePublishedEvent(): OrderEventProto {
        val captor = ArgumentCaptor.forClass(OrderEventProto::class.java)
        // capture() returns null (Mockito side-effect mechanism); the ?: fallback satisfies
        // Kotlin's non-null check at the call site without affecting what Mockito captures.
        verify(publisher).publish(captor.capture() ?: OrderEventProto.getDefaultInstance())
        return captor.value
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = org.mockito.Mockito.any<T>() as T
}
