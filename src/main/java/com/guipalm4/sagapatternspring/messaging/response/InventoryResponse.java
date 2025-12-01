package com.guipalm4.sagapatternspring.messaging.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private String sagaId;
    private Long orderId;
    private String productId;
    private Integer requestedQuantity;
    private Integer reservedQuantity;
    private boolean successful;
    private String errorMessage;
    private String reservationId;
    private LocalDateTime processedAt;
}