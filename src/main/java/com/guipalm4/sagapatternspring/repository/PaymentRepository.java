package com.guipalm4.sagapatternspring.repository;

import com.guipalm4.sagapatternspring.domain.Payment;
import com.guipalm4.sagapatternspring.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByCustomerId(String customerId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
