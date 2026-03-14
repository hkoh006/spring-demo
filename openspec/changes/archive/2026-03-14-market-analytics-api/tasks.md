## 1. OpenAPI Spec

- [x] 1.1 Add `MarketAnalyticsDto` schema to `crypto-exchange-openapi.yaml` with fields: `high`, `low`, `lastPrice` (nullable numbers), `volume` (non-null number), `bestBid`, `bestAsk` (nullable numbers)
- [x] 1.2 Add `GET /api/market/analytics` path to `crypto-exchange-openapi.yaml` returning `MarketAnalyticsDto`

## 2. Repository Query

- [x] 2.1 Add a `findAnalyticsSince(since: Instant)` query method to `TradeRepository` (or a dedicated `@Query`) that returns max price, min price, sum of quantity, and most recent trade price for trades after the given timestamp

## 3. Service Layer

- [x] 3.1 Create `MarketAnalyticsService` that calls `TradeRepository` for 24h aggregates and reads `Exchange.getOrderBook()` for best bid/ask, returning a `MarketAnalyticsDto`

## 4. Controller

- [x] 4.1 Create `MarketAnalyticsController` implementing the OpenAPI-generated `MarketAnalyticsEndpoint` interface, delegating to `MarketAnalyticsService`

## 5. Tests

- [x] 5.1 Write a unit test for `MarketAnalyticsService` using MockK to verify correct aggregation logic and null handling when no trades exist
- [x] 5.2 Write an integration test for `GET /api/market/analytics` using Testcontainers (PostgreSQL + Kafka) verifying HTTP 200 and correct field values with seeded trade data
