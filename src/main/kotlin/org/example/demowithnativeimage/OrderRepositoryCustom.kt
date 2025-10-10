package org.example.demowithnativeimage

interface OrderRepositoryCustom {
    fun findAllWithBlaze(): List<OrderEntity>
    fun findByIdWithBlaze(id: Long): OrderEntity?
}
