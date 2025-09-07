package com.chatalyst.backend.Repository;

import com.chatalyst.backend.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Basic methods ---
    Optional<User> findByEmail(String email);

    // --- Message consumption (business logic) ---
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


    // --- Extended search methods ---
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email, String firstName, String lastName, Pageable pageable);

    // --- Count methods ---
    long countByEnabledTrue();
    long countByEnabledFalse();

    long countByCreatedAtAfter(LocalDateTime date);

    // --- Find by role ---
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    // --- Active/inactive users ---
    Page<User> findByEnabledTrue(Pageable pageable);
    Page<User> findByEnabledFalse(Pageable pageable);

    // --- Users created after date ---
    Page<User> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

    // --- Find users by email domain ---
    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain%")
    Page<User> findByEmailDomain(@Param("domain") String domain, Pageable pageable);
}
