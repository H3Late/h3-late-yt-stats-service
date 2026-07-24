package com.h3late.stats.service;

import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.repository.LivestreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivestreamSyncService {

    private final LivestreamRepository livestreamRepository;
    private final LivestreamService livestreamService;

    // Runs 5 minutes after startup, then every 5 hours after the previous run completes.
    // Reprocesses any streams stuck in LIVE status to pick up missed ENDED transitions.
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = 5 * 60 * 60 * 1000)
    public void syncLiveStreams() {
        List<Livestream> liveStreams = livestreamRepository.findAllByStatus(StreamStatus.LIVE);

        if (liveStreams.isEmpty()) {
            log.info("Live stream sync: no streams in LIVE status, nothing to do.");
            return;
        }

        log.info("Live stream sync: found {} stream(s) in LIVE status, reprocessing.", liveStreams.size());

        for (Livestream stream : liveStreams) {
            log.info("Live stream sync: reprocessing videoId=[{}] title='{}'", stream.getVideoId(), stream.getTitle());
            try {
                livestreamService.processVideoById(stream.getVideoId(), false);
            } catch (Exception e) {
                log.error("Live stream sync: failed to reprocess videoId=[{}]: {}", stream.getVideoId(), e.getMessage());
            }
        }

        log.info("Live stream sync: completed reprocessing {} stream(s).", liveStreams.size());
    }
}
