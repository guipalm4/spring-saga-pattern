package com.guipalm4.sagapatternspring.service;

import com.guipalm4.sagapatternspring.messaging.events.InventoryEvent;
import com.guipalm4.sagapatternspring.messaging.request.CompensationRequest;
import com.guipalm4.sagapatternspring.messaging.request.InventoryRequest;
import com.guipalm4.sagapatternspring.messaging.response.InventoryResponse;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InventoryService {

    private final SqsTemplate sqsTemplate;

    // Simulação de estoque em memória
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Map<String, String> reservations = new ConcurrentHashMap<>();

    public InventoryService(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
        initializeInventory();
    }

    private void initializeInventory() {
        // Inicializar com alguns produtos
        inventory.put("product-456", 100);
        inventory.put("product-789", 50);
        inventory.put("product-123", 25);

        log.info("Estoque inicializado: {}", inventory);
    }

    @SqsListener("inventory-queue")
    public void processInventoryRequest(InventoryRequest request) {
        log.info("Processando solicitação de estoque: {}", request);

        try {
            InventoryResponse response;

            if ("RESERVE".equals(request.getOperation())) {
                response = reserveInventory(request);
            } else if ("RELEASE".equals(request.getOperation())) {
                response = releaseInventory(request);
            } else {
                response = InventoryResponse.builder()
                        .sagaId(request.getSagaId())
                        .orderId(request.getOrderId())
                        .productId(request.getProductId())
                        .successful(false)
                        .errorMessage("Operação inválida: " + request.getOperation())
                        .processedAt(LocalDateTime.now())
                        .build();
            }

            sqsTemplate.send("inventory-response-queue", response);
            publishInventoryEvent(request, response);

        } catch (Exception e) {
            log.error("Erro ao processar solicitação de estoque: {}", request.getSagaId(), e);

            InventoryResponse errorResponse = InventoryResponse.builder()
                    .sagaId(request.getSagaId())
                    .orderId(request.getOrderId())
                    .productId(request.getProductId())
                    .successful(false)
                    .errorMessage("Erro interno: " + e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();

            sqsTemplate.send("inventory-response-queue", errorResponse);
        }
    }

    private InventoryResponse reserveInventory(InventoryRequest request) {
        String productId = request.getProductId();
        Integer requestedQuantity = request.getQuantity();

        synchronized (inventory) {
            Integer availableQuantity = inventory.getOrDefault(productId, 0);

            if (availableQuantity >= requestedQuantity) {
                // Reservar estoque
                inventory.put(productId, availableQuantity - requestedQuantity);
                String reservationId = UUID.randomUUID().toString();
                reservations.put(reservationId, request.getSagaId());

                log.info("Estoque reservado: {} unidades do produto {} para saga {}",
                        requestedQuantity, productId, request.getSagaId());

                return InventoryResponse.builder()
                        .sagaId(request.getSagaId())
                        .orderId(request.getOrderId())
                        .productId(productId)
                        .requestedQuantity(requestedQuantity)
                        .reservedQuantity(requestedQuantity)
                        .successful(true)
                        .reservationId(reservationId)
                        .processedAt(LocalDateTime.now())
                        .build();
            } else {
                log.warn("Estoque insuficiente: {} disponível, {} solicitado para produto {}",
                        availableQuantity, requestedQuantity, productId);

                return InventoryResponse.builder()
                        .sagaId(request.getSagaId())
                        .orderId(request.getOrderId())
                        .productId(productId)
                        .requestedQuantity(requestedQuantity)
                        .reservedQuantity(0)
                        .successful(false)
                        .errorMessage("Estoque insuficiente")
                        .processedAt(LocalDateTime.now())
                        .build();
            }
        }
    }

    private InventoryResponse releaseInventory(InventoryRequest request) {
        String productId = request.getProductId();
        Integer quantity = request.getQuantity();

        synchronized (inventory) {
            Integer currentQuantity = inventory.getOrDefault(productId, 0);
            inventory.put(productId, currentQuantity + quantity);

            log.info("Estoque liberado: {} unidades do produto {} para saga {}",
                    quantity, productId, request.getSagaId());

            return InventoryResponse.builder()
                    .sagaId(request.getSagaId())
                    .orderId(request.getOrderId())
                    .productId(productId)
                    .requestedQuantity(quantity)
                    .reservedQuantity(quantity)
                    .successful(true)
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    @SqsListener("inventory-compensation-queue")
    public void compensateInventory(CompensationRequest request) {
        log.info("Executando compensação de estoque: {}", request.getSagaId());

        try {
            // Buscar dados da compensação
            Map<String, Object> data = request.getCompensationData();
            String productId = (String) data.get("productId");
            Integer quantity = (Integer) data.get("quantity");

            if (productId != null && quantity != null) {
                InventoryRequest releaseRequest = InventoryRequest.builder()
                        .sagaId(request.getSagaId())
                        .orderId(request.getOrderId())
                        .productId(productId)
                        .quantity(quantity)
                        .operation("RELEASE")
                        .requestedAt(LocalDateTime.now())
                        .build();

                releaseInventory(releaseRequest);
                log.info("Compensação de estoque concluída para saga: {}", request.getSagaId());
            }

        } catch (Exception e) {
            log.error("Erro na compensação de estoque para saga: {}", request.getSagaId(), e);
        }
    }

    private void publishInventoryEvent(InventoryRequest request, InventoryResponse response) {
        InventoryEvent event = InventoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(response.isSuccessful() ? "INVENTORY_RESERVED" : "INVENTORY_RESERVATION_FAILED")
                .sagaId(request.getSagaId())
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .reservationId(response.getReservationId())
                .inventoryStatus(response.isSuccessful() ? "RESERVED" : "FAILED")
                .eventTime(LocalDateTime.now())
                .source("InventoryService")
                .errorMessage(response.getErrorMessage())
                .build();

        try {
            sqsTemplate.send("inventory-events-queue", event);
        } catch (Exception e) {
            log.error("Erro ao publicar evento de estoque", e);
        }
    }

    public Map<String, Integer> getCurrentInventory() {
        return new ConcurrentHashMap<>(inventory);
    }
}