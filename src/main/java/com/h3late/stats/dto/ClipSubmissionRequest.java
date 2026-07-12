package com.h3late.stats.dto;

import lombok.Data;

@Data
public class ClipSubmissionRequest {
    private String videoId;
    private String title;
    private String description;
    private int startSeconds;
    private int endSeconds;
    private String userToken;
    private String submitterName;
}
