package com.h3late.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
@Slf4j
public class GameNotificationService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String gameServerUrl;

    public GameNotificationService(@Value("${game.server.url}") String gameServerUrl) {
        this.gameServerUrl = gameServerUrl;
    }

    public void notifyStreamLive(String videoId, String title) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("videoId", videoId, "title", title));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gameServerUrl + "/api/live"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(r -> log.info("Notified game server of live stream: {} - {}", videoId, title))
                    .exceptionally(e -> {
                        log.error("Failed to notify game server of live stream '{}': {}", videoId, e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to build game server notification for '{}': {}", videoId, e.getMessage());
        }
    }
}
