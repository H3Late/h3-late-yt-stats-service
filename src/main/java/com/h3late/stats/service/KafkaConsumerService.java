package com.h3late.stats.service;

import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.dto.TomatoEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TomatoEventService tomatoService;
    private final LivestreamService livestreamService;

    @KafkaListener(topics = "${kafka.video-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenToVideoEvents(
            @Header(KafkaHeaders.RECEIVED_KEY)
            String videoId,
            @Payload(required = false)
            VideoEventDto messageBody
    ) {
        log.info("Received kafka message with key '{}' and body=[{}]", videoId, messageBody);

        livestreamService.processLivestreamEvent(videoId, messageBody);
    }

    // Listener for tomato events
    @KafkaListener(topics = "${kafka.tomato-chat-topic}", 
                    groupId = "${spring.kafka.consumer.group-id}", 
                    containerFactory = "tomatoKafkaListenerContainerFactory")
    public void listenToTomatoEvents(
            @Header(KafkaHeaders.RECEIVED_KEY)
            String videoId,
            @Payload(required =  true)
            TomatoEventDto messageBody
    ) {
        log.info("Received tomato kafka message with key '{}' and body=[{}]", videoId, messageBody);

        tomatoService.processTomatoEvent(messageBody);
    }
}
