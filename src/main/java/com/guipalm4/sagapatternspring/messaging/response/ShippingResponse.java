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
public class ShippingResponse {
    private String sagaId;
    private Long orderId;
    private String trackingNumber;
    private boolean successful;
    private String errorMessage;
    private String shippingProvider;
    private LocalDateTime scheduledDelivery;
    private LocalDateTime processedAt;
}