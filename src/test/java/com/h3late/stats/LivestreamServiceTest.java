package com.h3late.stats;

import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.repository.LivestreamRepository;
import com.h3late.stats.service.LivestreamService;
import com.h3late.stats.service.YoutubeApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LivestreamServiceTest {

    private YoutubeApiService youtubeApiService;
    private LivestreamRepository livestreamRepository;
    private LivestreamService livestreamService;

    @BeforeEach
    public void setUp() {
        youtubeApiService = Mockito.mock(YoutubeApiService.class);
        livestreamRepository = Mockito.mock(LivestreamRepository.class);
        livestreamService = new LivestreamService(livestreamRepository, youtubeApiService);
    }

    // ==================== HELPER METHODS ====================

    private YoutubeApiResponseDto.VideoItem createVideoItem(
            String title,
            String uploadStatus,
            String scheduledStartTime,
            String actualStartTime,
            String actualEndTime
    ) {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet(title, "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status(uploadStatus);
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails =
                new YoutubeApiResponseDto.LiveStreamingDetails(actualStartTime, scheduledStartTime, actualEndTime);
        return new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);
    }

    private Livestream createExistingLivestream(
            String videoId,
            String title,
            Instant scheduledStart,
            Instant actualStart,
            Instant actualEnd,
            StreamStatus status,
            TimeStatus timeStatus,
            Long diffSeconds,
            Long totalDurationSeconds
    ) {
        Livestream livestream = new Livestream();
        livestream.setVideoId(videoId);
        livestream.setTitle(title);
        livestream.setScheduledStart(scheduledStart);
        livestream.setActualStart(actualStart);
        livestream.setActualEnd(actualEnd);
        livestream.setStatus(status);
        livestream.setTimeStatus(timeStatus);
        livestream.setDiffSeconds(diffSeconds);
        livestream.setTotalDurationSeconds(totalDurationSeconds);
        return livestream;
    }

    // ==================== getLivestreamById TESTS ====================

    @Test
    public void getLivestreamById_whenExists_thenReturnLivestream() {
        // Arrange
        String videoId = "get-by-id-001";
        Livestream livestream = createExistingLivestream(
                videoId, "Test Stream", Instant.now(), null, null,
                StreamStatus.SCHEDULED, null, null, null
        );
        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(livestream));

        // Act
        Optional<Livestream> result = livestreamService.getLivestreamById(videoId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(videoId, result.get().getVideoId());
        verify(livestreamRepository, times(1)).findById(videoId);
    }

    @Test
    public void getLivestreamById_whenNotExists_thenReturnEmpty() {
        // Arrange
        String videoId = "nonexistent-id";
        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());

        // Act
        Optional<Livestream> result = livestreamService.getLivestreamById(videoId);

        // Assert
        assertFalse(result.isPresent());
        verify(livestreamRepository, times(1)).findById(videoId);
    }

    // ==================== searchLivestreams TESTS ====================

    @Test
    @SuppressWarnings("unchecked")
    public void searchLivestreams_withNoFilters_thenReturnAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Livestream> livestreams = List.of(
                createExistingLivestream("id1", "Stream 1", Instant.now(), null, null, StreamStatus.SCHEDULED, null, null, null),
                createExistingLivestream("id2", "Stream 2", Instant.now(), null, null, StreamStatus.LIVE, TimeStatus.ON_TIME, 5L, null)
        );
        Page<Livestream> page = new PageImpl<>(livestreams, pageable, livestreams.size());
        when(livestreamRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<Livestream> result = livestreamService.searchLivestreams(null, null, null, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(livestreamRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchLivestreams_withStatusFilter_thenReturnFiltered() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Livestream> livestreams = List.of(
                createExistingLivestream("id1", "Live Stream", Instant.now(), Instant.now(), null, StreamStatus.LIVE, TimeStatus.ON_TIME, 5L, null)
        );
        Page<Livestream> page = new PageImpl<>(livestreams, pageable, livestreams.size());
        when(livestreamRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<Livestream> result = livestreamService.searchLivestreams(StreamStatus.LIVE, null, null, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(StreamStatus.LIVE, result.getContent().getFirst().getStatus());
        verify(livestreamRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchLivestreams_withTimeStatusFilter_thenReturnFiltered() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Livestream> livestreams = List.of(
                createExistingLivestream("id1", "Late Stream", Instant.now(), Instant.now(), null, StreamStatus.LIVE, TimeStatus.LATE, 30L, null)
        );
        Page<Livestream> page = new PageImpl<>(livestreams, pageable, livestreams.size());
        when(livestreamRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<Livestream> result = livestreamService.searchLivestreams(null, TimeStatus.LATE, null, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(TimeStatus.LATE, result.getContent().getFirst().getTimeStatus());
        verify(livestreamRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchLivestreams_withBothFilters_thenReturnFiltered() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Livestream> livestreams = List.of(
                createExistingLivestream("id1", "Ended On Time", Instant.now(), Instant.now(), Instant.now(), StreamStatus.ENDED, TimeStatus.ON_TIME, 5L, 3600L)
        );
        Page<Livestream> page = new PageImpl<>(livestreams, pageable, livestreams.size());
        when(livestreamRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<Livestream> result = livestreamService.searchLivestreams(StreamStatus.ENDED, TimeStatus.ON_TIME, null, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(StreamStatus.ENDED, result.getContent().getFirst().getStatus());
        assertEquals(TimeStatus.ON_TIME, result.getContent().getFirst().getTimeStatus());
        verify(livestreamRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchLivestreams_withEmptyResult_thenReturnEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Livestream> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(livestreamRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        // Act
        Page<Livestream> result = livestreamService.searchLivestreams(StreamStatus.CANCELLED, null, null, pageable);

        // Assert
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(livestreamRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    // ==================== processLivestreamEvent - CREATE NEW LIVESTREAM TESTS ====================

    @Test
    public void processLivestreamEvent_whenNewLivestream_thenCreateWithScheduledStatus() {
        // Arrange
        String videoId = "new-video-001";
        String title = "My New Livestream";
        String scheduledStart = "2026-03-04T10:00:00Z";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(videoId, saved.getVideoId());
        assertEquals(title, saved.getTitle());
        assertEquals(StreamStatus.SCHEDULED, saved.getStatus());
        assertEquals(Instant.parse(scheduledStart), saved.getScheduledStart());
        assertNull(saved.getActualStart());
        assertNull(saved.getActualEnd());
    }

    @Test
    public void processLivestreamEvent_whenNewLivestreamWithoutScheduledStart_thenCreateWithNullScheduledStart() {
        // Arrange
        String videoId = "new-video-002";
        String title = "Livestream No Schedule";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", null, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(videoId, saved.getVideoId());
        assertEquals(StreamStatus.SCHEDULED, saved.getStatus());
        assertNull(saved.getScheduledStart());
    }

    @Test
    public void processLivestreamEvent_whenNewLivestreamWithActualStart_thenCreateAndUpdateToLive() {
        // Arrange
        String videoId = "new-video-with-start-001";
        String title = "Already Started Livestream";
        String scheduledStart = "2026-03-04T10:00:00Z";
        String actualStart = "2026-03-04T10:00:05Z";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart, actualStart, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(2)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(videoId, saved.getVideoId());
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(Instant.parse(actualStart), saved.getActualStart());
    }

    // ==================== processLivestreamEvent - TITLE UPDATE TESTS ====================

    @Test
    public void processLivestreamEvent_whenTitleChanges_thenUpdateTitle() {
        // Arrange
        String videoId = "title-update-001";
        String oldTitle = "Old Title";
        String newTitle = "New Title";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, oldTitle, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                newTitle, "succeeded", scheduledStart.toString(), null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(newTitle));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(newTitle, saved.getTitle());
    }

    @Test
    public void processLivestreamEvent_whenTitleUnchanged_thenNoSave() {
        // Arrange
        String videoId = "title-unchanged-001";
        String title = "Same Title";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== processLivestreamEvent - ACTUAL START TIME - ON TIME ====================

    @Test
    public void processLivestreamEvent_whenActualStartWithin10Seconds_thenStatusOnTime() {
        // Arrange
        String videoId = "ontime-001";
        String title = "On Time Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z"); // 5 seconds late

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(actualStart, saved.getActualStart());
        assertEquals(5L, saved.getDiffSeconds());
    }

    @Test
    public void processLivestreamEvent_whenActualStartExactly10Seconds_thenStatusOnTime() {
        // Arrange
        String videoId = "ontime-exact-001";
        String title = "Exact 10 Seconds Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:10Z"); // Exactly 10 seconds

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(10L, saved.getDiffSeconds());
    }

    // ==================== processLivestreamEvent - ACTUAL START TIME - LATE ====================

    @Test
    public void processLivestreamEvent_whenActualStartMoreThan10Seconds_thenStatusLate() {
        // Arrange
        String videoId = "late-001";
        String title = "Late Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:30Z"); // 30 seconds late

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.LATE, saved.getTimeStatus());
        assertEquals(30L, saved.getDiffSeconds());
    }

    @Test
    public void processLivestreamEvent_whenActualStart11SecondsLate_thenStatusLate() {
        // Arrange
        String videoId = "late-boundary-001";
        String title = "Boundary Late Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:11Z"); // 11 seconds late

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.LATE, saved.getTimeStatus());
        assertEquals(11L, saved.getDiffSeconds());
    }

    // ==================== processLivestreamEvent - ACTUAL START TIME - EARLY ====================

    @Test
    public void processLivestreamEvent_whenActualStartBeforeScheduled_thenStatusEarly() {
        // Arrange
        String videoId = "early-001";
        String title = "Early Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T09:59:45Z"); // 15 seconds early

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.EARLY, saved.getTimeStatus());
        assertEquals(-15L, saved.getDiffSeconds());
    }

    // ==================== processLivestreamEvent - ACTUAL START TIME - ALREADY SET ====================

    @Test
    public void processLivestreamEvent_whenActualStartAlreadySet_thenNoUpdate() {
        // Arrange
        String videoId = "already-started-001";
        String title = "Already Started Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant existingActualStart = Instant.parse("2026-03-04T10:00:05Z");
        Instant newActualStart = Instant.parse("2026-03-04T10:00:30Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, existingActualStart, null,
                StreamStatus.LIVE, TimeStatus.ON_TIME, 5L, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), newActualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== processLivestreamEvent - ACTUAL END TIME TESTS ====================

    @Test
    public void processLivestreamEvent_whenActualEndTimeReceived_thenStatusEnded() {
        // Arrange
        String videoId = "ended-001";
        String title = "Ended Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z");
        Instant actualEnd = Instant.parse("2026-03-04T12:00:05Z"); // 2 hours duration

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, actualStart, null,
                StreamStatus.LIVE, TimeStatus.ON_TIME, 5L, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), actualEnd.toString()
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.ENDED, saved.getStatus());
        assertEquals(actualEnd, saved.getActualEnd());
        assertEquals(7200L, saved.getTotalDurationSeconds()); // 2 hours = 7200 seconds
    }

    @Test
    public void processLivestreamEvent_whenActualEndAlreadySet_thenNoUpdate() {
        // Arrange
        String videoId = "already-ended-001";
        String title = "Already Ended Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z");
        Instant existingActualEnd = Instant.parse("2026-03-04T11:00:05Z");
        Instant newActualEnd = Instant.parse("2026-03-04T12:00:05Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, actualStart, existingActualEnd,
                StreamStatus.ENDED, TimeStatus.ON_TIME, 5L, 3600L
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), newActualEnd.toString()
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== processLivestreamEvent - CANCELLED LIVESTREAM TESTS ====================

    @Test
    public void processLivestreamEvent_whenNullBodyAndLivestreamExists_thenMarkCancelled() {
        // Arrange
        String videoId = "cancelled-001";
        String title = "To Be Cancelled";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));

        // Act
        livestreamService.processLivestreamEvent(videoId, null);

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());
        verify(youtubeApiService, never()).getVideoDetails(anyString());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.CANCELLED, saved.getStatus());
        assertEquals(TimeStatus.CANCELLED, saved.getTimeStatus());
    }

    @Test
    public void processLivestreamEvent_whenNullBodyAndLivestreamDoesNotExist_thenNoSave() {
        // Arrange
        String videoId = "nonexistent-001";

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());

        // Act
        livestreamService.processLivestreamEvent(videoId, null);

        // Assert
        verify(livestreamRepository, never()).save(any());
        verify(youtubeApiService, never()).getVideoDetails(anyString());
    }

    // ==================== processLivestreamEvent - PROCESSED STATUS (NON-LIVESTREAM) TESTS ====================

    @Test
    public void processLivestreamEvent_whenUploadStatusIsProcessed_thenSkipProcessing() {
        // Arrange
        String videoId = "processed-001";
        String title = "Not A Livestream";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "processed", null, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== processLivestreamEvent - API ERROR HANDLING TESTS ====================

    @Test
    public void processLivestreamEvent_whenYoutubeApiReturnsEmpty_thenNoSave() {
        // Arrange
        String videoId = "api-error-001";
        String title = "API Error Stream";

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.empty());

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== processLivestreamEvent - NULL LIVE STREAMING DETAILS TESTS ====================

    @Test
    public void processLivestreamEvent_whenLiveStreamingDetailsIsNull_thenNoUpdateToExisting() {
        // Arrange
        String videoId = "null-details-001";
        String title = "Stream With Null Details";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet(title, "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status("succeeded");
        YoutubeApiResponseDto.VideoItem videoItem = new YoutubeApiResponseDto.VideoItem(snippet, status, null);

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== processLivestreamEvent - COMBINED UPDATE TESTS ====================

    @Test
    public void processLivestreamEvent_whenTitleChangesAndStreamStarts_thenBothUpdated() {
        // Arrange
        String videoId = "combined-001";
        String oldTitle = "Old Title";
        String newTitle = "New Title";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z");

        Livestream existing = createExistingLivestream(
                videoId, oldTitle, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                newTitle, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        livestreamService.processLivestreamEvent(videoId, new VideoEventDto(newTitle));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(newTitle, saved.getTitle());
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(actualStart, saved.getActualStart());
    }
}
