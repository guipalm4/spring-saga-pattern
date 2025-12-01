package com.guipalm4.sagapatternspring.messaging.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private String sagaId;
    private Long orderId;
    private String customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime requestedAt;
}