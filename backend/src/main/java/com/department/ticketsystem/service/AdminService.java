package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.DashboardResponse;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.EventRepository;
import com.department.ticketsystem.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public AdminService(BookingRepository bookingRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public DashboardResponse getDashboardData() {
        List<Map<String, Object>> bookingsPerEvent = bookingRepository.getBookingTotalsByEvent().stream()
                .map(row -> Map.<String, Object>of("name", row[0], "bookings", row[1]))
                .toList();

        List<Map<String, Object>> ticketDistribution = eventRepository.findAll().stream()
                .map(event -> Map.<String, Object>of(
                        "name", event.getName(),
                        "tickets", event.getTotalTickets() - event.getAvailableTickets()))
                .toList();

        List<Map<String, Object>> remainingVsBooked = eventRepository.findAll().stream()
                .map(event -> Map.<String, Object>of(
                        "name", event.getName(),
                        "booked", event.getTotalTickets() - event.getAvailableTickets(),
                        "remaining", event.getAvailableTickets()))
                .toList();

        List<Map<String, Object>> bookingsOverTime = bookingRepository
                .getBookingsOverTime(LocalDateTime.now().minusDays(30)).stream()
                .map(row -> Map.<String, Object>of("date", row[0].toString(), "tickets", row[1]))
                .toList();

        return new DashboardResponse(
                eventRepository.count(),
                bookingRepository.count(),
                userRepository.count(),
                bookingsPerEvent,
                ticketDistribution,
                remainingVsBooked,
                bookingsOverTime);
    }
}
