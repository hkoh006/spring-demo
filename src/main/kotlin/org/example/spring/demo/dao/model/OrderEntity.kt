package org.example.spring.demo.dao.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Table(name = "orders")
@Entity
open class OrderEntity(
    @Id
    open var id: Long? = null,

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    open var orderDetails: OrderDetails = OrderDetails()
)

data class OrderDetails(
    val allocations: List<Allocation> = emptyList(),
    val orderType: OrderType = Market()
)

data class Allocation(
    val id: String = "",
    val quantity: Int = 0
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
sealed interface OrderType

@JsonTypeName("MARKET")
class Market() : OrderType

@JsonTypeName("LIMIT")
data class Limit(
    val price: Int = 0
) : OrderType

@JsonTypeName("STOP_LOSS")
data class StopLoss(
    val stopPrice: Int = 0
) : OrderType

@JsonTypeName("STOP_LIMIT")
data class StopLimit(
    val stopPrice: Int = 0,
    val limitPrice: Int = 0
) : OrderType

@JsonTypeName("MARKET_ON_CLOSE")
class MarketOnClose() : OrderType

@JsonTypeName("LIMIT_ON_CLOSE")
data class LimitOnClose(
    val price: Int = 0
) : OrderType
