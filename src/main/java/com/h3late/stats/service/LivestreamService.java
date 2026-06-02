package com.h3late.stats.service;

import com.h3late.stats.dto.StatsByDayProjection;
import com.h3late.stats.dto.StatsSummaryProjection;
import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.repository.LivestreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivestreamService {
    private final LivestreamRepository livestreamRepository;
    private final YoutubeApiService youtubeApiService;

    // Inject voteService to allow for attributing pending votes to a stream once it starts, 
    // which can be used to count those votes in the final leaderboard
    private final VoteService voteService; 

    public Page<Livestream> searchLivestreams(StreamStatus status, TimeStatus timeStatus, String search, Pageable pageable) {
        return livestreamRepository.searchAdvanced(status, timeStatus, search, pageable);
    }

    public Optional<Livestream> getLivestreamById(String id) {
        return livestreamRepository.findById(id);
    }

    public void processLivestreamEvent(String videoId, VideoEventDto messageBody) {
        if (messageBody == null) {
            processCancelledLivestream(videoId);
        } else {
            youtubeApiService.getVideoDetails(videoId)
                    .ifPresent((videoItem) -> {

                        log.info("Fetched livestream details for '{}' with videoId=[{}]", videoItem.getSnippet().getTitle(), videoId);
                        Optional<Livestream> existingVideo = livestreamRepository.findById(videoId);

                        // Skip if not a livestream... scheduled VODS will have processed but will not be in our DB
                        // This allows us to capture updates to actual livestreams that may come
                        // after the livestream has ended... as these will have already been captured in our DB, and we will update them accordingly
                        if (videoItem.getStatus().getUploadStatus().equals("processed") && existingVideo.isEmpty()) {
                            log.info("Livestream '{}' with videoId=[{}] has status 'processed'. This is likely not a livestream", videoItem.getSnippet().getTitle(), videoId);
                            return;
                        }

                        if (existingVideo.isPresent()) {
                            // Update if livestream already exists
                            updateExistingLivestream(
                                    videoId,
                                    messageBody.getTitle(),
                                    videoItem,
                                    existingVideo.get()
                            );
                        } else {
                            // Create new livestream if it doesn't exist
                            createNewLivestream(videoId, videoItem);
                        }
                    });
        }
    }

    private void updateExistingLivestream(
            String videoId,
            String title,
            YoutubeApiResponseDto.VideoItem videoItem,
            Livestream existingVideo
    ) {
        log.info("Livestream with videoId=[{}] already exists in the database.", videoId);
        boolean hasUpdates = false;

        if (!title.equals(existingVideo.getTitle())) {
            log.info("New title '{}' is different from existing title '{}' for videoId=[{}]", title, existingVideo.getTitle(), videoId);
            existingVideo.setTitle(title);
            hasUpdates = true;
        }

        YoutubeApiResponseDto.LiveStreamingDetails liveDetails = videoItem.getLiveStreamingDetails();

        if (liveDetails == null) {
            log.warn("Live streaming details are missing for videoId=[{}]. Skipping livestream processing.", videoId);
        }

        if (
                existingVideo.getActualStart() == null
                        && liveDetails != null
                        && liveDetails.getActualStartTime() != null
        ) {
            existingVideo.setStatus((StreamStatus.LIVE));

            Instant actualStart = Instant.parse(liveDetails.getActualStartTime());
            existingVideo.setActualStart(actualStart);

            long lateTimeSeconds = java.time.Duration.between(
                    existingVideo.getScheduledStart(),
                    actualStart
            ).getSeconds();

            if (lateTimeSeconds > 10) {
                existingVideo.setTimeStatus(TimeStatus.LATE);
            } else if (lateTimeSeconds < 0) {
                existingVideo.setTimeStatus(TimeStatus.EARLY);
            } else {
                existingVideo.setTimeStatus(TimeStatus.ON_TIME);
            }
            existingVideo.setDiffSeconds(lateTimeSeconds);
            log.info("Livestream with videoId=[{}] has started. Actual start time: {}, Scheduled start time: {}, Late time (seconds): {}, Time status: {}",
                    videoId, actualStart, existingVideo.getScheduledStart(), lateTimeSeconds, existingVideo.getTimeStatus()
            );

            // Attribute any pending votes to the stream now that it has started, allowing them to be counted in the final leaderboard
            voteService.attributePendingVotes(videoId);
            hasUpdates = true;
        }

        if (
                existingVideo.getActualEnd() == null
                        && liveDetails != null
                        && liveDetails.getActualEndTime() != null
        ) {
            existingVideo.setStatus(StreamStatus.ENDED);
            Instant actualEnd = Instant.parse(liveDetails.getActualEndTime());
            long totalDurationSeconds = java.time.Duration.between(
                    existingVideo.getActualStart(),
                    actualEnd
            ).getSeconds();
            existingVideo.setActualEnd(actualEnd);
            existingVideo.setTotalDurationSeconds(totalDurationSeconds);
            log.info("Livestream with videoId=[{}] has ended with a total duration of {}. Actual end time: {}", videoId, totalDurationSeconds, actualEnd);
            hasUpdates = true;
        }


        if (!hasUpdates) {
            log.info("No relevant updates for '{}' with videoId=[{}] Skipping processing", title, videoId);
        } else {
            livestreamRepository.save(existingVideo);
            log.info("Updated livestream with videoId=[{}]", videoId);
        }
    }

    private void createNewLivestream(String videoId, YoutubeApiResponseDto.VideoItem videoItem) {
        Livestream newLivestream = new Livestream();
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails = videoItem.getLiveStreamingDetails();
        if (liveDetails != null && liveDetails.getScheduledStartTime() != null) {
            newLivestream.setScheduledStart(Instant.parse(liveDetails.getScheduledStartTime()));
        }
        newLivestream.setVideoId(videoId);
        newLivestream.setTitle(videoItem.getSnippet().getTitle());
        newLivestream.setStatus(StreamStatus.SCHEDULED);

        if (liveDetails != null && liveDetails.getActualStartTime() != null) {
            updateExistingLivestream(
                    videoId,
                    videoItem.getSnippet().getTitle(),
                    videoItem,
                    newLivestream
            );
        }
        livestreamRepository.save(newLivestream);
    }


    private void processCancelledLivestream(String videoId) {
        log.info("Received null message body for videoId=[{}]. Marking livestream as cancelled if it exists.", videoId);
        Optional<Livestream> existingVideo = livestreamRepository.findById(videoId);

        if (existingVideo.isPresent()) {

            Livestream livestream = existingVideo.get();
            livestream.setStatus(StreamStatus.CANCELLED);
            livestream.setTimeStatus(TimeStatus.CANCELLED);
            livestreamRepository.save(livestream); 
            

            log.info("Marked livestream with videoId=[{}] as cancelled.", videoId);
        } else {
            log.info("No existing livestream found for videoId=[{}] to mark as cancelled.", videoId);
        }
    }

    public StatsSummaryProjection getGlobalStats() {
        return livestreamRepository.getGlobalStats();
    }

    public List<StatsByDayProjection> getStatsByDay() {
        return livestreamRepository.getStatsByDay();
    }

    /**
     * Processes a video by videoId, fetching all details from YouTube API
     * Similar to processLivestreamEvent but without relying on Kafka message body
     */
    public void processVideoById(String videoId, boolean bypassCheck) {
        log.info("Processing video by ID: {} (bypassCheck: {})", videoId, bypassCheck);
        
        youtubeApiService.getVideoDetails(videoId)
                .ifPresentOrElse(
                    (videoItem) -> {
                        log.info("Fetched livestream details for '{}' with videoId=[{}]", videoItem.getSnippet().getTitle(), videoId);
                        Optional<Livestream> existingVideo = livestreamRepository.findById(videoId);

                        // Skip if not a livestream... scheduled VODS will have processed but will not be in our DB
                        // This allows us to capture updates to actual livestreams that may come
                        // after the livestream has ended... as these will have already been captured in our DB, and we will update them accordingly
                        if (!bypassCheck && videoItem.getStatus().getUploadStatus().equals("processed") && existingVideo.isEmpty()) {
                            log.info("Livestream '{}' with videoId=[{}] has status 'processed'. This is likely not a livestream", videoItem.getSnippet().getTitle(), videoId);
                            return;
                        }

                        if (existingVideo.isPresent()) {
                            // Update if livestream already exists - use title from YouTube API
                            updateExistingLivestream(
                                    videoId,
                                    videoItem.getSnippet().getTitle(),
                                    videoItem,
                                    existingVideo.get()
                            );
                        } else {
                            // Create new livestream if it doesn't exist
                            createNewLivestream(videoId, videoItem);
                        }
                    },
                    () -> log.warn("Could not fetch video details for videoId=[{}] from YouTube API", videoId)
                );
    }

    /**
     * Processes multiple videos by their videoIds, fetching all details from YouTube API
     * Returns a map with video ID as key and processing result as value
     */
    public Map<String, String> processVideosByIds(List<String> videoIds, boolean bypassCheck) {
        Map<String, String> results = new HashMap<>();
        
        for (String videoId : videoIds) {
            try {
                log.info("Processing video ID: {} (bypassCheck: {})", videoId, bypassCheck);
                processVideoById(videoId, bypassCheck);
                results.put(videoId, "SUCCESS");
            } catch (Exception e) {
                log.error("Failed to process video ID {}: {}", videoId, e.getMessage(), e);
                results.put(videoId, "ERROR: " + e.getMessage());
            }
        }
        
        log.info("Batch processing completed. Processed {} videos (bypassCheck: {})", videoIds.size(), bypassCheck);
        return results;
    }

}