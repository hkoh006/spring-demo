package org.example.crypto.exchange

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Pure unit tests for [TradeEntity] — no Spring context needed.
 *
 * Mirrors the structure of [OrderEntityTest] and verifies default field values,
 * JPA no-arg constructor, and data-class equality semantics.
 */
class TradeEntityTest {
    // -------------------------------------------------------------------------
    // Default field values
    // -------------------------------------------------------------------------

    @Nested
    inner class DefaultFields {
        @Test
        fun `id is non-blank by default`() {
            val trade = trade()
            assertThat(trade.id).isNotBlank()
        }

        @Test
        fun `two trades created without explicit ids have different ids`() {
            val t1 = trade()
            val t2 = trade()
            assertThat(t1.id).isNotEqualTo(t2.id)
        }

        @Test
        fun `timestamp defaults to approximately now`() {
            val before = Instant.now()
            val trade = trade()
            val after = Instant.now()
            assertThat(trade.timestamp).isBetween(before, after)
        }

        @Test
        fun `buyerId is stored as supplied`() {
            val trade = trade(buyerId = "buyer-99")
            assertThat(trade.buyerId).isEqualTo("buyer-99")
        }

        @Test
        fun `sellerId is stored as supplied`() {
            val trade = trade(sellerId = "seller-77")
            assertThat(trade.sellerId).isEqualTo("seller-77")
        }

        @Test
        fun `price is stored as supplied`() {
            val price = BigDecimal("123.45")
            val trade = trade(price = price)
            assertThat(trade.price).isEqualByComparingTo(price)
        }

        @Test
        fun `quantity is stored as supplied`() {
            val qty = BigDecimal("0.75")
            val trade = trade(quantity = qty)
            assertThat(trade.quantity).isEqualByComparingTo(qty)
        }
    }

    // -------------------------------------------------------------------------
    // No-arg constructor (required by JPA)
    // -------------------------------------------------------------------------

    @Test
    fun `no-arg constructor produces an instance without throwing`() {
        val trade = TradeEntity()
        assertThat(trade).isNotNull()
        assertThat(trade.id).isEmpty()
        assertThat(trade.buyerId).isEmpty()
        assertThat(trade.sellerId).isEmpty()
        assertThat(trade.price).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(trade.quantity).isEqualByComparingTo(BigDecimal.ZERO)
    }

    // -------------------------------------------------------------------------
    // Data class equality
    // -------------------------------------------------------------------------

    @Nested
    inner class Equality {
        @Test
        fun `two trades with the same fields are equal`() {
            val id = "fixed-trade-id"
            val ts = Instant.parse("2024-01-01T00:00:00Z")
            val t1 = trade(id = id, timestamp = ts)
            val t2 = trade(id = id, timestamp = ts)
            assertThat(t1).isEqualTo(t2)
        }

        @Test
        fun `trades with different ids are not equal`() {
            val ts = Instant.parse("2024-01-01T00:00:00Z")
            val t1 = trade(id = "id-A", timestamp = ts)
            val t2 = trade(id = "id-B", timestamp = ts)
            assertThat(t1).isNotEqualTo(t2)
        }

        @Test
        fun `trades with different buyer ids are not equal`() {
            val id = "same-id"
            val t1 = trade(id = id, buyerId = "buyer-1")
            val t2 = trade(id = id, buyerId = "buyer-2")
            assertThat(t1).isNotEqualTo(t2)
        }

        @Test
        fun `trades with different prices are not equal`() {
            val id = "same-id"
            val ts = Instant.parse("2024-01-01T00:00:00Z")
            val t1 = trade(id = id, price = BigDecimal("100"), timestamp = ts)
            val t2 = trade(id = id, price = BigDecimal("200"), timestamp = ts)
            assertThat(t1).isNotEqualTo(t2)
        }

        @Test
        fun `hashCode is consistent with equals`() {
            val id = "fixed-id"
            val ts = Instant.parse("2024-01-01T00:00:00Z")
            val t1 = trade(id = id, timestamp = ts)
            val t2 = trade(id = id, timestamp = ts)
            assertThat(t1.hashCode()).isEqualTo(t2.hashCode())
        }
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    fun `toString should contain the trade id`() {
        val trade = trade(id = "trade-abc")
        assertThat(trade.toString()).contains("trade-abc")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun trade(
        id: String = UUID.randomUUID().toString(),
        buyerId: String = "buyer",
        sellerId: String = "seller",
        price: BigDecimal = BigDecimal("100"),
        quantity: BigDecimal = BigDecimal("1.0"),
        timestamp: Instant = Instant.now(),
    ) = TradeEntity(
        id = id,
        buyerId = buyerId,
        sellerId = sellerId,
        price = price,
        quantity = quantity,
        timestamp = timestamp,
    )
}
