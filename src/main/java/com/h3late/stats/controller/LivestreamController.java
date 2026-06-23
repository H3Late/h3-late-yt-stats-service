package com.h3late.stats.controller;

import com.h3late.stats.dto.StatsSummaryProjection;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.service.LivestreamService;
import com.h3late.stats.service.YoutubeApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/livestream")
@RequiredArgsConstructor
class LivestreamController {

    private final LivestreamService livestreamService;
    private final YoutubeApiService youtubeApiService;

    // Read-only YouTube API lookup — no DB writes
    @GetMapping("/inspect/{videoId}")
    public ResponseEntity<YoutubeApiResponseDto.VideoItem> inspectVideo(@PathVariable String videoId) {
        return youtubeApiService.getVideoDetails(videoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 1. Single Video Lookup
    @GetMapping("/{id}")
    public ResponseEntity<Livestream> getLivestreamById(@PathVariable String id) {
        return livestreamService.getLivestreamById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. The Global Search (Filters + Pagination + Sorting)
    @GetMapping
    public ResponseEntity<Page<Livestream>> searchLivestreams(
            @RequestParam(required = false) StreamStatus status,
            @RequestParam(required = false) TimeStatus timeStatus,
            @RequestParam(required = false) String search, // New parameter
            @PageableDefault(size = 20, sort = "scheduledStart", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(livestreamService.searchLivestreams(status, timeStatus, search, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsSummaryProjection> getGlobalStats() {
        return ResponseEntity.ok(livestreamService.getGlobalStats());
    }

    @GetMapping("/stats/day")
    public ResponseEntity<?> getStatsByDay() {
        return ResponseEntity.ok(livestreamService.getStatsByDay());
    }

    // Process video(s) by videoId(s) - accepts comma-separated list - fetches all details from YouTube API
    @PostMapping("/process/{videoIds}")
    public ResponseEntity<Map<String, Object>> processVideos(
            @PathVariable String videoIds,
            @RequestParam(defaultValue = "false") boolean bypassCheck) {
        try {
            // Parse comma-separated video IDs
            String[] videoIdArray = videoIds.split(",");
            List<String> videoIdList = Arrays.stream(videoIdArray)
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
            
            if (videoIdList.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No valid video IDs provided");
                errorResponse.put("totalProcessed", 0);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, String> results = livestreamService.processVideosByIds(videoIdList, bypassCheck);
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalRequested", videoIdList.size());
            response.put("bypassCheck", bypassCheck);
            response.put("results", results);
            
            long successCount = results.values().stream().mapToLong(status -> "SUCCESS".equals(status) ? 1 : 0).sum();
            response.put("successCount", successCount);
            response.put("failureCount", videoIdList.size() - successCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing videos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}
