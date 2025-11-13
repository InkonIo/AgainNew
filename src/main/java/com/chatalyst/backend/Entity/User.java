package com.chatalyst.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(nullable = true, length = 255)
    private String password;
    
    @Column(nullable = false, length = 50)
    private String firstName;
    
    @Column(nullable = false, length = 50)
    private String lastName;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public User(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // ====== Тариф и лимиты ======
@Column(nullable = false, columnDefinition = "integer default 0")
private Integer monthlyMessagesLimit = 0; // Лимит сообщений в месяц

@Column(nullable = false, columnDefinition = "integer default 0")
private Integer monthlyMessagesUsed = 0; // Использовано сообщений

@Column(nullable = false, columnDefinition = "integer default 0")
private Integer botsAllowed = 0; // Сколько ботов можно создать

@Column(nullable = false, length = 50, columnDefinition = "varchar(50) default 'USER'")
private String supportLevel = "USER"; // USER, BASIC, PRIORITY

@Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
private LocalDateTime subscriptionStart; // Дата начала тарифа

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
private LocalDateTime subscriptionEnd; // Дата окончания тарифа

	@Column(nullable = false, columnDefinition = "boolean default false")
	private Boolean isTrialCompleted = false; // Завершил ли пользователь пробный месяц (для скидки)
// ============================

@Column(nullable = false)
private Boolean active = true;


}