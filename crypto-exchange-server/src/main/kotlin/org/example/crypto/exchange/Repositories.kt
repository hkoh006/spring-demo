package org.example.crypto.exchange

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<OrderEntity, String>

@Repository
interface TradeRepository : JpaRepository<TradeEntity, String>
