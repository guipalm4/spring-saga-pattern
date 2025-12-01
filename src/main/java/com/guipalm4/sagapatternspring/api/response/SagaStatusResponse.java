package com.guipalm4.sagapatternspring.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaStatusResponse {
    private String sagaId;
    private Long orderId;
    private String status;
    private String currentStep;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
