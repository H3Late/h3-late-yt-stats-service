package com.h3late.stats.config;

import com.h3late.stats.dto.TomatoEventDto;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TomatoKafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, TomatoEventDto> tomatoEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${KAFKA_API_KEY}") String apiKey,
            @Value("${KAFKA_API_SECRET}") String apiSecret
    ) {
        Map<String, Object> properties = new HashMap<>();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(
                SaslConfigs.SASL_JAAS_CONFIG,
                String.format(
                        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                        apiKey,
                        apiSecret
                )
        );

        JsonDeserializer<TomatoEventDto> tomatoDeserializer =
                new JsonDeserializer<>(TomatoEventDto.class, false);

        tomatoDeserializer.addTrustedPackages("com.h3late.stats.dto");

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(tomatoDeserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TomatoEventDto>
    tomatoKafkaListenerContainerFactory(
            ConsumerFactory<String, TomatoEventDto> tomatoEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, TomatoEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(tomatoEventConsumerFactory);
        return factory;
    }
}