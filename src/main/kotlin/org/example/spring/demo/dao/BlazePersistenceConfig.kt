package org.example.spring.demo.dao

import com.blazebit.persistence.Criteria
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory
import com.blazebit.persistence.spi.JpqlFunctionGroup
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class BlazePersistenceConfig {

    @Bean
    fun criteriaBuilderFactory(emf: EntityManagerFactory): CriteriaBuilderFactory {
        val default = Criteria.getDefault()
        val jsonbContains = JpqlFunctionGroup("jsonb_contains", PgJsonbContainsFunction())
        default.registerFunction(jsonbContains)
        return default.createCriteriaBuilderFactory(emf)
    }

    @Bean
    fun blazeJPAQueryFactory(entityManager: EntityManager, cbf: CriteriaBuilderFactory): BlazeJPAQueryFactory {
        return BlazeJPAQueryFactory(entityManager, cbf)
    }
}
