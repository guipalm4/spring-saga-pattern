package com.guipalm4.sagapatternspring.domain;

import com.guipalm4.sagapatternspring.domain.enums.SagaStatus;
import com.guipalm4.sagapatternspring.domain.enums.SagaStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaTransaction {

    @Id
    @Column(name = "saga_id", length = 100)
    private String sagaId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 30)
    @Builder.Default
    private SagaStep currentStep = SagaStep.ORDER_CREATED;

    @Column(name = "compensation_step", length = 30)
    @Enumerated(EnumType.STRING)
    private SagaStep compensationStep;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SagaStatus.STARTED;
        }
        if (this.currentStep == null) {
            this.currentStep = SagaStep.ORDER_CREATED;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Métodos de conveniência
    public void markAsCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompensating(SagaStep compensationStep) {
        this.status = SagaStatus.COMPENSATING;
        this.compensationStep = compensationStep;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStep(SagaStep newStep) {
        this.currentStep = newStep;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return SagaStatus.COMPLETED.equals(this.status);
    }

    public boolean isFailed() {
        return SagaStatus.FAILED.equals(this.status);
    }

    public boolean isCompensating() {
        return SagaStatus.COMPENSATING.equals(this.status);
    }
}