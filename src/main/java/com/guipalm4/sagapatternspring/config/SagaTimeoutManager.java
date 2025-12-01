package com.guipalm4.sagapatternspring.config;

import com.guipalm4.sagapatternspring.service.SagaOrchestrator;
import com.guipalm4.sagapatternspring.domain.enums.SagaStatus;
import com.guipalm4.sagapatternspring.domain.SagaTransaction;
import com.guipalm4.sagapatternspring.repository.SagaTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaTimeoutManager {

    private final SagaTransactionRepository sagaRepository;

    private final SagaOrchestrator sagaOrchestrator;

    @Scheduled(fixedDelay = 60000) // Executar a cada 1 minuto
    public void checkForTimeoutSagas() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(5);

        List<SagaTransaction> stuckSagas = sagaRepository.findStuckSagas(
                SagaStatus.IN_PROGRESS, timeoutThreshold);

        for (SagaTransaction saga : stuckSagas) {
            log.warn("Saga com timeout detectada: {} - Iniciando compensação", saga.getSagaId());

            try {
                // Marcar como timeout e iniciar compensação
                saga.setStatus(SagaStatus.FAILED);
                saga.setUpdatedAt(LocalDateTime.now());
                sagaRepository.save(saga);

                // Iniciar compensação
                sagaOrchestrator.compensateSagaTimeout(saga.getSagaId(), saga.getCurrentStep());

            } catch (Exception e) {
                log.error("Erro ao processar timeout da saga: {}", saga.getSagaId(), e);
            }
        }
    }
}