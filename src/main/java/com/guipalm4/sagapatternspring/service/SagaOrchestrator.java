package com.guipalm4.sagapatternspring.service;

import com.guipalm4.sagapatternspring.config.SagaMetricsCollector;
import com.guipalm4.sagapatternspring.domain.SagaMetrics;
import com.guipalm4.sagapatternspring.messaging.request.CompensationRequest;
import com.guipalm4.sagapatternspring.messaging.request.InventoryRequest;
import com.guipalm4.sagapatternspring.messaging.response.InventoryResponse;
import com.guipalm4.sagapatternspring.domain.Order;
import com.guipalm4.sagapatternspring.messaging.request.PaymentRequest;
import com.guipalm4.sagapatternspring.messaging.response.PaymentResponse;
import com.guipalm4.sagapatternspring.domain.enums.SagaStatus;
import com.guipalm4.sagapatternspring.domain.enums.SagaStep;
import com.guipalm4.sagapatternspring.domain.SagaTransaction;
import com.guipalm4.sagapatternspring.messaging.request.ShippingRequest;
import com.guipalm4.sagapatternspring.messaging.response.ShippingResponse;
import com.guipalm4.sagapatternspring.repository.SagaTransactionRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SagaOrchestrator {

    private final SqsTemplate sqsTemplate;
    private final SagaTransactionRepository sagaRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final SagaMetricsCollector sagaMetricsCollector;

    public SagaOrchestrator(
            SqsTemplate sqsTemplate,
            SagaTransactionRepository sagaRepository,
            OrderService orderService,
            PaymentService paymentService,
            InventoryService inventoryService,
            SagaMetricsCollector sagaMetricsCollector
    ) {
        this.sqsTemplate = sqsTemplate;
        this.sagaRepository = sagaRepository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.sagaMetricsCollector = sagaMetricsCollector;
    }

    @Transactional
    public String startOrderSaga(Order order) {
        String sagaId = UUID.randomUUID().toString();

        try {
            // Criar transação saga
            SagaTransaction saga = SagaTransaction.builder()
                    .sagaId(sagaId)
                    .orderId(order.getId())
                    .status(SagaStatus.STARTED)
                    .currentStep(SagaStep.ORDER_CREATED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            sagaRepository.save(saga);

            sagaMetricsCollector.recordSagaStarted();

            processPayment(sagaId, order);

            log.info("Saga iniciada: {} para pedido: {}", sagaId, order.getId());
            return sagaId;

        } catch (Exception e) {
            log.error("Erro ao iniciar saga para pedido: {}", order.getId(), e);
            sagaMetricsCollector.recordSagaFailed();
            throw new RuntimeException("Falha ao iniciar saga", e);
        }
    }

    private void processPayment(String sagaId, Order order) {
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .sagaId(sagaId)
                    .orderId(order.getId())
                    .customerId(order.getCustomerId())
                    .amount(order.getAmount())
                    .paymentMethod("CREDIT_CARD") // Valor padrão
                    .requestedAt(LocalDateTime.now())
                    .build();

            sqsTemplate.send("payment-queue", paymentRequest);

            updateSagaStep(sagaId, SagaStep.PAYMENT_PROCESSED, SagaStatus.IN_PROGRESS);
            log.info("Solicitação de pagamento enviada para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao processar pagamento para saga: {}", sagaId, e);
            failSaga(sagaId, "Erro no processamento de pagamento: " + e.getMessage());
        }
    }

    @SqsListener("payment-response-queue")
    public void handlePaymentResponse(PaymentResponse response) {
        log.info("Resposta de pagamento recebida: {}", response);

        try {
            if (response.isSuccessful()) {
                reserveInventory(response.getSagaId(), response.getOrderId());
            } else {
                log.warn("Pagamento falhou para saga: {} - Motivo: {}",
                        response.getSagaId(), response.getErrorMessage());
                compensateSaga(response.getSagaId(), SagaStep.PAYMENT_PROCESSED);
            }
        } catch (Exception e) {
            log.error("Erro ao processar resposta de pagamento para saga: {}",
                    response.getSagaId(), e);
            failSaga(response.getSagaId(), "Erro ao processar resposta de pagamento");
        }
    }

    private void reserveInventory(String sagaId, Long orderId) {
        try {
            Order order = orderService.findById(orderId);

            InventoryRequest inventoryRequest = InventoryRequest.builder()
                    .sagaId(sagaId)
                    .orderId(orderId)
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .operation("RESERVE")
                    .requestedAt(LocalDateTime.now())
                    .build();

            sqsTemplate.send("inventory-queue", inventoryRequest);
            updateSagaStep(sagaId, SagaStep.INVENTORY_RESERVED, SagaStatus.IN_PROGRESS);
            log.info("Solicitação de reserva de estoque enviada para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao reservar estoque para saga: {}", sagaId, e);
            compensateSaga(sagaId, SagaStep.PAYMENT_PROCESSED);
        }
    }

    @SqsListener("inventory-response-queue")
    public void handleInventoryResponse(InventoryResponse response) {
        log.info("Resposta de estoque recebida: {}", response);

        try {
            if (response.isSuccessful()) {
                arrangeShipping(response.getSagaId(), response.getOrderId());
            } else {
                log.warn("Reserva de estoque falhou para saga: {} - Motivo: {}",
                        response.getSagaId(), response.getErrorMessage());
                compensateSaga(response.getSagaId(), SagaStep.INVENTORY_RESERVED);
            }
        } catch (Exception e) {
            log.error("Erro ao processar resposta de estoque para saga: {}",
                    response.getSagaId(), e);
            compensateSaga(response.getSagaId(), SagaStep.INVENTORY_RESERVED);
        }
    }

    private void arrangeShipping(String sagaId, Long orderId) {
        try {
            Order order = orderService.findById(orderId);

            ShippingRequest shippingRequest = ShippingRequest.builder()
                    .sagaId(sagaId)
                    .orderId(orderId)
                    .customerId(order.getCustomerId())
                    .shippingAddress("Endereço padrão") // Você pode pegar do pedido
                    .shippingMethod("STANDARD")
                    .requestedAt(LocalDateTime.now())
                    .build();

            sqsTemplate.send("shipping-queue", shippingRequest);
            updateSagaStep(sagaId, SagaStep.SHIPPING_ARRANGED, SagaStatus.IN_PROGRESS);
            log.info("Solicitação de envio enviada para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao arranjar envio para saga: {}", sagaId, e);
            compensateSaga(sagaId, SagaStep.INVENTORY_RESERVED);
        }
    }

    @SqsListener("shipping-response-queue")
    public void handleShippingResponse(ShippingResponse response) {
        log.info("Resposta de envio recebida: {}", response);

        try {
            if (response.isSuccessful()) {
                completeSaga(response.getSagaId());
            } else {
                log.warn("Arranjo de envio falhou para saga: {} - Motivo: {}",
                        response.getSagaId(), response.getErrorMessage());
                compensateSaga(response.getSagaId(), SagaStep.SHIPPING_ARRANGED);
            }
        } catch (Exception e) {
            log.error("Erro ao processar resposta de envio para saga: {}",
                    response.getSagaId(), e);
            compensateSaga(response.getSagaId(), SagaStep.SHIPPING_ARRANGED);
        }
    }

    private void completeSaga(String sagaId) {
        try {
            SagaTransaction saga = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));

            saga.setStatus(SagaStatus.COMPLETED);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            // Marcar pedido como enviado
            orderService.shipOrder(saga.getOrderId());

            // ✅ Registrar métricas de sucesso
            sagaMetricsCollector.recordSagaCompleted();
            sagaMetricsCollector.recordSagaDuration(saga.getCreatedAt(), LocalDateTime.now());

            log.info("✅ Saga concluída com sucesso: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao completar saga: {}", sagaId, e);
            failSaga(sagaId, "Erro na conclusão da saga: " + e.getMessage());
        }
    }

    private void compensateSaga(String sagaId, SagaStep failedStep) {
        log.info("🔄 Iniciando compensação para saga: {} na etapa: {}", sagaId, failedStep);

        try {
            SagaTransaction saga = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));

            saga.setStatus(SagaStatus.COMPENSATING);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            // Executar compensações na ordem reversa
            switch (failedStep) {
                case SHIPPING_ARRANGED:
                    cancelShipping(sagaId, saga.getOrderId());
                    // fall through
                case INVENTORY_RESERVED:
                    releaseInventory(sagaId, saga.getOrderId());
                    // fall through
                case PAYMENT_PROCESSED:
                    refundPayment(sagaId, saga.getOrderId());
                    // fall through
                case ORDER_CREATED:
                    cancelOrder(sagaId);
                    break;
            }

            saga.setStatus(SagaStatus.COMPENSATED);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            // ✅ Registrar métricas de compensação
            sagaMetricsCollector.recordSagaCompensated();
            sagaMetricsCollector.recordSagaDuration(saga.getCreatedAt(), LocalDateTime.now());

            log.info("🔄 Saga compensada com sucesso: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro durante compensação da saga: {}", sagaId, e);
            failSaga(sagaId, "Erro na compensação: " + e.getMessage());
        }
    }

    private void failSaga(String sagaId, String reason) {
        try {
            SagaTransaction saga = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));

            saga.setStatus(SagaStatus.FAILED);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            // Cancelar pedido em caso de falha definitiva
            orderService.cancelOrder(saga.getOrderId());

            // ✅ Registrar métricas de falha
            sagaMetricsCollector.recordSagaFailed();
            sagaMetricsCollector.recordSagaDuration(saga.getCreatedAt(), LocalDateTime.now());

            log.error("❌ Saga falhou definitivamente: {} - Motivo: {}", sagaId, reason);

        } catch (Exception e) {
            log.error("Erro ao marcar saga como falha: {}", sagaId, e);
        }
    }

    public void compensateSagaTimeout(String sagaId, SagaStep currentStep) {
        log.warn("⏰ Timeout detectado para saga: {} na etapa: {}", sagaId, currentStep);

        try {
            SagaTransaction saga = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));

            // Marcar como timeout antes de compensar
            saga.setStatus(SagaStatus.FAILED);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            // ✅ Registrar métrica de falha por timeout
            sagaMetricsCollector.recordSagaFailed();
            sagaMetricsCollector.recordSagaDuration(saga.getCreatedAt(), LocalDateTime.now());

            // Iniciar compensação
            compensateSaga(sagaId, currentStep);

        } catch (Exception e) {
            log.error("Erro ao processar timeout da saga: {}", sagaId, e);
        }
    }

    private void updateSagaStep(String sagaId, SagaStep step, SagaStatus status) {
        try {
            SagaTransaction saga = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));

            saga.setCurrentStep(step);
            saga.setStatus(status);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            log.debug("Saga {} atualizada: step={}, status={}", sagaId, step, status);

        } catch (Exception e) {
            log.error("Erro ao atualizar step da saga: {}", sagaId, e);
            throw new RuntimeException("Falha ao atualizar saga", e);
        }
    }

    // ✅ Métodos de compensação atualizados com mais informações
    private void cancelShipping(String sagaId, Long orderId) {
        try {
            Map<String, Object> compensationData = new HashMap<>();
            compensationData.put("orderId", orderId);
            compensationData.put("action", "CANCEL_SHIPPING");

            CompensationRequest request = CompensationRequest.builder()
                    .sagaId(sagaId)
                    .orderId(orderId)
                    .compensationType("SHIPPING_CANCELLATION")
                    .compensationData(compensationData)
                    .requestedAt(LocalDateTime.now())
                    .reason("Saga compensation required")
                    .build();

            sqsTemplate.send("shipping-compensation-queue", request);
            log.info("Solicitação de cancelamento de envio enviada para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao cancelar envio para saga: {}", sagaId, e);
        }
    }

    private void releaseInventory(String sagaId, Long orderId) {
        try {
            Order order = orderService.findById(orderId);

            Map<String, Object> compensationData = new HashMap<>();
            compensationData.put("orderId", orderId);
            compensationData.put("productId", order.getProductId());
            compensationData.put("quantity", order.getQuantity());
            compensationData.put("action", "RELEASE_INVENTORY");

            CompensationRequest request = CompensationRequest.builder()
                    .sagaId(sagaId)
                    .orderId(orderId)
                    .compensationType("INVENTORY_RELEASE")
                    .compensationData(compensationData)
                    .requestedAt(LocalDateTime.now())
                    .reason("Saga compensation required")
                    .build();

            sqsTemplate.send("inventory-compensation-queue", request);
            log.info("Solicitação de liberação de estoque enviada para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao liberar estoque para saga: {}", sagaId, e);
        }
    }

    private void refundPayment(String sagaId, Long orderId) {
        try {
            Order order = orderService.findById(orderId);

            Map<String, Object> compensationData = new HashMap<>();
            compensationData.put("orderId", orderId);
            compensationData.put("customerId", order.getCustomerId());
            compensationData.put("amount", order.getAmount());
            compensationData.put("action", "REFUND_PAYMENT");

            CompensationRequest request = CompensationRequest.builder()
                    .sagaId(sagaId)
                    .orderId(orderId)
                    .compensationType("PAYMENT_REFUND")
                    .compensationData(compensationData)
                    .requestedAt(LocalDateTime.now())
                    .reason("Saga compensation required")
                    .build();

            sqsTemplate.send("payment-compensation-queue", request);
            log.info("Solicitação de estorno de pagamento enviada para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao estornar pagamento para saga: {}", sagaId, e);
        }
    }

    private void cancelOrder(String sagaId) {
        try {
            SagaTransaction saga = sagaRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));

            orderService.cancelOrder(saga.getOrderId());
            log.info("Pedido cancelado para saga: {}", sagaId);

        } catch (Exception e) {
            log.error("Erro ao cancelar pedido para saga: {}", sagaId, e);
        }
    }

    // ✅ Método utilitário para obter métricas atuais
    public SagaMetrics getCurrentMetrics() {
        return sagaMetricsCollector.getCurrentMetrics();
    }
}