package com.chatalyst.backend.Support.Repository;

import com.chatalyst.backend.Support.Entity.SupportMessageReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

