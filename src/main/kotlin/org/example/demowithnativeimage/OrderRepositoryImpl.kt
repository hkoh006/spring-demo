package org.example.demowithnativeimage

import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.querydsl.BlazeJPAQuery
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository


@Repository
class OrderJdbcRepository(
    private val entityManager: EntityManager,
    private val criteriaBuilderFactory: CriteriaBuilderFactory
) {

    private val order = QOrderEntity.orderEntity

    fun findAll(): List<OrderEntity> {
        return BlazeJPAQuery<List<OrderEntity>>(entityManager, criteriaBuilderFactory)
            .from(order)
            .select(order)
            .fetch()
    }

}
