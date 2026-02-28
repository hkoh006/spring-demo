package org.example.crypto.exchange.controller

import org.example.crypto.exchange.TradeRepository
import org.example.crypto.exchange.api.TradeEndpointApi
import org.example.crypto.exchange.model.TradeEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api/trades")
class TradeEndpoint(
    private val tradeRepository: TradeRepository,
) : TradeEndpointApi {
    @GetMapping
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

    @GetMapping("/{id}")
    override fun getTradeById(
        @PathVariable id: String,
    ): ResponseEntity<TradeEntity> =
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
