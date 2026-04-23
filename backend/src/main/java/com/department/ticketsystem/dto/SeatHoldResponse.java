package com.department.ticketsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SeatHoldResponse(
        Long eventId,
        List<String> seatNumbers,
        BigDecimal pricePerTicket,
        BigDecimal totalAmount,
        LocalDateTime holdExpiresAt
) {
}
