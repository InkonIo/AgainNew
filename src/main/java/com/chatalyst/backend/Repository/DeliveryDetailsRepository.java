package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.DeliveryDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryDetailsRepository extends JpaRepository<DeliveryDetails, Long> {

    // Найти детали доставки по боту
    Optional<DeliveryDetails> findByBot(Bot bot);
}
