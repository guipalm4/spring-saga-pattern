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
public class ShippingRequest {
    private String sagaId;
    private Long orderId;
    private String customerId;
    private String shippingAddress;
    private String shippingMethod;
    private LocalDateTime requestedAt;
}