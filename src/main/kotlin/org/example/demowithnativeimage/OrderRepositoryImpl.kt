package org.example.demowithnativeimage

import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory
import org.springframework.stereotype.Repository
import jakarta.persistence.PersistenceContext
import jakarta.persistence.EntityManager

// Spring Data will pick up this implementation by the naming convention: <RepoInterfaceName>Impl
@Repository
class OrderRepositoryImpl(
    private val blazeJPAQueryFactory: BlazeJPAQueryFactory,
    @PersistenceContext private val entityManager: EntityManager
) : OrderRepositoryCustom {

    override fun findAllWithBlaze(): List<OrderEntity> {
        val qOrder = QOrderEntity.orderEntity
        return blazeJPAQueryFactory.select(qOrder)
            .from(qOrder)
            .fetch()
    }

    override fun findByIdWithBlaze(id: Long): OrderEntity? {
        val qOrder = QOrderEntity.orderEntity
        return blazeJPAQueryFactory.select(qOrder)
            .from(qOrder)
            .where(qOrder.id.eq(id))
            .fetchOne()
    }
}
