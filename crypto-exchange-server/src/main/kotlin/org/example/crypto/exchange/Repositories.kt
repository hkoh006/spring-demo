package org.example.crypto.exchange

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
interface OrderRepository : JpaRepository<OrderEntity, String>

interface TradeAggregates {
    fun getHigh(): BigDecimal?

    fun getLow(): BigDecimal?

    fun getVolume(): BigDecimal?
}

@Repository
interface TradeRepository : JpaRepository<TradeEntity, String> {
    @Query(
        "SELECT MAX(t.price) AS high, MIN(t.price) AS low, SUM(t.quantity) AS volume " +
            "FROM TradeEntity t WHERE t.timestamp >= :since",
    )
    fun findAggregatesSince(
        @Param("since") since: Instant,
    ): TradeAggregates

    fun findTopByOrderByTimestampDesc(): TradeEntity?
}
