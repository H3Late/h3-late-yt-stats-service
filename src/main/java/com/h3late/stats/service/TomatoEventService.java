package com.h3late.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.h3late.stats.dto.TomatoEventDto;
import com.h3late.stats.entity.TomatoEvent;
import com.h3late.stats.repository.TomatoEventRepository; 
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional; 

@Service
@RequiredArgsConstructor
@Slf4j
public class TomatoEventService {
    private final TomatoEventRepository tomatoEventRepository;

    // Receieve a TomatoEvent and save it to the database
    public TomatoEvent saveTomatoEvent(TomatoEvent tomatoEvent) {
        int inserted = tomatoEventRepository.insertIfAbsent(
                tomatoEvent.getEventId(),
                tomatoEvent.getVideoId(),
                tomatoEvent.getLiveChatId(),
                tomatoEvent.getMessageId(),
                tomatoEvent.getUserId(),
                tomatoEvent.getUserDisplayName(),
                tomatoEvent.getPublishedAt(),
                tomatoEvent.getDetectedAt(),
                tomatoEvent.getTomatoCount()
        );

        if (inserted == 0) {
            log.info(
                    "Tomato event already exists; skipping messageId={}",
                    tomatoEvent.getMessageId()
            );
            return tomatoEvent;
        }

        log.info("Saved tomato event messageId={}", tomatoEvent.getMessageId());
        return tomatoEvent;
    }   
 
    @Transactional
    public void processTomatoEvent(TomatoEventDto tomatoEvent) {
        validateTomatoEvent(tomatoEvent);
        
       // save the TomatoEvent to the database
        saveTomatoEvent(mapToEntity(tomatoEvent));

        log.info(
                "Stored tomato event videoId={} messageId={} count={}",
                tomatoEvent.getVideoId(),
                tomatoEvent.getMessageId(),
                tomatoEvent.getTomatoCount()
        );
    }

    // Private mapper from DTO to TomatoEvent entity
    private TomatoEvent mapToEntity(TomatoEventDto dto) {
        TomatoEvent event = TomatoEvent.builder()
                .eventId(dto.getEventId())
                .videoId(dto.getVideoId())
                .liveChatId(dto.getLiveChatId())
                .messageId(dto.getMessageId())
                .userId(dto.getUserId())
                .userDisplayName(dto.getUserDisplayName())
                .publishedAt(dto.getPublishedAt().toInstant())
                .detectedAt(dto.getDetectedAt().toInstant())
                .tomatoCount(dto.getTomatoCount())
                .build();

        return event;
    }

    private static void validateTomatoEvent(TomatoEventDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Tomato event cannot be null");
        }

        requireNonBlank(dto.getEventId(), "eventId");
        requireNonBlank(dto.getVideoId(), "videoId");
        requireNonBlank(dto.getLiveChatId(), "liveChatId");
        requireNonBlank(dto.getMessageId(), "messageId");

        if (dto.getPublishedAt() == null) {
            throw new IllegalArgumentException("publishedAt cannot be null");
        }

        if (dto.getDetectedAt() == null) {
            throw new IllegalArgumentException("detectedAt cannot be null");
        }

        if (dto.getTomatoCount() <= 0) {
            throw new IllegalArgumentException("tomatoCount must be greater than zero");
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
}

