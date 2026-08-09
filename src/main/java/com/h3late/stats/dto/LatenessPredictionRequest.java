package com.h3late.stats.dto;

import lombok.Data;

@Data
public class LatenessPredictionRequest {
    private Integer diffSeconds;
    private String userToken;
}
