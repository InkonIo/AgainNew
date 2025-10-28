package com.chatalyst.backend.Support.Repository;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Support.Entity.SupportMessageReply;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupportMessageReplyRepository extends JpaRepository<SupportMessageReply, Long> {
    
    List<SupportMessageReply> findByMessageIdOrderByCreatedAtAsc(Long messageId);
    
    @Modifying
    @Query("DELETE FROM SupportMessageReply r WHERE r.message.id = :messageId")
    void deleteByMessageId(@Param("messageId") Long messageId);
    
    Long countByMessageId(Long messageId);
    
    @Query("SELECT r FROM SupportMessageReply r WHERE r.message.id = :messageId ORDER BY r.createdAt ASC")
    List<SupportMessageReply> findRepliesByMessageId(@Param("messageId") Long messageId);
    
    // Подсчет ответов после определенной даты
    @Query("SELECT COUNT(r) FROM SupportMessageReply r WHERE r.createdAt >= :date")
    Long countByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    // Средний время ответа в часах
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(HOUR, m.createdAt, r.createdAt))
        FROM SupportMessageReply r
        JOIN r.message m
        WHERE r.isAdminReply = true
        AND r.createdAt >= :since
    """)
    Double findAverageResponseTimeInHours(@Param("since") LocalDateTime since);
    
    // Средний время ответа в часах (без параметра даты)
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(HOUR, m.createdAt, r.createdAt))
        FROM SupportMessageReply r
        JOIN r.message m
        WHERE r.isAdminReply = true
    """)
    Double findAverageResponseTimeInHours();

    // НОВЫЙ МЕТОД: Удаление всех ответов на сообщения поддержки, связанных с пользователем
    @Modifying
    @Transactional
    @Query("DELETE FROM SupportMessageReply r WHERE r.message.user = :user")
    void deleteByUser(@Param("user") User user);
}
