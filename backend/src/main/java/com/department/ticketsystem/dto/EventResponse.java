package com.department.ticketsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String department,
        LocalDateTime dateTime,
        String venue,
        BigDecimal ticketPrice,
        Integer availableTickets,
        Integer totalTickets
) {
}
