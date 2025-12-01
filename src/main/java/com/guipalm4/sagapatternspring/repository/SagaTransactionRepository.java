package com.guipalm4.sagapatternspring.repository;

import com.guipalm4.sagapatternspring.domain.enums.SagaStatus;
import com.guipalm4.sagapatternspring.domain.SagaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaTransactionRepository extends JpaRepository<SagaTransaction, String> {

    List<SagaTransaction> findByStatus(SagaStatus status);

    List<SagaTransaction> findByOrderId(Long orderId);

    @Query("SELECT s FROM SagaTransaction s WHERE s.status = :status AND s.createdAt < :before")
    List<SagaTransaction> findStuckSagas(@Param("status") SagaStatus status,
                                         @Param("before") LocalDateTime before);

    @Query("SELECT s FROM SagaTransaction s WHERE s.status IN :statuses ORDER BY s.createdAt DESC")
    List<SagaTransaction> findByStatusIn(@Param("statuses") List<SagaStatus> statuses);

    Optional<SagaTransaction> findByOrderIdAndStatus(Long orderId, SagaStatus status);
}