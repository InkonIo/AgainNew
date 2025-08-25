package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Random; // Для генерации 6-значного кода

import com.chatalyst.backend.Entity.User;

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    private static final int EXPIRATION_TIME_MINUTES = 10; // Токен действует 10 минут для кода
    private static final int CODE_LENGTH = 6; // Длина 6-значного кода

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = CODE_LENGTH) // Устанавливаем длину для кода
    private String token; // Теперь это будет 6-значный код

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(User user) {
        this.user = user;
        this.token = generateSixDigitCode(); // Генерируем 6-значный код
        this.expiryDate = calculateExpiryDate();
    }

    private String generateSixDigitCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Генерируем число от 100000 до 999999
        return String.valueOf(code);
    }

    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusMinutes(EXPIRATION_TIME_MINUTES);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
