package org.example.crypto.exchange

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, String>

@Repository
interface TradeRepository : JpaRepository<Trade, String>
