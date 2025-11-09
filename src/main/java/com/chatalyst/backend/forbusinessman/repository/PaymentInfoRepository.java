package com.chatalyst.backend.forbusinessman.repository;

import com.chatalyst.backend.forbusinessman.model.PaymentInfo;
import com.chatalyst.backend.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {
    
    /**
     * Найти информацию об оплате для конкретного бота
     */
    Optional<PaymentInfo> findByBot(Bot bot);
    
    /**
     * Найти информацию об оплате по botId
     */
    @Query("SELECT p FROM PaymentInfo p WHERE p.bot.id = :botId")
    Optional<PaymentInfo> findByBotId(@Param("botId") Long botId);
}