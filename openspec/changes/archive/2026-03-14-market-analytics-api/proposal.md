## Why

The platform currently exposes order and trade data but provides no aggregated market analytics. Traders and UIs need real-time market summary data (open/high/low/close prices and volume) to make informed decisions and render charts without computing these values client-side.

## What Changes

- Introduce a new `/api/v1/market/analytics` REST endpoint on the `crypto-exchange-server` that returns current market analytics per trading symbol
- Compute analytics (high, low, current/last price, volume) from the in-memory `OrderBook` and persisted `TradeEntity` records
- Expose analytics via OpenAPI spec so the generated controller interface is available

## Capabilities

### New Capabilities
- `market-analytics`: Read-only API endpoint that returns aggregated market data (high, low, last price, 24h volume) for each trading symbol, computed from trade history and current order book state

### Modified Capabilities
<!-- none -->

## Impact

- **crypto-exchange-server**: New `MarketAnalyticsController`, `MarketAnalyticsService`, and `MarketAnalytics` model; updated `crypto-exchange-openapi.yaml`
- **crypto-exchange-server-docker**: No changes needed; inherits new endpoint
- **Frontend**: Can consume new endpoint to display live market summary (out of scope for this change)
- **Dependencies**: No new dependencies; uses existing `TradeEntity` JPA repository and `Exchange`/`OrderBook` in-memory state
