package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Random; // Для генерации 6-значного кода

import com.chatalyst.backend.Entity.User;

@Entity
@Table(
    name = "password_reset_tokens",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id") // уникальность именно по пользователю
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    private static final int EXPIRATION_TIME_MINUTES = 10;
    private static final int CODE_LENGTH = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = CODE_LENGTH) // убираем unique=true
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(User user) {
        this.user = user;
        this.token = generateSixDigitCode();
        this.expiryDate = calculateExpiryDate();
    }

    private String generateSixDigitCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusMinutes(EXPIRATION_TIME_MINUTES);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}

