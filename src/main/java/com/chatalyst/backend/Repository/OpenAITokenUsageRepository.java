package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.OpenAITokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OpenAITokenUsageRepository extends JpaRepository<OpenAITokenUsage, Long> {
    List<OpenAITokenUsage> findByBotIdentifier(String botIdentifier);
}