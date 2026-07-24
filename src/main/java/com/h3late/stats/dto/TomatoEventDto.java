package com.h3late.stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TomatoEventDto {
    private String eventId;
    private String videoId;
    private String liveChatId;
    private String messageId;
    private String userId;
    private String userDisplayName;
    private OffsetDateTime publishedAt;
    private OffsetDateTime detectedAt;
    private int tomatoCount;
    private long runningTotal;
}