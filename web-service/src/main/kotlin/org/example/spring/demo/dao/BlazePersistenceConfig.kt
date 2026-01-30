package org.example.spring.demo.dao

import com.blazebit.persistence.Criteria
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory
import com.blazebit.persistence.spi.JpqlFunctionGroup
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.example.spring.demo.dao.jpql.PgJsonbContainsAnyOfFunction
import org.example.spring.demo.dao.jpql.PgJsonbContainsFunction
import org.example.spring.demo.dao.jpql.PgJsonbPathQueryArrayFunction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BlazePersistenceConfig {
    @Bean
    fun criteriaBuilderFactory(emf: EntityManagerFactory): CriteriaBuilderFactory {
        val default = Criteria.getDefault()
        val jsonbContains = JpqlFunctionGroup("jsonb_contains", PgJsonbContainsFunction())
        val jsonbContainsAnyOf = JpqlFunctionGroup("jsonb_contains_any_of", PgJsonbContainsAnyOfFunction())
        val jsonbPathQueryArray = JpqlFunctionGroup("jsonb_path_query_array", PgJsonbPathQueryArrayFunction())

        default.registerFunction(jsonbContains)
        default.registerFunction(jsonbContainsAnyOf)
        default.registerFunction(jsonbPathQueryArray)

        return default.createCriteriaBuilderFactory(emf)
    }

    @Bean
    fun blazeJPAQueryFactory(
        entityManager: EntityManager,
        cbf: CriteriaBuilderFactory,
    ): BlazeJPAQueryFactory = BlazeJPAQueryFactory(entityManager, cbf)
}
