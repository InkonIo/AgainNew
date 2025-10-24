package com.chatalyst.backend.config;

import com.chatalyst.backend.security.jwt.AuthEntryPointJwt;
import com.chatalyst.backend.security.jwt.AuthTokenFilter;
import com.chatalyst.backend.security.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final AuthTokenFilter authTokenFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // Публичные эндпоинты (не требуют авторизации)
                .requestMatchers(
                    "/api/auth/signin",
                    "/api/auth/signup",
                    "/api/auth/register",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/webhook/telegram",
                    "/telegram/webhook/**",
                    "/api/telegram/webhook/**",
                    "/api/token-usage/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/h2-console/**",
                    "/error",
                    "/favicon.ico",
                    "/"
                ).permitAll()

                // Админские эндпоинты (только ADMIN)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/support/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/support/messages").hasRole("ADMIN")
                .requestMatchers("/api/support/messages/*/status").hasRole("ADMIN")
                .requestMatchers("/api/support/messages/*/assign").hasRole("ADMIN")
                .requestMatchers("/api/support/messages/*/replies").hasRole("ADMIN")
                .requestMatchers("/api/support/replies/**").hasRole("ADMIN")

                // Эндпоинты для всех авторизованных пользователей
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/update-profile").authenticated()
                .requestMatchers("/api/auth/delete-account").authenticated()
                .requestMatchers("/api/auth/refresh-token").authenticated()
                .requestMatchers("/api/bots/**").authenticated()
                .requestMatchers("/api/products/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers("/api/support/messages/my").authenticated()
                .requestMatchers("/api/support/messages/{id}").authenticated()
                .requestMatchers("/api/user/subscription-details").authenticated()
                .requestMatchers("/api/user/subscription").authenticated()

                // Остальные эндпоинты требуют аутентификации
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider());

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}