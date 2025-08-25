package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.RoleRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.Repository.ChatMessageRepository;
import com.chatalyst.backend.Repository.OpenAITokenUsageRepository;
import com.chatalyst.backend.Repository.ProductRepository;
import com.chatalyst.backend.dto.JwtResponse;
import com.chatalyst.backend.dto.LoginRequest;
import com.chatalyst.backend.dto.RegisterRequest;
import com.chatalyst.backend.security.jwt.JwtUtils;
import com.chatalyst.backend.model.PasswordResetToken;
import com.chatalyst.backend.Repository.PasswordResetTokenRepository;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.ChatMessage;
import com.chatalyst.backend.model.OpenAITokenUsage;
import com.chatalyst.backend.model.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    
    // Репозитории для удаления связанных данных
    private final BotRepository botRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OpenAITokenUsageRepository openAITokenUsageRepository;
    private final ProductRepository productRepository;

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Определяем основную роль для localStorage
        String primaryRole = roles.contains("ROLE_ADMIN") ? "admin" : "user";

        return new JwtResponse(jwt, userPrincipal.getId(), userPrincipal.getEmail(),
                userPrincipal.getFirstName(), userPrincipal.getLastName(), roles, primaryRole);
    }

    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Email is already taken!");
        }

        // Create new user
        User user = new User(registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getFirstName(),
                registerRequest.getLastName());

        // Set default role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);

        user.setRoles(roles);
        userRepository.save(user);

        log.info("User registered successfully: {}", registerRequest.getEmail());
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Handles the password reset request.
     * Generates a 6-digit code and sends it via email to the user.
     * @param email The email of the user requesting a password reset.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: User with this email not found!"));

        // Удаляем все существующие токены сброса пароля для этого пользователя
        passwordResetTokenRepository.deleteByUser(user);

        // Создаем новый токен (6-значный код)
        PasswordResetToken token = new PasswordResetToken(user);
        passwordResetTokenRepository.save(token);

        // Отправляем email с кодом
        emailService.sendPasswordResetCode(user, token.getToken());

        log.info("Password reset code generated and email sent for user: {}", email);
    }

    /**
     * Resets the user's password using a valid 6-digit code.
     * @param code The 6-digit password reset code.
     * @param newPassword The new password for the user.
     * @throws RuntimeException if the code is invalid or expired, or user not found.
     */
    @Transactional
    public void resetPassword(String code, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(code) // Ищем по коду
                .orElseThrow(() -> new RuntimeException("Error: Invalid or expired password reset code!"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken); // Удаляем просроченный токен
            throw new RuntimeException("Error: Password reset code has expired!");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw new RuntimeException("Error: User associated with this code not found!");
        }

        // Обновляем пароль пользователя
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Удаляем токен после использования
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    /**
     * Changes the password for an authenticated user.
     * @param userEmail The email of the authenticated user.
     * @param currentPassword The current password for verification.
     * @param newPassword The new password.
     * @throws RuntimeException if the current password is incorrect or user not found.
     */
    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Error: User not found!"));

        // Проверяем текущий пароль
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Проверяем, что новый пароль отличается от текущего
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Обновляем пароль
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userEmail);
    }

    /**
     * Updates the user's profile information (currently only email).
     * @param currentEmail The current email of the user.
     * @param newEmail The new email address.
     * @throws RuntimeException if the user is not found or new email is already taken.
     */
    @Transactional
    public void updateUserProfile(String currentEmail, String newEmail) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Error: User not found!"));

        // Если email не изменился, ничего не делаем
        if (currentEmail.equals(newEmail)) {
            return;
        }

        // Проверяем, не занят ли новый email
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new RuntimeException("Email is already taken");
        }

        // Обновляем email
        user.setEmail(newEmail);
        userRepository.save(user);

        log.info("Profile updated successfully for user: {} -> {}", currentEmail, newEmail);
    }

    /**
     * Deletes the user account and all associated data.
     * @param userEmail The email of the user to delete.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public void deleteUserAccount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Error: User not found!"));

        try {
            // Получаем все боты пользователя
            List<Bot> userBots = botRepository.findByOwner(user);
            log.debug("Found {} bots for user: {}", userBots.size(), userEmail);

            // Удаляем связанные данные для каждого бота
            for (Bot bot : userBots) {
                String botIdentifier = bot.getBotIdentifier();
                
                // 1. Удаляем сообщения чата для этого бота
                List<ChatMessage> chatMessages = chatMessageRepository.findTop30ByChatIdAndBotIdentifierOrderByIdDesc(null, botIdentifier);
                // Поскольку нет метода для удаления по botIdentifier, удаляем через findAll и фильтрацию
                List<ChatMessage> allMessages = chatMessageRepository.findAll();
                List<ChatMessage> botMessages = allMessages.stream()
                    .filter(msg -> botIdentifier.equals(msg.getBotIdentifier()))
                    .collect(Collectors.toList());
                chatMessageRepository.deleteAll(botMessages);
                log.debug("Deleted {} chat messages for bot: {}", botMessages.size(), botIdentifier);

                // 2. Удаляем статистику использования токенов для этого бота
                List<OpenAITokenUsage> tokenUsages = openAITokenUsageRepository.findByBotIdentifier(botIdentifier);
                openAITokenUsageRepository.deleteAll(tokenUsages);
                log.debug("Deleted {} token usage records for bot: {}", tokenUsages.size(), botIdentifier);

                // 3. Удаляем продукты для этого бота
                List<Product> products = productRepository.findByBot(bot);
                productRepository.deleteAll(products);
                log.debug("Deleted {} products for bot: {}", products.size(), bot.getId());
            }

            // 4. Удаляем всех ботов пользователя
            botRepository.deleteAll(userBots);
            log.debug("Deleted {} bots for user: {}", userBots.size(), userEmail);

            // 5. Удаляем токены сброса пароля
            passwordResetTokenRepository.deleteByUser(user);
            log.debug("Deleted password reset tokens for user: {}", userEmail);

            // 6. Наконец, удаляем самого пользователя
            userRepository.delete(user);
            
            log.info("Account and all associated data deleted successfully for user: {}", userEmail);
            
        } catch (Exception e) {
            log.error("Error deleting account for user {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Error deleting account: " + e.getMessage());
        }
    }
}

