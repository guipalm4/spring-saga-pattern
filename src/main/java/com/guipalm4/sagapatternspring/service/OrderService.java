package com.guipalm4.sagapatternspring.service;


import com.guipalm4.sagapatternspring.messaging.events.OrderEvent;
import com.guipalm4.sagapatternspring.domain.Order;
import com.guipalm4.sagapatternspring.domain.enums.OrderStatus;
import com.guipalm4.sagapatternspring.repository.OrderRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final SqsTemplate sqsTemplate;

    public OrderService(OrderRepository orderRepository, SqsTemplate sqsTemplate) {
        this.orderRepository = orderRepository;
        this.sqsTemplate = sqsTemplate;
    }

    @Transactional
    public Order createOrder(Order order) {
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Publicar evento de criação de pedido
        publishOrderEvent(savedOrder, "ORDER_CREATED");

        log.info("Pedido criado: {}", savedOrder.getId());
        return savedOrder;
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        publishOrderEvent(updatedOrder, "ORDER_CONFIRMED");

        log.info("Pedido confirmado: {}", orderId);
        return updatedOrder;
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        publishOrderEvent(updatedOrder, "ORDER_CANCELLED");

        log.info("Pedido cancelado: {}", orderId);
        return updatedOrder;
    }

    @Transactional
    public Order shipOrder(Long orderId) {
        Order order = findById(orderId);
        order.setStatus(OrderStatus.SHIPPED);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        publishOrderEvent(updatedOrder, "ORDER_SHIPPED");

        log.info("Pedido enviado: {}", orderId);
        return updatedOrder;
    }

    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + orderId));
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> findByCustomerId(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    private void publishOrderEvent(Order order, String eventType) {
        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .orderStatus(order.getStatus().name())
                .eventTime(LocalDateTime.now())
                .source("OrderService")
                .build();

        try {
            sqsTemplate.send("order-events-queue", event);
            log.debug("Evento de pedido publicado: {} para pedido: {}", eventType, order.getId());
        } catch (Exception e) {
            log.error("Erro ao publicar evento de pedido: {}", eventType, e);
        }
    }
}
