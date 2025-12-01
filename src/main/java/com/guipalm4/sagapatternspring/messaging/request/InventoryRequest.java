package com.guipalm4.sagapatternspring.messaging.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {
    private String sagaId;
    private Long orderId;
    private String productId;
    private Integer quantity;
    private String operation; // RESERVE, RELEASE
    private LocalDateTime requestedAt;
}