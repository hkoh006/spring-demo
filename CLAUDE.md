# Spring Demo – Project Memory

## Module Structure

Multi-module Gradle project with 7 modules:

### Library Modules (JAR only, no bootJar)
- **web-service**: JPA/Blaze-Persistence service, order entities, REST controllers, WebSocket
- **crypto-exchange-server**: Order matching engine, Kafka consumer, OpenAPI-generated endpoints
- **order-generator**: Scheduled Kafka producer generating random order events

### Executable Modules (Spring Boot apps)
- **web-service-docker:local**: Auto-provisions PostgreSQL via Testcontainers; Scalar UI enabled
- **web-service-docker:server**: Expects external PostgreSQL; production-like
- **crypto-exchange-server-docker:local**: Auto-provisions PostgreSQL + Kafka via Testcontainers
- **crypto-exchange-server-docker:server**: Production-like exchange server

### Frontend
- **crypto-frontend**: Next.js React UI at `frontend/crypto-frontend`

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.2.20 |
| Framework | Spring Boot | 4.0.2 |
| ORM | Hibernate + Spring Data JPA | 7.1 |
| Advanced Queries | Blaze-Persistence | 1.6.17 |
| Type-safe Queries | QueryDSL | 5.1.0 (Jakarta) |
| JSONB Support | Hypersistence Utils | 3.15.1 |
| Database | PostgreSQL | 17 |
| Messaging | Apache Kafka + Protobuf | 3.25.5 |
| Schema Registry | Confluent | 7.8.0 |
| API Docs | SpringDoc OpenAPI + Scalar UI | 3.0.1 |
| Testing | JUnit 6.0.2 + Testcontainers 2.0.3 + MockK 1.13.12 | — |

## Order Type Model

### web-service (`OrderEntity.kt`)
The `OrderType` sealed interface uses Jackson polymorphic type info (`@JsonTypeInfo` / `@JsonTypeName`) and is stored as JSONB in PostgreSQL.

Supported subtypes:
| Class | JSON discriminator | Fields |
|---|---|---|
| `Market` | `MARKET` | — |
| `Limit` | `LIMIT` | `price: BigDecimal` |
| `StopLoss` | `STOP_LOSS` | `stopPrice: BigDecimal` |
| `StopLimit` | `STOP_LIMIT` | `stopPrice`, `limitPrice: BigDecimal` |
| `MarketOnClose` | `MARKET_ON_CLOSE` | — |
| `LimitOnClose` | `LIMIT_ON_CLOSE` | `price: BigDecimal` |

### order-generator / crypto-exchange-server (Protobuf)
`order_event.proto` defines `OrderTypeProto` enum with `MARKET` (0) and `LIMIT` (1) values.
`OrderEventProto` carries `order_type` (field 5) in addition to `user_id`, `side`, `price`, and `quantity`.
`OrderGeneratorService` randomly assigns `MARKET` or `LIMIT` on each generated event.

## Order Processing Pipeline

1. `OrderGeneratorService` generates random MARKET/LIMIT orders on a schedule
2. Published to Kafka "orders" topic as `OrderEventProto` (Protobuf)
3. `OrderSubscriber` in crypto-exchange-server consumes events
4. `Exchange` service matches orders against in-memory `OrderBook`
5. Trades created and persisted to PostgreSQL
6. Orders and trades accessible via REST APIs

## Key Architectural Patterns

- **JSONB polymorphism**: `OrderType` sealed interface stored as JSONB in PostgreSQL with Jackson `@JsonTypeInfo`/`@JsonTypeName`
- **Custom JPQL functions**: `PgJsonbContainsFunction` (`@>`), `PgJsonbContainsAnyOfFunction` (`??|`), registered in `BlazePersistenceConfig`
- **Code generation**: QueryDSL Q-classes via KSP; REST controllers from OpenAPI YAML via OpenAPI Generator plugin
- **Dual repository pattern**: `OrderJpaRepository` (standard JPA) + `OrderJdbcRepository` (Blaze-Persistence/QueryDSL for JSONB queries)
- **Thread-safe order matching**: `Exchange.placeOrder()` uses `@Synchronized` + `@Transactional`
- **Self-trade prevention**: `Exchange` skips order book entries where `userId` matches the incoming order's `userId`; the skipped order remains in the book for other users to match against

## Key File Locations

### web-service
| File | Purpose |
|---|---|
| `web-service/src/main/kotlin/.../dao/model/OrderEntity.kt` | `OrderEntity`, `OrderDetails`, `Allocation`, `OrderType` sealed interface + 6 subtypes |
| `web-service/src/main/kotlin/.../dao/OrderJpaRepository.kt` | Standard JPA repository |
| `web-service/src/main/kotlin/.../dao/OrderJdbcRepository.kt` | Advanced Blaze-Persistence/QueryDSL with JSONB queries |
| `web-service/src/main/kotlin/.../config/BlazePersistenceConfig.kt` | Registers custom PostgreSQL JSONB JPQL functions |
| `web-service/src/main/kotlin/.../controller/OrderController.kt` | REST CRUD at `/api/v1/orders` |
| `web-service/src/main/kotlin/.../ws/EchoWebSocketHandler.kt` | WebSocket echo at `/ws/echo` |

### crypto-exchange-server
| File | Purpose |
|---|---|
| `crypto-exchange-server/src/main/proto/order_event.proto` | Protobuf definitions for `OrderEventProto`, `OrderSideProto`, `OrderTypeProto` |
| `crypto-exchange-server/src/main/kotlin/.../Exchange.kt` | Core order matching engine service |
| `crypto-exchange-server/src/main/kotlin/.../Models.kt` | `OrderEntity`, `TradeEntity`, `OrderBook` (in-memory TreeSets) |
| `crypto-exchange-server/src/main/kotlin/.../messaging/OrderSubscriber.kt` | Kafka consumer on "orders" topic |
| `crypto-exchange-server/src/main/resources/crypto-exchange-openapi.yaml` | OpenAPI spec (controllers generated from this) |

### order-generator
| File | Purpose |
|---|---|
| `order-generator/src/main/kotlin/.../OrderGeneratorService.kt` | `@Scheduled` random order generator (MARKET or LIMIT) |
| `order-generator/src/main/kotlin/.../OrderPublisher.kt` | Kafka producer with Protobuf serialization |
| `order-generator/src/main/kotlin/.../OrderGeneratorProperties.kt` | Configurable min/max prices, quantities, users, topic |

## Testing

- **Stack**: JUnit 5 + MockK (not Mockito) + AssertJ + Testcontainers (`@ServiceConnection`)
- **Testcontainers pattern**: `PostgreSQLContainer` / Kafka container with `@ServiceConnection` for auto-wired config; used in both `web-service` and `crypto-exchange-server` test source sets

### Key Test Files

**web-service**: `OrderTypeSerializationTest`, `OrderDetailsTest`, `OrderEntityTest`, `OrderJdbcRepositoryJsonLiteralTest`, `OrderControllerTest`, `OrderControllerUnitTest`, `EchoWebSocketHandlerTest`, `PgJsonbContainsFunctionTest`

**crypto-exchange-server**: `ExchangeTest`, `ExchangeUnitTest`, `ExchangeChaosTest` (thread-safety), `OrderBookTest`, `OrderSubscriberTest`, `OrderEndpointUnitTest`, `TradeEndpointUnitTest`

**order-generator**: `OrderGeneratorServiceTest` (`@RepeatedTest(20)` for order-type assertion), `OrderPublisherTest`
