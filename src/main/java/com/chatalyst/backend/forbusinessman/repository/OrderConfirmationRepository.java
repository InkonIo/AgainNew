package com.chatalyst.backend.forbusinessman.repository;

import com.chatalyst.backend.forbusinessman.model.OrderConfirmation;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderConfirmationRepository extends JpaRepository<OrderConfirmation, Long> {
    
    /**
     * Найти подтверждение по заказу
     */
    Optional<OrderConfirmation> findByOrder(Order order);
    
    /**
     * Найти все подтверждения для бота
     */
    Page<OrderConfirmation> findByBotOrderByCreatedAtDesc(Bot bot, Pageable pageable);
    
    /**
     * Найти подтверждения по статусу для бота
     */
    Page<OrderConfirmation> findByBotAndStatusOrderByCreatedAtDesc(
        Bot bot, 
        OrderConfirmation.ConfirmationStatus status, 
        Pageable pageable
    );
    
    /**
     * Найти все ожидающие подтверждения для владельца бота
     */
    @Query("""
        SELECT oc FROM OrderConfirmation oc 
        WHERE oc.bot.owner.id = :ownerId 
        AND oc.status = 'PENDING'
        ORDER BY oc.createdAt DESC
    """)
    List<OrderConfirmation> findPendingConfirmationsByOwner(@Param("ownerId") Long ownerId);
    
    /**
     * Подсчитать количество ожидающих подтверждений для владельца
     */
    @Query("""
        SELECT COUNT(oc) FROM OrderConfirmation oc 
        WHERE oc.bot.owner.id = :ownerId 
        AND oc.status = 'PENDING'
    """)
    long countPendingByOwner(@Param("ownerId") Long ownerId);
}