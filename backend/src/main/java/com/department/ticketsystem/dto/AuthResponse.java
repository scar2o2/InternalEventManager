package com.department.ticketsystem.dto;

public record AuthResponse(
        String token,
        String role,
        String name,
        String email
) {
}
