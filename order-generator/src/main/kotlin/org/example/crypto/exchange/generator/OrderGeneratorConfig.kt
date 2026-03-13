package org.example.crypto.exchange.generator

import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.example.crypto.exchange.messaging.proto.OrderEventProto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OrderGeneratorProperties::class)
class OrderGeneratorConfig {
    @Bean("orderProducerFactory")
    fun orderProducerFactory(
        connectionDetails: KafkaConnectionDetails,
        @Value("\${spring.kafka.properties.schema.registry.url:mock://crypto-exchange-local}") schemaRegistryUrl: String,
    ): ProducerFactory<String, OrderEventProto> =
        DefaultKafkaProducerFactory(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to connectionDetails.bootstrapServers.joinToString(","),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaProtobufSerializer::class.java,
                "schema.registry.url" to schemaRegistryUrl,
                "auto.register.schemas" to "true",
            ),
        )

    @Bean("orderKafkaTemplate")
    fun orderKafkaTemplate(
        @Qualifier("orderProducerFactory") factory: ProducerFactory<String, OrderEventProto>,
    ): KafkaTemplate<String, OrderEventProto> = KafkaTemplate(factory)
}
