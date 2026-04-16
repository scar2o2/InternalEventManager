package com.department.ticketsystem.controller;

import com.department.ticketsystem.dto.BookingRequest;
import com.department.ticketsystem.dto.BookingResponse;
import com.department.ticketsystem.service.BookingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody BookingRequest request, Principal principal) {
        return bookingService.createBooking(principal.getName(), request);
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(Principal principal) {
        return bookingService.getUserBookings(principal.getName());
    }
}
