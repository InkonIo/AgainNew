package com.chatalyst.backend.Support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.chatalyst.backend.Support.Entity.SupportMessage;
import com.chatalyst.backend.Support.Entity.SupportMessageReply;

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
    private Double averageResponseTimeHours;
    private Map<String, Long> messagesByDate;
    private Long recentMessages;
    private Long recentReplies;

}
