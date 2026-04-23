package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.BookingStatsResponse;
import com.department.ticketsystem.dto.DashboardResponse;
import com.department.ticketsystem.dto.RevenuePointResponse;
import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.EventRepository;
import com.department.ticketsystem.repository.SeatRepository;
import com.department.ticketsystem.repository.UserRepository;
import com.department.ticketsystem.model.SeatStatus;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;

    public AdminService(BookingRepository bookingRepository, EventRepository eventRepository, UserRepository userRepository,
                        SeatRepository seatRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
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

    public List<RevenuePointResponse> getRevenueData() {
        return bookingRepository.getRevenueByEvent().stream()
                .map(row -> new RevenuePointResponse(
                        ((Number) row[0]).longValue(),
                        row[1].toString(),
                        (BigDecimal) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();
    }

    public BookingStatsResponse getBookingStats() {
        List<Map<String, Object>> ticketDistribution = eventRepository.findAll().stream()
                .map(event -> Map.<String, Object>of(
                        "name", event.getName(),
                        "tickets", seatRepository.countByEventAndStatus(event, SeatStatus.BOOKED)))
                .toList();

        List<Map<String, Object>> soldVsRemaining = eventRepository.findAll().stream()
                .map(event -> Map.<String, Object>of(
                        "name", event.getName(),
                        "booked", seatRepository.countByEventAndStatus(event, SeatStatus.BOOKED),
                        "remaining", seatRepository.countByEventAndStatus(event, SeatStatus.AVAILABLE)))
                .toList();

        List<Map<String, Object>> bookingsOverTime = bookingRepository
                .getBookingsOverTime(LocalDateTime.now().minusDays(30)).stream()
                .map(row -> Map.<String, Object>of("date", row[0].toString(), "tickets", row[1]))
                .toList();

        return new BookingStatsResponse(ticketDistribution, bookingsOverTime, soldVsRemaining);
    }

    public byte[] exportReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        List<Booking> bookings = bookingRepository.findByEventAndStatusOrderByBookingDateDesc(event, BookingStatus.CONFIRMED);
        BigDecimal revenue = bookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph("Event Report"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Event: " + event.getName()));
            document.add(new Paragraph("Department: " + event.getDepartment()));
            document.add(new Paragraph("Venue: " + event.getVenue()));
            document.add(new Paragraph("Date and Time: " + event.getDateTime()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total bookings: " + bookings.size()));
            document.add(new Paragraph("Tickets sold: " + bookings.stream().mapToInt(Booking::getTickets).sum()));
            document.add(new Paragraph("Revenue summary: Rs. " + revenue));
            document.add(new Paragraph(" "));
            for (Booking booking : bookings) {
                document.add(new Paragraph(
                        booking.getBookingDate() + " | " + booking.getUser().getName() + " | Seats: "
                                + booking.getSeatNumbers() + " | Total: Rs. " + booking.getTotalAmount()));
            }
            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate PDF report", exception);
        }
    }
}
