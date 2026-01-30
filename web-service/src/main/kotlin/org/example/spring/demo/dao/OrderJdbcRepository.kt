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

    private companion object {
        private const val JSON_PATH_ALLOC_IDS = "'$.allocations[*].id'"
    }

    fun findAll(): List<OrderEntity> =
        BlazeJPAQuery<OrderEntity>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .fetch()

    fun findAllWithIdsMatchingAny(allocationIds: List<String>): List<OrderEntity> =
        BlazeJPAQuery<OrderEntity>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .where(
                if (allocationIds.isNotEmpty()) {
                    Expressions.booleanTemplate(
                        "jsonb_contains_any_of(jsonb_path_query_array({0}, {1}), {2}) = true",
                        order.orderDetails,
                        Expressions.stringTemplate(JSON_PATH_ALLOC_IDS),
                        Expressions.stringTemplate(
                            jsonTextArrayLiteral(allocationIds),
                        ),
                    )
                } else {
                    BooleanBuilder()
                },
            ).fetch()

    fun findAllWithIdsMatchingAll(allocationIds: List<String>): List<OrderEntity> =
        BlazeJPAQuery<OrderEntity>(entityManager, criteriaBuilderFactory)
            .select(order)
            .from(order)
            .where(
                if (allocationIds.isNotEmpty()) {
                    Expressions.booleanTemplate(
                        "jsonb_contains(jsonb_path_query_array({0}, {1}), {2}) = true",
                        order.orderDetails,
                        Expressions.stringTemplate(JSON_PATH_ALLOC_IDS),
                        Expressions.stringTemplate(
                            jsonTextArrayLiteral(allocationIds),
                        ),
                    )
                } else {
                    BooleanBuilder()
                },
            ).fetch()

    private fun jsonTextArrayLiteral(values: List<String>): String {
        // Produces a SQL string literal representing a JSON text array, e.g. '[''a'',''b'']' wrapped in single quotes
        if (values.isEmpty()) return "'[]'"
        val joined = values.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
        return "'$joined'"
    }
}
