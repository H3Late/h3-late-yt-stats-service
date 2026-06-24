package com.h3late.stats.dto;

import com.h3late.stats.entity.ReportStatus;
import lombok.Data;

@Data
public class ReportStatusUpdateRequest {
    private ReportStatus status;
}
