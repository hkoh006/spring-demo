package org.example.crypto.exchange.controller

import org.example.crypto.exchange.TradeRepository
import org.example.crypto.exchange.api.TradeEndpointApi
import org.example.crypto.exchange.model.TradeEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@Component
class TradeEndpoint(
    private val tradeRepository: TradeRepository,
) : TradeEndpointApi {
    override fun getAllTrades(): ResponseEntity<List<TradeEntity>> =
        ResponseEntity.ok(
            tradeRepository.findAll().map {
                TradeEntity(
                    id = it.id,
                    buyerId = it.buyerId,
                    sellerId = it.sellerId,
                    price = it.price,
                    quantity = it.quantity,
                    timestamp = it.timestamp.atOffset(ZoneOffset.UTC),
                )
            },
        )

    override fun getTradeById(id: String): ResponseEntity<TradeEntity> =
        tradeRepository
            .findById(id)
            .getOrNull()
            ?.let {
                ResponseEntity.ok(
                    TradeEntity(
                        id = it.id,
                        buyerId = it.buyerId,
                        sellerId = it.sellerId,
                        price = it.price,
                        quantity = it.quantity,
                        timestamp = it.timestamp.atOffset(ZoneOffset.UTC),
                    ),
                )
            }
            ?: ResponseEntity.notFound().build()
}
