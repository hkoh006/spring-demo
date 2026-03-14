### Requirement: Market analytics endpoint
The system SHALL expose a `GET /api/market/analytics` endpoint that returns aggregated market data computed from the last 24 hours of trade history and the current live order book state.

#### Scenario: Analytics returned when trades exist
- **WHEN** at least one trade has occurred within the last 24 hours
- **THEN** the response SHALL include `high` (maximum trade price), `low` (minimum trade price), `lastPrice` (most recent trade price overall), and `volume` (total quantity traded in the last 24 hours)

#### Scenario: Analytics returned when order book has open orders
- **WHEN** the order book contains at least one bid and one ask
- **THEN** the response SHALL include `bestBid` (highest bid price) and `bestAsk` (lowest ask price)

#### Scenario: Analytics returned when no trades have occurred
- **WHEN** no trades exist in the last 24 hours
- **THEN** `high`, `low`, and `lastPrice` SHALL be `null`, `volume` SHALL be `0`, and `bestBid`/`bestAsk` SHALL reflect live order book state

#### Scenario: Analytics returned when order book is empty
- **WHEN** the order book has no open bids or asks
- **THEN** `bestBid` and `bestAsk` SHALL be `null`

### Requirement: 24-hour rolling time window
The system SHALL compute `high`, `low`, and `volume` over a rolling 24-hour window ending at the time of the request. `lastPrice` SHALL always reflect the most recently executed trade regardless of the time window.

#### Scenario: Trades outside the 24-hour window are excluded
- **WHEN** a trade occurred more than 24 hours before the request
- **THEN** that trade's price and quantity SHALL NOT be included in `high`, `low`, or `volume`

#### Scenario: Trades within the 24-hour window are included
- **WHEN** a trade occurred within the last 24 hours
- **THEN** that trade's price SHALL be considered for `high` and `low`, and its quantity SHALL be added to `volume`

### Requirement: Response schema
The system SHALL return a JSON object conforming to the `MarketAnalyticsDto` schema with the following fields: `high` (nullable number), `low` (nullable number), `lastPrice` (nullable number), `volume` (number, non-null, minimum 0), `bestBid` (nullable number), `bestAsk` (nullable number).

#### Scenario: Response is always HTTP 200
- **WHEN** a client sends `GET /api/market/analytics`
- **THEN** the server SHALL respond with HTTP `200 OK` and a valid `MarketAnalyticsDto` body under all market conditions
