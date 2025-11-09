package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Найти все заказы для конкретного бота
    Page<Order> findByBotOrderByCreatedAtDesc(Bot bot, Pageable pageable);

    // Найти все заказы для конкретного пользователя (клиента)
    List<Order> findByUserId(Long userId);

    // Найти заказы по статусу для конкретного бота
    Page<Order> findByBotAndStatusOrderByCreatedAtDesc(Bot bot, Order.OrderStatus status, Pageable pageable);
}
