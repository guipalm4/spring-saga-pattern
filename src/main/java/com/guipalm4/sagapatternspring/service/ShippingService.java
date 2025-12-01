package com.guipalm4.sagapatternspring.service;

import com.guipalm4.sagapatternspring.messaging.request.CompensationRequest;
import com.guipalm4.sagapatternspring.messaging.request.ShippingRequest;
import com.guipalm4.sagapatternspring.messaging.response.ShippingResponse;
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
public class ShippingService {

    private final SqsTemplate sqsTemplate;
    private final Map<String, String> shipments = new ConcurrentHashMap<>();

    public ShippingService(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @SqsListener("shipping-queue")
    public void processShippingRequest(ShippingRequest request) {
        log.info("Processando solicitação de envio: {}", request);

        try {
            // Simular processamento de envio
            Thread.sleep(500);

            // Simular falha em 10% dos casos
            boolean success = Math.random() > 0.1;

            ShippingResponse response;

            if (success) {
                String trackingNumber = "TRK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                shipments.put(request.getSagaId(), trackingNumber);

                response = ShippingResponse.builder()
                        .sagaId(request.getSagaId())
                        .orderId(request.getOrderId())
                        .trackingNumber(trackingNumber)
                        .successful(true)
                        .shippingProvider("Express Delivery")
                        .scheduledDelivery(LocalDateTime.now().plusDays(3))
                        .processedAt(LocalDateTime.now())
                        .build();

                log.info("Envio processado com sucesso: {} - Tracking: {}",
                        request.getSagaId(), trackingNumber);
            } else {
                response = ShippingResponse.builder()
                        .sagaId(request.getSagaId())
                        .orderId(request.getOrderId())
                        .successful(false)
                        .errorMessage("Endereço de entrega inválido")
                        .processedAt(LocalDateTime.now())
                        .build();

                log.warn("Falha no processamento de envio: {}", request.getSagaId());
            }

            sqsTemplate.send("shipping-response-queue", response);

        } catch (Exception e) {
            log.error("Erro ao processar solicitação de envio: {}", request.getSagaId(), e);

            ShippingResponse errorResponse = ShippingResponse.builder()
                    .sagaId(request.getSagaId())
                    .orderId(request.getOrderId())
                    .successful(false)
                    .errorMessage("Erro interno: " + e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();

            sqsTemplate.send("shipping-response-queue", errorResponse);
        }
    }

    @SqsListener("shipping-compensation-queue")
    public void compensateShipping(CompensationRequest request) {
        log.info("Executando compensação de envio: {}", request.getSagaId());

        try {
            String trackingNumber = shipments.remove(request.getSagaId());

            if (trackingNumber != null) {
                // Simular cancelamento do envio
                log.info("Envio cancelado: {} para saga: {}", trackingNumber, request.getSagaId());

                // Aqui você poderia integrar com APIs reais de transportadoras
                // para cancelar o envio

            } else {
                log.warn("Nenhum envio encontrado para compensação: {}", request.getSagaId());
            }

        } catch (Exception e) {
            log.error("Erro na compensação de envio para saga: {}", request.getSagaId(), e);
        }
    }

    public Map<String, String> getCurrentShipments() {
        return new ConcurrentHashMap<>(shipments);
    }
}