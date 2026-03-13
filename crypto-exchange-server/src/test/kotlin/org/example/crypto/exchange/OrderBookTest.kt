package org.example.crypto.exchange

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Pure unit tests for [OrderBook] — no Spring context needed.
 *
 * Key invariants verified:
 *  - Bids (BUY side) are sorted highest price first, then oldest first.
 *  - Asks (SELL side) are sorted lowest price first, then oldest first.
 *  - addOrder routes BUY orders to bids and SELL orders to asks.
 */
class OrderBookTest {
    private lateinit var book: OrderBook

    @BeforeEach
    fun setUp() {
        book = OrderBook()
    }

    // -------------------------------------------------------------------------
    // addOrder routing
    // -------------------------------------------------------------------------

    @Nested
    inner class AddOrder {
        @Test
        fun `should add BUY order to bids`() {
            val order = buyOrder(price = "100", id = "b1")
            book.addOrder(order)

            assertThat(book.bids).containsExactly(order)
            assertThat(book.asks).isEmpty()
        }

        @Test
        fun `should add SELL order to asks`() {
            val order = sellOrder(price = "100", id = "s1")
            book.addOrder(order)

            assertThat(book.asks).containsExactly(order)
            assertThat(book.bids).isEmpty()
        }

        @Test
        fun `should add multiple orders of both sides correctly`() {
            val buy = buyOrder(price = "100", id = "b1")
            val sell = sellOrder(price = "101", id = "s1")
            book.addOrder(buy)
            book.addOrder(sell)

            assertThat(book.bids).containsExactly(buy)
            assertThat(book.asks).containsExactly(sell)
        }
    }

    // -------------------------------------------------------------------------
    // Bid ordering (highest price first, then oldest timestamp first)
    // -------------------------------------------------------------------------

    @Nested
    inner class BidOrdering {
        @Test
        fun `bids should be ordered highest price first`() {
            val low = buyOrder(price = "99", id = "b-low")
            val high = buyOrder(price = "101", id = "b-high")
            val mid = buyOrder(price = "100", id = "b-mid")

            book.addOrder(low)
            book.addOrder(high)
            book.addOrder(mid)

            val prices = book.bids.map { it.price }
            assertThat(prices).containsExactly(
                BigDecimal("101"),
                BigDecimal("100"),
                BigDecimal("99"),
            )
        }

        @Test
        fun `bids at the same price should be ordered oldest first`() {
            val t0 = Instant.ofEpochSecond(1000)
            val t1 = Instant.ofEpochSecond(2000)

            val older = buyOrder(price = "100", id = "b-older", timestamp = t0)
            val newer = buyOrder(price = "100", id = "b-newer", timestamp = t1)

            // Insert newer first to ensure the TreeSet sorts, not insertion order
            book.addOrder(newer)
            book.addOrder(older)

            assertThat(book.bids.first()).isEqualTo(older)
            assertThat(book.bids.last()).isEqualTo(newer)
        }

        @Test
        fun `bids with same price and timestamp are differentiated by id`() {
            val t = Instant.ofEpochSecond(1000)
            val o1 = buyOrder(price = "100", id = "aaa", timestamp = t)
            val o2 = buyOrder(price = "100", id = "bbb", timestamp = t)

            book.addOrder(o2)
            book.addOrder(o1)

            // Both should be present (TreeSet de-duplication only happens for equal comparator results)
            assertThat(book.bids).hasSize(2)
        }
    }

    // -------------------------------------------------------------------------
    // Ask ordering (lowest price first, then oldest timestamp first)
    // -------------------------------------------------------------------------

    @Nested
    inner class AskOrdering {
        @Test
        fun `asks should be ordered lowest price first`() {
            val high = sellOrder(price = "102", id = "s-high")
            val low = sellOrder(price = "100", id = "s-low")
            val mid = sellOrder(price = "101", id = "s-mid")

            book.addOrder(high)
            book.addOrder(low)
            book.addOrder(mid)

            val prices = book.asks.map { it.price }
            assertThat(prices).containsExactly(
                BigDecimal("100"),
                BigDecimal("101"),
                BigDecimal("102"),
            )
        }

        @Test
        fun `asks at the same price should be ordered oldest first`() {
            val t0 = Instant.ofEpochSecond(1000)
            val t1 = Instant.ofEpochSecond(2000)

            val older = sellOrder(price = "100", id = "s-older", timestamp = t0)
            val newer = sellOrder(price = "100", id = "s-newer", timestamp = t1)

            book.addOrder(newer)
            book.addOrder(older)

            assertThat(book.asks.first()).isEqualTo(older)
            assertThat(book.asks.last()).isEqualTo(newer)
        }
    }

    // -------------------------------------------------------------------------
    // Empty book
    // -------------------------------------------------------------------------

    @Test
    fun `new OrderBook has empty bids and asks`() {
        assertThat(book.bids).isEmpty()
        assertThat(book.asks).isEmpty()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buyOrder(
        price: String,
        id: String,
        timestamp: Instant = Instant.now(),
    ) = OrderEntity(
        id = id,
        userId = "user",
        side = OrderSide.BUY,
        price = BigDecimal(price),
        quantity = BigDecimal("1.0"),
        timestamp = timestamp,
    )

    private fun sellOrder(
        price: String,
        id: String,
        timestamp: Instant = Instant.now(),
    ) = OrderEntity(
        id = id,
        userId = "user",
        side = OrderSide.SELL,
        price = BigDecimal(price),
        quantity = BigDecimal("1.0"),
        timestamp = timestamp,
    )
}
