## Context

The `crypto-exchange-server` runs a single in-memory `OrderBook` backed by a PostgreSQL `trades` table. It currently exposes order and trade CRUD endpoints generated from `crypto-exchange-openapi.yaml`. There is no concept of trading symbols/pairs — all orders are for a single implicit market.

`TradeEntity` records carry `price`, `quantity`, and `timestamp`, making them the natural source for high/low/volume aggregations. `Exchange.getOrderBook()` exposes the live in-memory `OrderBook`, from which best bid and best ask can be read without a DB query.

## Goals / Non-Goals

**Goals:**
- Add `GET /api/market/analytics` returning high, low, last trade price, 24-hour volume, best bid, and best ask
- Compute analytics from existing data sources with no schema migration
- Follow the existing OpenAPI-first pattern: define in `crypto-exchange-openapi.yaml`, implement the generated interface

**Non-Goals:**
- Multi-symbol / trading-pair support (single market only)
- Real-time streaming / WebSocket push of analytics
- OHLCV candlestick history; only the current 24-hour window summary is in scope
- Persisting computed analytics (computed on-demand)

## Decisions

### 1. Data sources: DB for trade aggregates, in-memory for order book
Query `TradeRepository` for 24h trade data (high, low, last price, volume); read `Exchange.getOrderBook()` for best bid/ask.

**Rationale**: Trades are already persisted with `timestamp`. The order book is only in-memory. A single synchronized read of `Exchange` state is safe because `placeOrder` and `clear` are also `@Synchronized`.

**Alternative**: Maintain a running `MarketAnalytics` state inside `Exchange` updated on every trade. Rejected: couples analytics logic to the hot matching path.

### 2. Time window: rolling 24 hours from query time
High, low, and volume are computed over trades where `timestamp >= now() - 24h`. Last price uses the most recent trade overall.

**Rationale**: Consistent with industry convention for "24h analytics". Implementable with a single `@Query` using `Instant.now().minus(24, HOURS)`.

**Alternative**: Since server start. Rejected: restarts would produce misleading values.

### 3. Response when no trades exist
Return `200 OK` with `null` for `high`, `low`, `lastPrice`, and `bestBid`/`bestAsk`; `volume = 0`.

**Rationale**: An empty or 404 response forces clients to treat a valid (just empty) market state as an error. Nullable fields with a zero volume are cleaner.

### 4. OpenAPI-first controller pattern
Add the new path and `MarketAnalyticsDto` schema to `crypto-exchange-openapi.yaml`. The OpenAPI Generator Gradle plugin generates the `MarketAnalyticsEndpoint` interface; a `MarketAnalyticsController` bean implements it.

**Rationale**: Consistent with `OrderEndpoint`/`TradeEndpoint` pattern already in place.

## Risks / Trade-offs

- **Performance of 24h query** → For high trade volumes, a full scan on `trades` filtered by `timestamp` may be slow. Mitigation: add an index on `trades.timestamp` if latency becomes an issue (out of scope here).
- **Single-market assumption** → If multi-symbol support is added later, this endpoint will need versioning or a `symbol` query parameter.
- **In-memory best bid/ask staleness** → Values come from the live `OrderBook` which is rebuilt on restart from unfilled orders, so they remain consistent after warmup.
