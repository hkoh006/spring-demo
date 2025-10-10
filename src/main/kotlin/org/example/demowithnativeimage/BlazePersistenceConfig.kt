package org.example.demowithnativeimage

import com.blazebit.persistence.Criteria
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BlazePersistenceConfig {

    @Bean
    fun criteriaBuilderFactory(emf: EntityManagerFactory): CriteriaBuilderFactory {
        return Criteria.getDefault().createCriteriaBuilderFactory(emf)
    }

    @Bean
    fun blazeJPAQueryFactory(entityManager: EntityManager, cbf: CriteriaBuilderFactory): BlazeJPAQueryFactory {
        return BlazeJPAQueryFactory(entityManager, cbf)
    }
}
