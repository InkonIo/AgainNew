package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender; // Инжектируем JavaMailSender

    public void sendPasswordResetCode(User user, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("VerifPharmacy@gmail.com"); // Убедитесь, что это ваш email из application.properties
            message.setTo(user.getEmail());
            message.setSubject("Chatalyst: Ваш код для сброса пароля");
            message.setText(
                "Привет, " + user.getFirstName() + "!\n\n" +
                "Вы запросили сброс пароля. Ваш 6-значный код для сброса пароля:\n\n" +
                "КОД: " + code + "\n\n" +
                "Этот код действителен в течение 10 минут.\n" +
                "Если вы не запрашивали сброс пароля, проигнорируйте это письмо.\n\n" +
                "С уважением,\nКоманда Chatalyst"
            );
            mailSender.send(message);
            log.info("Password reset code email sent to: {}", user.getEmail());
        } catch (MailException e) {
            log.error("Failed to send password reset code email to {}: {}", user.getEmail(), e.getMessage());
            // В реальном приложении здесь можно добавить более сложную обработку ошибок,
            // например, бросить исключение или записать в базу данных.
            throw new RuntimeException("Failed to send email. Please try again later.");
        }
    }
}
