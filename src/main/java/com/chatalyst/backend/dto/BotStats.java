package com.chatalyst.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BotStats {
    private long totalMessages;
    private long totalDialogues;
}