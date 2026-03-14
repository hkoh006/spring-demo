# Spring Demo – Project Memory

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

## Key File Locations
- Proto definition: `crypto-exchange-server/src/main/proto/order_event.proto`
- Order model + types: `web-service/src/main/kotlin/org/example/spring/demo/dao/model/OrderEntity.kt`
- Generator service: `order-generator/src/main/kotlin/org/example/crypto/exchange/generator/OrderGeneratorService.kt`

## Testing
- Serialization round-trips: `OrderTypeSerializationTest`
- Domain unit tests: `OrderDetailsTest`
- Generator unit tests: `OrderGeneratorServiceTest` (includes order-type assertion with `@RepeatedTest(20)`)
