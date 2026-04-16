package com.department.ticketsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long eventId,
        @NotNull @Min(1) Integer tickets
) {
}
