package org.example.crypto.exchange.controller

import org.example.crypto.exchange.MarketAnalyticsService
import org.example.crypto.exchange.api.MarketAnalyticsEndpointApi
import org.example.crypto.exchange.model.MarketAnalyticsDto
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class MarketAnalyticsEndpoint(
    private val marketAnalyticsService: MarketAnalyticsService,
) : MarketAnalyticsEndpointApi {
    override fun getMarketAnalytics(): ResponseEntity<MarketAnalyticsDto> = ResponseEntity.ok(marketAnalyticsService.getAnalytics())
}
