package org.example.crypto.exchange

import org.example.crypto.exchange.model.MarketAnalyticsDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class MarketAnalyticsService(
    private val tradeRepository: TradeRepository,
    private val exchange: Exchange,
) {
    fun getAnalytics(): MarketAnalyticsDto {
        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val aggregates = tradeRepository.findAggregatesSince(since)
        val lastTrade = tradeRepository.findTopByOrderByTimestampDesc()
        val orderBook = exchange.getOrderBook()

        return MarketAnalyticsDto(
            high = aggregates.getHigh(),
            low = aggregates.getLow(),
            lastPrice = lastTrade?.price,
            volume = aggregates.getVolume() ?: BigDecimal.ZERO,
            bestBid = orderBook.bids.firstOrNull()?.price,
            bestAsk = orderBook.asks.firstOrNull()?.price,
        )
    }
}
