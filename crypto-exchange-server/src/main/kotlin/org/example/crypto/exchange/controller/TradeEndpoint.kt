package org.example.crypto.exchange.controller

import org.example.crypto.exchange.TradeEntity
import org.example.crypto.exchange.TradeRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/trades")
class TradeEndpoint(
    private val tradeRepository: TradeRepository,
) {
    @GetMapping
    fun getAllTrades(): List<TradeEntity> = tradeRepository.findAll()

    @GetMapping("/{id}")
    fun getTradeById(
        @PathVariable id: String,
    ): ResponseEntity<TradeEntity> =
        tradeRepository
            .findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
}
