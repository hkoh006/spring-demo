package org.example.crypto.exchange

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExchangeTest {
    @Test
    fun `test simple match`() {
        val exchange = Exchange()

        val sellOrder =
            Order(
                userId = "seller",
                side = OrderSide.SELL,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.5"),
            )
        exchange.placeOrder(sellOrder)

        val buyOrder =
            Order(
                userId = "buyer",
                side = OrderSide.BUY,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.0"),
            )
        val trades = exchange.placeOrder(buyOrder)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("1.0"), trades[0].quantity)
        assertEquals(BigDecimal("100"), trades[0].price)
        assertEquals("buyer", trades[0].buyerId)
        assertEquals("seller", trades[0].sellerId)

        assertTrue(buyOrder.isFilled())
        assertEquals(BigDecimal("0.5"), sellOrder.remainingQuantity)
        assertEquals(1, exchange.getOrderBook().asks.size)
        assertEquals(0, exchange.getOrderBook().bids.size)
    }

    @Test
    fun `test price priority`() {
        val exchange = Exchange()

        // Higher sell price
        exchange.placeOrder(
            Order(
                userId = "seller1",
                side = OrderSide.SELL,
                price = BigDecimal("101"),
                quantity = BigDecimal("1.0"),
            ),
        )

        // Lower sell price (should be matched first)
        exchange.placeOrder(
            Order(
                userId = "seller2",
                side = OrderSide.SELL,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.0"),
            ),
        )

        val buyOrder =
            Order(
                userId = "buyer",
                side = OrderSide.BUY,
                price = BigDecimal("105"),
                quantity = BigDecimal("1.5"),
            )
        val trades = exchange.placeOrder(buyOrder)

        assertEquals(2, trades.size)
        assertEquals("seller2", trades[0].sellerId) // Price 100 matched first
        assertEquals(BigDecimal("1.0"), trades[0].quantity)

        assertEquals("seller1", trades[1].sellerId) // Price 101 matched second
        assertEquals(BigDecimal("0.5"), trades[1].quantity)
    }

    @Test
    fun `test order book remains correct after partial match`() {
        val exchange = Exchange()

        exchange.placeOrder(
            Order(
                userId = "seller",
                side = OrderSide.SELL,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.0"),
            ),
        )

        val buyOrder =
            Order(
                userId = "buyer",
                side = OrderSide.BUY,
                price = BigDecimal("100"),
                quantity = BigDecimal("2.0"),
            )
        val trades = exchange.placeOrder(buyOrder)

        assertEquals(1, trades.size)
        assertEquals(BigDecimal("1.0"), buyOrder.remainingQuantity)
        assertEquals(0, exchange.getOrderBook().asks.size)
        assertEquals(1, exchange.getOrderBook().bids.size)
        assertEquals(buyOrder, exchange.getOrderBook().bids.first())
    }
}
