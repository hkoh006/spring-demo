package org.example.spring.demo.dao

import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.querydsl.BlazeJPAQuery
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import jakarta.persistence.EntityManager
import org.example.spring.demo.dao.model.OrderEntity
import org.example.spring.demo.dao.model.QOrderEntity
import org.springframework.stereotype.Repository

@Repository
class OrderJdbcRepository(
    private val entityManager: EntityManager,
    private val criteriaBuilderFactory: CriteriaBuilderFactory,
) {
    private val order = QOrderEntity.orderEntity

    fun findAll(): List<OrderEntity> {
        return BlazeJPAQuery<OrderEntity>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .fetch()
    }

    fun findAllWithIdsMatchingAny(allocationIds: List<String>): List<OrderEntity> {
        return BlazeJPAQuery<OrderEntity>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .where(
                if (allocationIds.isNotEmpty()) {
                    Expressions.booleanTemplate(
                        "jsonb_contains_any_of(jsonb_path_query_array({0}, {1}), {2}) = true",
                        order.orderDetails,
                        Expressions.stringTemplate("'$.allocations[*].id'"),
                        Expressions.stringTemplate(
                            "'${
                                allocationIds.joinToString(
                                    prefix = "[",
                                    postfix = "]",
                                    separator = ",",
                                ) { "\"$it\"" }
                            }'",
                        ),
                    )
                } else {
                    BooleanBuilder()
                },
            )
            .fetch()
    }

    fun findAllWithIdsMatchingAll(allocationIds: List<String>): List<OrderEntity> {
        return BlazeJPAQuery<OrderEntity>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .where(
                if (allocationIds.isNotEmpty()) {
                    Expressions.booleanTemplate(
                        "jsonb_contains(jsonb_path_query_array({0}, {1}), {2}) = true",
                        order.orderDetails,
                        Expressions.stringTemplate("'$.allocations[*].id'"),
                        Expressions.stringTemplate(
                            "'${
                                allocationIds.joinToString(
                                    prefix = "[",
                                    postfix = "]",
                                    separator = ",",
                                ) { "\"$it\"" }
                            }'",
                        ),
                    )
                } else {
                    BooleanBuilder()
                },
            )
            .fetch()
    }
}
