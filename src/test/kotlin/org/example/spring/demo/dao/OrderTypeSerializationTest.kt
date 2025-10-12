package org.example.spring.demo.dao

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.example.spring.demo.dao.model.*
import org.junit.jupiter.api.Test

class OrderTypeSerializationTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `serialize OrderDetails with Market orderType contains type discriminator and deserializes back to Market`() {
        val details = OrderDetails(orderType = Market())

        val json = mapper.writeValueAsString(details)

        // JSON should contain the type discriminator
        assertThat(json).contains("\"type\":\"MARKET\"")

        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(Market::class.java)
    }

    @Test
    fun `serialize OrderDetails with Limit orderType contains type and price and deserializes back to Limit`() {
        val details = OrderDetails(orderType = Limit(price = 123))

        val json = mapper.writeValueAsString(details)

        assertThat(json).contains("\"type\":\"LIMIT\"")
        assertThat(json).contains("\"price\":123")

        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(Limit::class.java)
        val limit = decoded.orderType as Limit
        assertThat(limit.price).isEqualTo(123)
    }

    @Test
    fun `deserialize raw JSON with MARKET to OrderDetails`() {
        val json = """
            {"orderType": {"type": "MARKET"}}
        """.trimIndent()

        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(Market::class.java)
    }

    @Test
    fun `deserialize raw JSON with LIMIT to OrderDetails`() {
        val json = """
            {"orderType": {"type": "LIMIT", "price": 456}}
        """.trimIndent()

        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(Limit::class.java)
        val limit = decoded.orderType as Limit
        assertThat(limit.price).isEqualTo(456)
    }

    @Test
    fun `serialize and deserialize StopLoss`() {
        val details = OrderDetails(orderType = StopLoss(stopPrice = 250))
        val json = mapper.writeValueAsString(details)
        assertThat(json).contains("\"type\":\"STOP_LOSS\"")
        assertThat(json).contains("\"stopPrice\":250")
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(StopLoss::class.java)
        val sl = decoded.orderType as StopLoss
        assertThat(sl.stopPrice).isEqualTo(250)
    }

    @Test
    fun `serialize and deserialize StopLimit`() {
        val details = OrderDetails(orderType = StopLimit(stopPrice = 260, limitPrice = 255))
        val json = mapper.writeValueAsString(details)
        assertThat(json).contains("\"type\":\"STOP_LIMIT\"")
        assertThat(json).contains("\"stopPrice\":260")
        assertThat(json).contains("\"limitPrice\":255")
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(StopLimit::class.java)
        val sl = decoded.orderType as StopLimit
        assertThat(sl.stopPrice).isEqualTo(260)
        assertThat(sl.limitPrice).isEqualTo(255)
    }

    @Test
    fun `serialize and deserialize MarketOnClose`() {
        val details = OrderDetails(orderType = MarketOnClose())
        val json = mapper.writeValueAsString(details)
        assertThat(json).contains("\"type\":\"MARKET_ON_CLOSE\"")
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(MarketOnClose::class.java)
    }

    @Test
    fun `serialize and deserialize LimitOnClose`() {
        val details = OrderDetails(orderType = LimitOnClose(price = 200))
        val json = mapper.writeValueAsString(details)
        assertThat(json).contains("\"type\":\"LIMIT_ON_CLOSE\"")
        assertThat(json).contains("\"price\":200")
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(LimitOnClose::class.java)
        val loc = decoded.orderType as LimitOnClose
        assertThat(loc.price).isEqualTo(200)
    }

    @Test
    fun `deserialize raw JSON with STOP_LOSS to OrderDetails`() {
        val json = """
            {"orderType": {"type": "STOP_LOSS", "stopPrice": 300}}
        """.trimIndent()
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(StopLoss::class.java)
        val sl = decoded.orderType as StopLoss
        assertThat(sl.stopPrice).isEqualTo(300)
    }

    @Test
    fun `deserialize raw JSON with STOP_LIMIT to OrderDetails`() {
        val json = """
            {"orderType": {"type": "STOP_LIMIT", "stopPrice": 310, "limitPrice": 305}}
        """.trimIndent()
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(StopLimit::class.java)
        val sl = decoded.orderType as StopLimit
        assertThat(sl.stopPrice).isEqualTo(310)
        assertThat(sl.limitPrice).isEqualTo(305)
    }

    @Test
    fun `deserialize raw JSON with MARKET_ON_CLOSE to OrderDetails`() {
        val json = """
            {"orderType": {"type": "MARKET_ON_CLOSE"}}
        """.trimIndent()
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(MarketOnClose::class.java)
    }

    @Test
    fun `deserialize raw JSON with LIMIT_ON_CLOSE to OrderDetails`() {
        val json = """
            {"orderType": {"type": "LIMIT_ON_CLOSE", "price": 220}}
        """.trimIndent()
        val decoded: OrderDetails = mapper.readValue(json)
        assertThat(decoded.orderType).isInstanceOf(LimitOnClose::class.java)
        val loc = decoded.orderType as LimitOnClose
        assertThat(loc.price).isEqualTo(220)
    }
}
