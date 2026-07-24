package com.h3late.stats.repository;

import com.h3late.stats.entity.TomatoEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TomatoEventRepository extends JpaRepository<TomatoEvent, String> {
 
    // Custom query to add a new TomatoEvent only if it doesn't already exist based on messageId
    @Modifying
    @Query(value = """
        INSERT INTO tomato_event (
            event_id,
            video_id,
            live_chat_id,
            message_id,
            user_id,
            user_display_name,
            published_at,
            detected_at,
            tomato_count,
            created_at
        )
        VALUES (
            :eventId,
            :videoId,
            :liveChatId,
            :messageId,
            :userId,
            :userDisplayName,
            :publishedAt,
            :detectedAt,
            :tomatoCount,
            NOW()
        )
        ON CONFLICT (message_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("videoId") String videoId,
            @Param("liveChatId") String liveChatId,
            @Param("messageId") String messageId,
            @Param("userId") String userId,
            @Param("userDisplayName") String userDisplayName,
            @Param("publishedAt") Instant publishedAt,
            @Param("detectedAt") Instant detectedAt,
            @Param("tomatoCount") int tomatoCount
    );
}