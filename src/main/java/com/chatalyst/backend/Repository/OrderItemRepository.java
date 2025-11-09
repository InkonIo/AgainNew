package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.Order;
import com.chatalyst.backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Найти все элементы заказа по объекту заказа
    List<OrderItem> findByOrder(Order order);
}
