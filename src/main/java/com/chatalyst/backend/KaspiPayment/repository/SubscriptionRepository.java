package com.chatalyst.backend.KaspiPayment.repository;

import com.chatalyst.backend.KaspiPayment.Entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByType(String type);
}