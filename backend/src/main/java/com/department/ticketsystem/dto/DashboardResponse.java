package com.department.ticketsystem.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalEvents,
        long totalBookings,
        long totalUsers,
        List<Map<String, Object>> bookingsPerEvent,
        List<Map<String, Object>> ticketDistribution,
        List<Map<String, Object>> remainingVsBooked,
        List<Map<String, Object>> bookingsOverTime
) {
}
