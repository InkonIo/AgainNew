package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAITokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String botIdentifier;
    private Long chatId;
    
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    
    private Double usdCost;
    private Double kztCost;
    
    private LocalDateTime timestamp;
}
