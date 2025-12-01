package com.guipalm4.sagapatternspring.api.controller;

import com.guipalm4.sagapatternspring.config.SagaMetricsCollector;
import com.guipalm4.sagapatternspring.domain.SagaMetrics;
import com.guipalm4.sagapatternspring.domain.enums.OrderStatus;
import com.guipalm4.sagapatternspring.service.SagaOrchestrator;
import com.guipalm4.sagapatternspring.api.request.CreateOrderRequest;
import com.guipalm4.sagapatternspring.api.response.OrderResponse;
import com.guipalm4.sagapatternspring.api.response.SagaStatusResponse;
import com.guipalm4.sagapatternspring.domain.Order;
import com.guipalm4.sagapatternspring.domain.SagaTransaction;
import com.guipalm4.sagapatternspring.repository.SagaTransactionRepository;
import com.guipalm4.sagapatternspring.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final SagaOrchestrator sagaOrchestrator;
    private final SagaTransactionRepository sagaRepository;
    private final SagaMetricsCollector sagaMetricsCollector;

    public OrderController(
            final OrderService orderService,
            final SagaOrchestrator sagaOrchestrator,
            final SagaTransactionRepository sagaRepository,
            final SagaMetricsCollector sagaMetricsCollector
    ) {
        this.orderService = orderService;
        this.sagaOrchestrator = sagaOrchestrator;
        this.sagaRepository = sagaRepository;
        this.sagaMetricsCollector = sagaMetricsCollector;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            log.info("Criando novo pedido: {}", request);

            Order order = Order.builder()
                    .customerId(request.getCustomerId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .amount(request.getAmount())
                    .build();

            Order createdOrder = orderService.createOrder(order);
            String sagaId = sagaOrchestrator.startOrderSaga(createdOrder);

            OrderResponse response = OrderResponse.builder()
                    .orderId(createdOrder.getId())
                    .sagaId(sagaId)
                    .status(createdOrder.getStatus().name())
                    .message("Pedido criado e saga iniciada com sucesso")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Erro ao criar pedido", e);

            OrderResponse errorResponse = OrderResponse.builder()
                    .message("Erro ao criar pedido: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.findById(orderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String customerId) {
        List<Order> orders = orderService.findByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.findByStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/sagas/{sagaId}/status")
    public ResponseEntity<SagaStatusResponse> getSagaStatus(@PathVariable String sagaId) {
        Optional<SagaTransaction> saga = sagaRepository.findById(sagaId);

        if (saga.isPresent()) {
            SagaTransaction transaction = saga.get();

            SagaStatusResponse response = SagaStatusResponse.builder()
                    .sagaId(sagaId)
                    .orderId(transaction.getOrderId())
                    .status(transaction.getStatus().name())
                    .currentStep(transaction.getCurrentStep().name())
                    .createdAt(transaction.getCreatedAt())
                    .updatedAt(transaction.getUpdatedAt())
                    .build();

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sagas")
    public ResponseEntity<List<SagaTransaction>> getAllSagas() {
        List<SagaTransaction> sagas = sagaRepository.findAll();
        return ResponseEntity.ok(sagas);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        try {
            Order cancelledOrder = orderService.cancelOrder(orderId);

            OrderResponse response = OrderResponse.builder()
                    .orderId(cancelledOrder.getId())
                    .status(cancelledOrder.getStatus().name())
                    .message("Pedido cancelado com sucesso")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao cancelar pedido: {}", orderId, e);

            OrderResponse errorResponse = OrderResponse.builder()
                    .orderId(orderId)
                    .message("Erro ao cancelar pedido: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @GetMapping("/metrics/saga")
    public ResponseEntity<SagaMetrics> getSagaMetrics() {
        SagaMetrics metrics = sagaMetricsCollector.getCurrentMetrics();
        log.info("Métricas atuais: {}", metrics);
        return ResponseEntity.ok(metrics);
    }
}
