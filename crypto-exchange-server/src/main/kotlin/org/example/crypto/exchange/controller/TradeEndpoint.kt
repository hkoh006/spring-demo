package org.example.crypto.exchange.controller

import org.example.crypto.exchange.TradeRepository
import org.example.crypto.exchange.api.TradeEndpointApi
import org.example.crypto.exchange.model.TradeDto
import org.example.crypto.exchange.model.TradeEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@Component
class TradeEndpoint(
    private val tradeRepository: TradeRepository,
) : TradeEndpointApi {
    override fun getAllTrades(): ResponseEntity<List<TradeDto>> =
        ResponseEntity.ok(
            tradeRepository.findAll().map {
                TradeDto(
                    id = it.id,
                    buyerId = it.buyerId,
                    sellerId = it.sellerId,
                    price = it.price,
                    quantity = it.quantity,
                    timestamp = it.timestamp.atOffset(ZoneOffset.UTC),
                )
            },
        )

    override fun getTradeById(id: String): ResponseEntity<TradeDto> =
        tradeRepository
            .findById(id)
            .getOrNull()
            ?.let {
                ResponseEntity.ok(
                    TradeDto(
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
