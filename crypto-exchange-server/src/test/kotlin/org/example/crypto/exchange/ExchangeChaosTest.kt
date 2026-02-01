package org.example.crypto.exchange

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random

class ExchangeChaosTest {
    private val random = Random()

    @Test
    fun `chaos test - random orders and invariant checks`() {
        val exchange = Exchange()
        val numOrders = 10000

        var totalBuyQuantity = BigDecimal.ZERO
        var totalSellQuantity = BigDecimal.ZERO

        var executedTradeQuantity = BigDecimal.ZERO

        repeat(numOrders) {
            val side = if (random.nextBoolean()) OrderSide.BUY else OrderSide.SELL
            val price = BigDecimal(random.nextInt(10) + 95).setScale(2) // Price between 95 and 105
            val quantity = BigDecimal(random.nextDouble() * 10 + 0.1).setScale(8, RoundingMode.HALF_UP)

            val order =
                Order(
                    userId = "user-${random.nextInt(10)}",
                    side = side,
                    price = price,
                    quantity = quantity,
                )

            if (side == OrderSide.BUY) {
                totalBuyQuantity += quantity
            } else {
                totalSellQuantity += quantity
            }

            val trades = exchange.placeOrder(order)
            trades.forEach { trade ->
                executedTradeQuantity += trade.quantity
            }

            // Invariant 1: Order book should never have crossing prices
            val orderBook = exchange.getOrderBook()
            if (orderBook.bids.isNotEmpty() && orderBook.asks.isNotEmpty()) {
                val bestBid = orderBook.bids.first().price
                val bestAsk = orderBook.asks.first().price
                assertTrue(bestBid < bestAsk, "Order book crossed: Bid $bestBid >= Ask $bestAsk")
            }

            // Invariant 3: Price priority
            if (orderBook.bids.size > 1) {
                val prices = orderBook.bids.map { it.price }
                for (i in 0 until prices.size - 1) {
                    assertTrue(prices[i] >= prices[i + 1], "Bids price priority failed: ${prices[i]} < ${prices[i + 1]}")
                }
            }
            if (orderBook.asks.size > 1) {
                val prices = orderBook.asks.map { it.price }
                for (i in 0 until prices.size - 1) {
                    assertTrue(prices[i] <= prices[i + 1], "Asks price priority failed: ${prices[i]} > ${prices[i + 1]}")
                }
            }
        }

        val orderBook = exchange.getOrderBook()
        val remainingBuyQuantity = orderBook.bids.sumOf { it.remainingQuantity }
        val remainingSellQuantity = orderBook.asks.sumOf { it.remainingQuantity }

        // Invariant 2: Conservation of quantity
        // totalBuyQuantity = executedTradeQuantity + remainingBuyQuantity
        // totalSellQuantity = executedTradeQuantity + remainingSellQuantity

        assertEquals(
            totalBuyQuantity.stripTrailingZeros(),
            (executedTradeQuantity + remainingBuyQuantity).stripTrailingZeros(),
            "Buy quantity mismatch",
        )
        assertEquals(
            totalSellQuantity.stripTrailingZeros(),
            (executedTradeQuantity + remainingSellQuantity).stripTrailingZeros(),
            "Sell quantity mismatch",
        )

        println("Chaos test passed with $numOrders orders.")
        println("Total trades: $executedTradeQuantity")
        println("Remaining bids: $remainingBuyQuantity")
        println("Remaining asks: $remainingSellQuantity")
    }
}

private fun Iterable<Order>.sumOf(selector: (Order) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
