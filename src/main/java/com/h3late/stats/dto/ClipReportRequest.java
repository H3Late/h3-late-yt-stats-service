package com.h3late.stats.dto;

import com.h3late.stats.entity.ReportReason;
import lombok.Data;

@Data
public class ClipReportRequest {
    private String reporterToken;
    private ReportReason reason;
    private String description;
}
