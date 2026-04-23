package com.department.ticketsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record HoldSeatsRequest(
        @NotNull Long eventId,
        @NotEmpty List<Long> seatIds
) {
}
