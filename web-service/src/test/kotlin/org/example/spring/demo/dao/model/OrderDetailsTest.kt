package org.example.spring.demo.dao.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Pure unit tests for [OrderDetails], [Allocation], and the [OrderType] hierarchy.
 *
 * JSON serialisation round-trips are covered in [org.example.spring.demo.dao.OrderTypeSerializationTest].
 * These tests focus on domain object construction and [isFilled] / value semantics.
 */
class OrderDetailsTest {
    // -------------------------------------------------------------------------
    // OrderDetails construction
    // -------------------------------------------------------------------------

    @Nested
    inner class OrderDetailsConstruction {
        @Test
        fun `should hold allocations list and order type`() {
            val allocs = listOf(Allocation("a1", 5), Allocation("a2", 10))
            val details = OrderDetails(allocations = allocs, orderType = Market())

            assertThat(details.allocations).hasSize(2)
            assertThat(details.orderType).isInstanceOf(Market::class.java)
        }

        @Test
        fun `should allow empty allocations list`() {
            val details = OrderDetails(allocations = emptyList(), orderType = Market())
            assertThat(details.allocations).isEmpty()
        }

        @Test
        fun `two OrderDetails with identical content are equal`() {
            val d1 =
                OrderDetails(
                    allocations = listOf(Allocation("x", 1)),
                    orderType = Limit(BigDecimal("99")),
                )
            val d2 =
                OrderDetails(
                    allocations = listOf(Allocation("x", 1)),
                    orderType = Limit(BigDecimal("99")),
                )
            assertThat(d1).isEqualTo(d2)
        }
    }

    // -------------------------------------------------------------------------
    // Allocation
    // -------------------------------------------------------------------------

    @Nested
    inner class AllocationTests {
        @Test
        fun `allocation stores id and quantity`() {
            val alloc = Allocation(id = "alloc-1", quantity = 42)
            assertThat(alloc.id).isEqualTo("alloc-1")
            assertThat(alloc.quantity).isEqualTo(42)
        }

        @Test
        fun `allocations with same id and quantity are equal`() {
            assertThat(Allocation("x", 1)).isEqualTo(Allocation("x", 1))
        }

        @Test
        fun `allocations with different ids are not equal`() {
            assertThat(Allocation("x", 1)).isNotEqualTo(Allocation("y", 1))
        }

        @Test
        fun `allocations with different quantities are not equal`() {
            assertThat(Allocation("x", 1)).isNotEqualTo(Allocation("x", 2))
        }
    }

    // -------------------------------------------------------------------------
    // OrderType subtypes
    // -------------------------------------------------------------------------

    @Nested
    inner class OrderTypeSubtypes {
        @Test
        fun `Market is an instance of OrderType`() {
            assertThat(Market()).isInstanceOf(OrderType::class.java)
        }

        @Test
        fun `Limit stores price with default zero`() {
            assertThat(Limit().price).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        fun `Limit stores explicit price`() {
            assertThat(Limit(BigDecimal("123.45")).price).isEqualByComparingTo("123.45")
        }

        @Test
        fun `StopLoss stores stopPrice with default zero`() {
            assertThat(StopLoss().stopPrice).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        fun `StopLoss stores explicit stopPrice`() {
            assertThat(StopLoss(BigDecimal("500")).stopPrice).isEqualByComparingTo("500")
        }

        @Test
        fun `StopLimit stores both stopPrice and limitPrice`() {
            val sl = StopLimit(stopPrice = BigDecimal("300"), limitPrice = BigDecimal("295"))
            assertThat(sl.stopPrice).isEqualByComparingTo("300")
            assertThat(sl.limitPrice).isEqualByComparingTo("295")
        }

        @Test
        fun `MarketOnClose is an instance of OrderType`() {
            assertThat(MarketOnClose()).isInstanceOf(OrderType::class.java)
        }

        @Test
        fun `LimitOnClose stores price`() {
            assertThat(LimitOnClose(BigDecimal("999")).price).isEqualByComparingTo("999")
        }

        @Test
        fun `two Limit objects with same price are equal`() {
            assertThat(Limit(BigDecimal("50"))).isEqualTo(Limit(BigDecimal("50")))
        }

        @Test
        fun `two Limit objects with different prices are not equal`() {
            assertThat(Limit(BigDecimal("50"))).isNotEqualTo(Limit(BigDecimal("51")))
        }

        @Test
        fun `two StopLimit objects with same fields are equal`() {
            val sl1 = StopLimit(BigDecimal("10"), BigDecimal("9"))
            val sl2 = StopLimit(BigDecimal("10"), BigDecimal("9"))
            assertThat(sl1).isEqualTo(sl2)
        }
    }
}
