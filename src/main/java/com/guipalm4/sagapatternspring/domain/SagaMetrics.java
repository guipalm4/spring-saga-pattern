package com.guipalm4.sagapatternspring.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SagaMetrics {
    private Long totalStarted;
    private Long totalCompleted;
    private Long totalFailed;
    private Long totalCompensated;
    private Double averageDurationMs;

    public double getSuccessRate() {
        if (totalStarted == 0) return 0.0;
        return (totalCompleted.doubleValue() / totalStarted.doubleValue()) * 100.0;
    }

    public double getFailureRate() {
        if (totalStarted == 0) return 0.0;
        return ((totalFailed + totalCompensated) / totalStarted.doubleValue()) * 100.0;
    }
}

