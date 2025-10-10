package org.example.demowithnativeimage

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
    open var orderDetails: OrderDetails
)

data class OrderDetails(

    val allocations: List<Allocation>
)

data class Allocation(
    val id: String,
    val quantity: Int
)