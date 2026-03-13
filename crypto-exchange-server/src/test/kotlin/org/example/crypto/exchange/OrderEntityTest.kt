package org.example.crypto.exchange

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Pure unit tests for [OrderEntity] — no Spring context needed.
 */
class OrderEntityTest {
    // -------------------------------------------------------------------------
    // isFilled()
    // -------------------------------------------------------------------------

    @Nested
    inner class IsFilled {
        @Test
        fun `should return false when remaining quantity is positive`() {
            val order = order(quantity = BigDecimal("1.0"), remainingQuantity = BigDecimal("0.5"))
            assertThat(order.isFilled()).isFalse()
        }

        @Test
        fun `should return true when remaining quantity is zero`() {
            val order = order(quantity = BigDecimal("1.0"), remainingQuantity = BigDecimal.ZERO)
            assertThat(order.isFilled()).isTrue()
        }

        @Test
        fun `should return true when remaining quantity is negative`() {
            // Defensive: guard against over-fills that could theoretically occur
            val order = order(quantity = BigDecimal("1.0"), remainingQuantity = BigDecimal("-0.01"))
            assertThat(order.isFilled()).isTrue()
        }

        @Test
        fun `should return false for a freshly created order with non-zero quantity`() {
            val order = order(quantity = BigDecimal("5.0"))
            // Default remainingQuantity == quantity
            assertThat(order.isFilled()).isFalse()
        }

        @Test
        fun `should return true for a freshly created order with zero quantity`() {
            val order = order(quantity = BigDecimal.ZERO)
            assertThat(order.isFilled()).isTrue()
        }
    }

    // -------------------------------------------------------------------------
    // Default field values
    // -------------------------------------------------------------------------

    @Nested
    inner class DefaultFields {
        @Test
        fun `remainingQuantity defaults to quantity when not explicitly set`() {
            val qty = BigDecimal("3.75")
            val order = order(quantity = qty)
            assertThat(order.remainingQuantity).isEqualByComparingTo(qty)
        }

        @Test
        fun `id is non-blank by default`() {
            val order = order()
            assertThat(order.id).isNotBlank()
        }

        @Test
        fun `timestamp defaults to approximately now`() {
            val before = Instant.now()
            val order = order()
            val after = Instant.now()
            assertThat(order.timestamp).isBetween(before, after)
        }

        @Test
        fun `two orders created without explicit ids have different ids`() {
            val o1 = order()
            val o2 = order()
            assertThat(o1.id).isNotEqualTo(o2.id)
        }
    }

    // -------------------------------------------------------------------------
    // No-arg constructor (required by JPA)
    // -------------------------------------------------------------------------

    @Test
    fun `no-arg constructor produces an instance without throwing`() {
        val order = OrderEntity()
        assertThat(order).isNotNull()
        assertThat(order.id).isEmpty()
        assertThat(order.side).isEqualTo(OrderSide.BUY)
        assertThat(order.price).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(order.quantity).isEqualByComparingTo(BigDecimal.ZERO)
    }

    // -------------------------------------------------------------------------
    // Data class equality
    // -------------------------------------------------------------------------

    @Test
    fun `two orders with the same id are equal`() {
        val id = "fixed-id"
        val o1 = order(id = id, quantity = BigDecimal("1.0"))
        val o2 = order(id = id, quantity = BigDecimal("1.0"))
        assertThat(o1).isEqualTo(o2)
    }

    @Test
    fun `orders with different ids are not equal`() {
        val o1 = order(id = "id-A")
        val o2 = order(id = "id-B")
        assertThat(o1).isNotEqualTo(o2)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun order(
        id: String = "test-id",
        userId: String = "user-1",
        side: OrderSide = OrderSide.BUY,
        price: BigDecimal = BigDecimal("100"),
        quantity: BigDecimal = BigDecimal("1.0"),
        remainingQuantity: BigDecimal = quantity,
    ) = OrderEntity(
        id = id,
        userId = userId,
        side = side,
        price = price,
        quantity = quantity,
        remainingQuantity = remainingQuantity,
    )
}
