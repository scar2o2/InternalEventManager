package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.BookingRequest;
import com.department.ticketsystem.dto.BookingResponse;
import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, EventService eventService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin cannot book tickets");
        }

        Event event = eventService.getEventEntity(request.eventId());
        if (request.tickets() <= 0) {
            throw new IllegalArgumentException("Tickets must be greater than 0");
        }
        if (request.tickets() > event.getAvailableTickets()) {
            throw new IllegalArgumentException("Requested tickets exceed availability");
        }

        event.setAvailableTickets(event.getAvailableTickets() - request.tickets());
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setTickets(request.tickets());
        booking.setTotalAmount(event.getTicketPrice().multiply(BigDecimal.valueOf(request.tickets())));
        booking.setBookingDate(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        return toResponse(booking);
    }

    public List<BookingResponse> getUserBookings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return bookingRepository.findByUserOrderByBookingDateDesc(user).stream().map(this::toResponse).toList();
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getName(),
                booking.getTickets(),
                booking.getTotalAmount(),
                booking.getBookingDate());
    }
}
