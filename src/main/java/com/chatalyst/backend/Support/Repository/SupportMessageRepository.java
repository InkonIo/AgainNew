package com.chatalyst.backend.Support.Repository;

import com.chatalyst.backend.Support.Entity.MessagePriority;
import com.chatalyst.backend.Support.Entity.MessageStatus;
import com.chatalyst.backend.Support.Entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long>, SupportMessageRepositoryCustom {

    List<SupportMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SupportMessage> findAllByOrderByCreatedAtDesc();

    Long countByStatus(MessageStatus status);
    Long countByPriority(MessagePriority priority);
    Long countByAdminIsNull();

    @Query("SELECT COUNT(m) FROM SupportMessage m WHERE DATE(m.createdAt) = :date")
    Long countByCreatedAtDate(LocalDate date);

    // Подсчет сообщений в диапазоне дат
    @Query("SELECT COUNT(m) FROM SupportMessage m WHERE m.createdAt BETWEEN :from AND :to")
    Long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Подсчет сообщений после определенной даты
    @Query("SELECT COUNT(m) FROM SupportMessage m WHERE m.createdAt >= :date")
    Long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    // Подсчет неназначенных сообщений в диапазоне дат
    @Query("SELECT COUNT(m) FROM SupportMessage m WHERE m.admin IS NULL AND m.createdAt BETWEEN :from AND :to")
    Long countByAdminIsNullAndCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Статистика по статусу и приоритету
    @Query("""
        SELECT m.status AS status, m.priority AS priority, COUNT(m) AS count
        FROM SupportMessage m
        GROUP BY m.status, m.priority
    """)
    List<StatusPriorityCount> countByStatusAndPriority();

    // Статистика по статусу и приоритету с диапазоном дат
    @Query("""
        SELECT m.status AS status, m.priority AS priority, COUNT(m) AS count
        FROM SupportMessage m
        WHERE m.createdAt BETWEEN :from AND :to
        GROUP BY m.status, m.priority
    """)
    List<StatusPriorityCount> countByStatusAndPriorityWithDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Статистика по дням, начиная с определенной даты
    @Query("""
        SELECT CAST(m.createdAt AS LocalDate) AS date, COUNT(m) AS count
        FROM SupportMessage m
        WHERE m.createdAt >= :date
        GROUP BY CAST(m.createdAt AS LocalDate)
    """)
    List<MessagesByDay> countByCreatedAtSince(@Param("date") LocalDateTime date);

    interface StatusPriorityCount {
        MessageStatus getStatus();
        MessagePriority getPriority();
        Long getCount();
    }

    interface MessagesByDay {
        LocalDate getDate();
        Long getCount();
    }
}

