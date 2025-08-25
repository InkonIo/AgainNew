package com.chatalyst.backend.Repository;

import com.chatalyst.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Modifying
    @Query("""
        UPDATE User u
           SET u.monthlyMessagesUsed = u.monthlyMessagesUsed + :units
         WHERE u.id = :userId
           AND (u.subscriptionEnd IS NULL OR u.subscriptionEnd > CURRENT_TIMESTAMP)
           AND u.monthlyMessagesUsed + :units <= u.monthlyMessagesLimit
      """)
    int tryConsumeMessages(@Param("userId") Long userId, @Param("units") int units);

    @Query("""
        SELECT COUNT(b) FROM Bot b 
        WHERE b.owner.id = :userId
      """)
    long countBotsByOwner(@Param("userId") Long userId);
}
