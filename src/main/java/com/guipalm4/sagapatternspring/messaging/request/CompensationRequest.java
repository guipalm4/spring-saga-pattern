package com.guipalm4.sagapatternspring.messaging.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationRequest {
    private String sagaId;
    private Long orderId;
    private String compensationType;
    private Map<String, Object> compensationData;
    private LocalDateTime requestedAt;
    private String reason;
}