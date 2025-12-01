package com.guipalm4.sagapatternspring.config;

import com.guipalm4.sagapatternspring.domain.SagaMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@Slf4j
public class SagaMetricsCollector {

    private final Counter sagaStartedCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaFailedCounter;
    private final Counter sagaCompensatedCounter;
    private final Timer sagaDurationTimer;

    public SagaMetricsCollector(MeterRegistry meterRegistry) {
        log.info("🔧 Inicializando SagaMetricsCollector...");

        this.sagaStartedCounter = Counter.builder("saga_started_total")
                .description("Total number of sagas started")
                .tag("service", "saga-orchestrator")
                .register(meterRegistry);

        this.sagaCompletedCounter = Counter.builder("saga_completed_total")
                .description("Total number of sagas completed successfully")
                .tag("service", "saga-orchestrator")
                .tag("status", "success")
                .register(meterRegistry);

        this.sagaFailedCounter = Counter.builder("saga_failed_total")
                .description("Total number of sagas that failed")
                .tag("service", "saga-orchestrator")
                .tag("status", "failed")
                .register(meterRegistry);

        this.sagaCompensatedCounter = Counter.builder("saga_compensated_total")
                .description("Total number of sagas that were compensated")
                .tag("service", "saga-orchestrator")
                .tag("status", "compensated")
                .register(meterRegistry);

        this.sagaDurationTimer = Timer.builder("saga_duration_seconds")
                .description("Duration of saga execution in seconds")
                .tag("service", "saga-orchestrator")
                .register(meterRegistry);

        log.info("✅ SagaMetricsCollector inicializado com sucesso");
    }

    public void recordSagaStarted() {
        sagaStartedCounter.increment();
        log.debug("📊 Métrica: Saga iniciada (total: {})", sagaStartedCounter.count());
    }

    public void recordSagaCompleted() {
        sagaCompletedCounter.increment();
        log.debug("�� Métrica: Saga concluída (total: {})", sagaCompletedCounter.count());
    }

    public void recordSagaFailed() {
        sagaFailedCounter.increment();
        log.debug("📊 Métrica: Saga falhou (total: {})", sagaFailedCounter.count());
    }

    public void recordSagaCompensated() {
        sagaCompensatedCounter.increment();
        log.debug("📊 Métrica: Saga compensada (total: {})", sagaCompensatedCounter.count());
    }

    public void recordSagaDuration(LocalDateTime startTime, LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        sagaDurationTimer.record(duration);
        log.debug("📊 Métrica: Duração da saga: {}ms", duration.toMillis());
    }

    public SagaMetrics getCurrentMetrics() {
        return SagaMetrics.builder()
                .totalStarted((long) sagaStartedCounter.count())
                .totalCompleted((long) sagaCompletedCounter.count())
                .totalFailed((long) sagaFailedCounter.count())
                .totalCompensated((long) sagaCompensatedCounter.count())
                .averageDurationMs(sagaDurationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
                .build();
    }
}