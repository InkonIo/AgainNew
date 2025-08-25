package com.chatalyst.backend.Support.Repository;

import com.chatalyst.backend.Support.Entity.MessagePriority;
import com.chatalyst.backend.Support.Entity.MessageStatus;
import com.chatalyst.backend.Support.Entity.SupportMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportMessageRepositoryCustom {
    List<SupportMessage> findWithAdvancedFilters(
            MessageStatus status,
            MessagePriority priority,
            Long adminId,
            String search,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String sortBy,
            String sortDirection
    );
}
