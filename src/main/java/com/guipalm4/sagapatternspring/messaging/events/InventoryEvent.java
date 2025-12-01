package com.guipalm4.sagapatternspring.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEvent {
    private String eventId;
    private String eventType;
    private String sagaId;
    private Long orderId;
    private String productId;
    private Integer quantity;
    private String reservationId;
    private String inventoryStatus;
    private LocalDateTime eventTime;
    private String source;
    private String errorMessage;
}
