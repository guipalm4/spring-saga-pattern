package com.guipalm4.sagapatternspring.messaging.response;

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
public class PaymentResponse {
    private String sagaId;
    private Long orderId;
    private String transactionId;
    private boolean successful;
    private String errorMessage;
    private String errorCode;
    private BigDecimal processedAmount;
    private LocalDateTime processedAt;
}