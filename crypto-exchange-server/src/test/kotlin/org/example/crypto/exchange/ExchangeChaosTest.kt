package org.example.crypto.exchange

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random

@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Import(TestcontainersPostgresConfig::class)
class ExchangeChaosTest {
    private val random = Random()

    @Autowired
    private lateinit var exchange: Exchange

    @BeforeEach
    fun clearDatabase() {
        exchange.clear()
    }

    @Test
    fun `chaos test - random orders and invariant checks`() {
        val numOrders = 100

        var totalBuyQuantity = BigDecimal.ZERO
        var totalSellQuantity = BigDecimal.ZERO

        var executedTradeQuantity = BigDecimal.ZERO

        repeat(numOrders) {
            val side = if (random.nextBoolean()) OrderSide.BUY else OrderSide.SELL
            val price = BigDecimal(random.nextInt(10) + 95).setScale(2) // Price between 95 and 105
            val quantity = BigDecimal(random.nextDouble() * 10 + 0.1).setScale(8, RoundingMode.HALF_UP)

            val order =
                OrderEntity(
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
                assertThat(bestBid < bestAsk)
                    .withFailMessage("Order book crossed: Bid $bestBid >= Ask $bestAsk")
                    .isTrue()
            }

            // Invariant 3: Price priority
            if (orderBook.bids.size > 1) {
                val prices = orderBook.bids.map { it.price }
                for (i in 0 until prices.size - 1) {
                    assertThat(prices[i] >= prices[i + 1])
                        .withFailMessage("Bids price priority failed: ${prices[i]} < ${prices[i + 1]}")
                        .isTrue()
                }
            }
            if (orderBook.asks.size > 1) {
                val prices = orderBook.asks.map { it.price }
                for (i in 0 until prices.size - 1) {
                    assertThat(prices[i] <= prices[i + 1])
                        .withFailMessage("Asks price priority failed: ${prices[i]} > ${prices[i + 1]}")
                        .isTrue()
                }
            }
        }

        val orderBook = exchange.getOrderBook()
        val remainingBuyQuantity = orderBook.bids.sumOf { it.remainingQuantity }
        val remainingSellQuantity = orderBook.asks.sumOf { it.remainingQuantity }

        // Invariant 2: Conservation of quantity
        // totalBuyQuantity = executedTradeQuantity + remainingBuyQuantity
        // totalSellQuantity = executedTradeQuantity + remainingSellQuantity

        assertThat((executedTradeQuantity + remainingBuyQuantity).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros())
            .withFailMessage("Buy quantity mismatch")
            .isEqualTo(totalBuyQuantity.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros())

        assertThat((executedTradeQuantity + remainingSellQuantity).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros())
            .withFailMessage("Sell quantity mismatch")
            .isEqualTo(totalSellQuantity.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros())

        println("Chaos test passed with $numOrders orders.")
        println("Total trades: $executedTradeQuantity")
        println("Remaining bids: $remainingBuyQuantity")
        println("Remaining asks: $remainingSellQuantity")
    }
}

private fun Iterable<OrderEntity>.sumOf(selector: (OrderEntity) -> BigDecimal): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
