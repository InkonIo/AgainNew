package com.chatalyst.backend.Support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportStatsResponse {
    private Long totalMessages;
    private Long openMessages;
    private Long inProgressMessages;
    private Long closedMessages;
    private Long highPriorityMessages;
    private Long mediumPriorityMessages;
    private Long lowPriorityMessages;
    private Map<String, Long> messagesByAdmin;
    private Long unassignedMessages;
    private Map<LocalDate, Long> last7DaysStats;
}
