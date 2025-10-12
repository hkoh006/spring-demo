package org.example.spring.demo.dao

import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.querydsl.BlazeJPAQuery
import jakarta.persistence.EntityManager
import org.example.spring.demo.dao.model.OrderEntity
import org.example.spring.demo.dao.model.QOrderEntity
import org.springframework.stereotype.Repository

@Repository
class OrderJdbcRepository(
    private val entityManager: EntityManager,
    private val criteriaBuilderFactory: CriteriaBuilderFactory
) {

    private val order = QOrderEntity.orderEntity

    fun findAll(): List<OrderEntity> {
        return BlazeJPAQuery<List<OrderEntity>>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .fetch()
    }

}