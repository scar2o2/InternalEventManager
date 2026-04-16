package com.department.ticketsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long eventId,
        String eventName,
        Integer tickets,
        BigDecimal totalAmount,
        LocalDateTime bookingDate
) {
}
