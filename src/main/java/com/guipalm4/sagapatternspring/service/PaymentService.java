package com.guipalm4.sagapatternspring.service;

import com.guipalm4.sagapatternspring.messaging.request.CompensationRequest;
import com.guipalm4.sagapatternspring.messaging.request.PaymentRequest;
import com.guipalm4.sagapatternspring.messaging.response.PaymentResponse;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final SqsTemplate sqsTemplate;

    @SqsListener("payment-queue")
    public void processPayment(PaymentRequest request) {
        log.info("Processando pagamento: {}", request);

        try {
            // Simular processamento de pagamento
            Thread.sleep(1000);

            // Simular falha em 20% dos casos
            boolean success = Math.random() > 0.2;

            PaymentResponse response = PaymentResponse.builder()
                    .sagaId(request.getSagaId())
                    .orderId(request.getOrderId())
                    .successful(success)
                    .transactionId(success ? UUID.randomUUID().toString() : null)
                    .build();

            sqsTemplate.send("payment-response-queue", response);

            log.info("Pagamento processado: {} - Sucesso: {}",
                    request.getSagaId(), success);

        } catch (Exception e) {
            log.error("Erro ao processar pagamento: {}", request.getSagaId(), e);

            PaymentResponse response = PaymentResponse.builder()
                    .sagaId(request.getSagaId())
                    .orderId(request.getOrderId())
                    .successful(false)
                    .build();

            sqsTemplate.send("payment-response-queue", response);
        }
    }

    @SqsListener("payment-compensation-queue")
    public void compensatePayment(CompensationRequest request) {
        log.info("Executando compensação de pagamento: {}", request.getSagaId());

        // Implementar lógica de estorno
        // Salvar no DynamoDB para auditoria
    }
}