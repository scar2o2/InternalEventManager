package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.BookingRequest;
import com.department.ticketsystem.dto.BookingResponse;
import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.BookingStatus;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.NotificationType;
import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.model.Seat;
import com.department.ticketsystem.model.SeatStatus;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.model.WaitlistEntry;
import com.department.ticketsystem.model.WaitlistStatus;
import com.department.ticketsystem.repository.BookingRepository;
import com.department.ticketsystem.repository.SeatRepository;
import com.department.ticketsystem.repository.UserRepository;
import com.department.ticketsystem.repository.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private static final int HOLD_MINUTES = 10;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final SeatRepository seatRepository;
    private final WaitlistRepository waitlistRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, EventService eventService,
                          SeatRepository seatRepository, WaitlistRepository waitlistRepository,
                          PricingService pricingService, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.seatRepository = seatRepository;
        this.waitlistRepository = waitlistRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {
        User user = getUser(email);
        Event event = eventService.getEventEntity(request.getEventId());
        eventService.ensureSeatsForEvent(event);
        releaseExpiredHolds(event);

        int ticketCount = resolveTicketCount(request);
        if (ticketCount <= 0) {
            throw new IllegalArgumentException("Please select at least one seat");
        }

        List<Seat> chosenSeats = resolveSeatsForBooking(event, request.getSeatIds(), ticketCount, user);
        if (chosenSeats.size() != ticketCount) {
            throw new IllegalArgumentException("Unable to reserve the requested number of seats");
        }

        return finalizeBooking(event, user, chosenSeats, null, NotificationType.BOOKING_CONFIRMATION,
                "Booking confirmed for " + event.getName() + ".");
    }

    public List<BookingResponse> getUserBookings(String email) {
        User user = getUser(email);
        return bookingRepository.findByUserOrderByBookingDateDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public Map<String, Object> holdSeats(String email, Long eventId, List<Long> seatIds) {
        User user = getUser(email);
        Event event = eventService.getEventEntity(eventId);
        eventService.ensureSeatsForEvent(event);
        releaseExpiredHolds(event);

        List<Seat> seats = seatRepository.findByEventAndIdIn(event, seatIds).stream()
                .sorted(Comparator.comparing(Seat::getSeatNumber))
                .toList();
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more selected seats do not exist");
        }

        for (Seat seat : seats) {
            boolean heldByCurrentUser = seat.getStatus() == SeatStatus.HELD
                    && seat.getHeldBy() != null
                    && Objects.equals(seat.getHeldBy().getId(), user.getId());
            if (seat.getStatus() == SeatStatus.BOOKED || (seat.getStatus() == SeatStatus.HELD && !heldByCurrentUser)) {
                throw new IllegalArgumentException("Some selected seats are no longer available");
            }
        }

        LocalDateTime heldAt = LocalDateTime.now();
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldBy(user);
            seat.setHeldAt(heldAt);
        });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(event);

        int heldSeats = (int) seatRepository.countByEventAndStatus(event, SeatStatus.HELD);
        BigDecimal pricePerTicket = pricingService.calculateCurrentPrice(event, event.getAvailableTickets() + heldSeats);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("eventId", event.getId());
        response.put("seatNumbers", seats.stream().map(Seat::getSeatNumber).toList());
        response.put("pricePerTicket", pricePerTicket);
        response.put("totalAmount", pricePerTicket.multiply(BigDecimal.valueOf(seats.size())));
        response.put("holdExpiresAt", heldAt.plusMinutes(HOLD_MINUTES));
        return response;
    }

    @Transactional
    public BookingResponse cancelBooking(String email, Long bookingId) {
        User user = getUser(email);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Booking does not belong to the current user");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Seat> seats = seatRepository.findByEventOrderBySeatNumberAsc(booking.getEvent()).stream()
                .filter(seat -> seat.getBooking() != null && seat.getBooking().getId().equals(booking.getId()))
                .toList();
        seats.forEach(seat -> {
            seat.setBooking(null);
            seat.setHeldBy(null);
            seat.setHeldAt(null);
            seat.setStatus(SeatStatus.AVAILABLE);
        });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(booking.getEvent());

        notificationService.createNotification(user, NotificationType.TICKET_CANCELLATION,
                "Your booking for " + booking.getEvent().getName() + " has been cancelled.");
        processWaitlist(booking.getEvent());
        return toResponse(booking);
    }

    @Transactional
    public String joinWaitlist(String email, Long eventId, Integer tickets) {
        User user = getUser(email);
        Event event = eventService.getEventEntity(eventId);
        if (tickets == null || tickets <= 0) {
            throw new IllegalArgumentException("Tickets must be greater than 0");
        }
        waitlistRepository.findByEventAndUserAndStatus(event, user, WaitlistStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("You are already on the waitlist for this event");
                });
        WaitlistEntry entry = new WaitlistEntry();
        entry.setEvent(event);
        entry.setUser(user);
        entry.setRequestedTickets(tickets);
        entry.setStatus(WaitlistStatus.PENDING);
        entry.setCreatedAt(LocalDateTime.now());
        waitlistRepository.save(entry);

        notificationService.createNotification(user, NotificationType.EVENT_UPDATE,
                "You joined the waitlist for " + event.getName() + ".");
        long position = waitlistRepository.findByEventAndStatusOrderByCreatedAtAsc(event, WaitlistStatus.PENDING).stream()
                .map(WaitlistEntry::getId)
                .toList()
                .indexOf(entry.getId()) + 1L;
        return "Added to waitlist. Current queue position: " + position;
    }

    @Transactional
    public void releaseExpiredHolds(Event event) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(HOLD_MINUTES);
        List<Seat> expiredSeats = seatRepository.findByStatusAndHeldAtBefore(SeatStatus.HELD, threshold).stream()
                .filter(seat -> seat.getEvent().getId().equals(event.getId()))
                .toList();
        if (expiredSeats.isEmpty()) {
            return;
        }
        expiredSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldBy(null);
            seat.setHeldAt(null);
        });
        seatRepository.saveAll(expiredSeats);
        eventService.refreshAvailableTickets(event);
    }

    private User getUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin cannot book tickets");
        }
        return user;
    }

    private int resolveTicketCount(BookingRequest request) {
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            return request.getSeatIds().size();
        }
        return request.getTickets() == null ? 0 : request.getTickets();
    }

    private List<Seat> resolveSeatsForBooking(Event event, List<Long> requestedSeatIds, int ticketCount, User user) {
        if (requestedSeatIds != null && !requestedSeatIds.isEmpty()) {
            List<Seat> chosenSeats = seatRepository.findByEventAndIdIn(event, requestedSeatIds).stream()
                    .sorted(Comparator.comparing(Seat::getSeatNumber))
                    .toList();
            for (Seat seat : chosenSeats) {
                boolean availableNow = seat.getStatus() == SeatStatus.AVAILABLE;
                boolean heldByCurrentUser = seat.getStatus() == SeatStatus.HELD
                        && seat.getHeldBy() != null
                        && Objects.equals(seat.getHeldBy().getId(), user.getId());
                if (!availableNow && !heldByCurrentUser) {
                    throw new IllegalArgumentException("Some selected seats are no longer available");
                }
            }
            return chosenSeats;
        }

        return seatRepository.findByEventAndStatusOrderBySeatNumberAsc(event, SeatStatus.AVAILABLE).stream()
                .limit(ticketCount)
                .toList();
    }

    private BookingResponse finalizeBooking(Event event, User user, List<Seat> seats, WaitlistEntry waitlistEntry,
                                            NotificationType notificationType, String notificationMessage) {
        int heldSeats = (int) seatRepository.countByEventAndStatus(event, SeatStatus.HELD);
        BigDecimal pricePerTicket = pricingService.calculateCurrentPrice(event, event.getAvailableTickets() + heldSeats);
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setEvent(event);
        booking.setTickets(seats.size());
        booking.setPricePerTicket(pricePerTicket);
        booking.setTotalAmount(pricePerTicket.multiply(BigDecimal.valueOf(seats.size())));
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSeatNumbers(String.join(", ", seats.stream().map(Seat::getSeatNumber).toList()));
        booking = bookingRepository.save(booking);

        Booking persistedBooking = booking;
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldBy(null);
            seat.setHeldAt(null);
            seat.setBooking(persistedBooking);
        });
        seatRepository.saveAll(seats);
        eventService.refreshAvailableTickets(event);

        if (waitlistEntry != null) {
            waitlistEntry.setStatus(WaitlistStatus.FULFILLED);
            waitlistRepository.save(waitlistEntry);
            notificationService.createNotification(user, NotificationType.WAITLIST_PROMOTION,
                    "A waitlisted ticket request for " + event.getName() + " has been promoted.");
        }

        notificationService.createNotification(user, notificationType, notificationMessage);
        return toResponse(booking);
    }

    private void processWaitlist(Event event) {
        releaseExpiredHolds(event);
        List<WaitlistEntry> queue = waitlistRepository.findByEventAndStatusOrderByCreatedAtAsc(event, WaitlistStatus.PENDING);
        for (WaitlistEntry entry : queue) {
            List<Seat> availableSeats = seatRepository.findByEventAndStatusOrderBySeatNumberAsc(event, SeatStatus.AVAILABLE);
            if (availableSeats.size() < entry.getRequestedTickets()) {
                break;
            }
            entry.setStatus(WaitlistStatus.PROMOTED);
            waitlistRepository.save(entry);
            finalizeBooking(event, entry.getUser(), availableSeats.stream().limit(entry.getRequestedTickets()).toList(),
                    entry, NotificationType.BOOKING_CONFIRMATION,
                    "Booking confirmed from waitlist for " + event.getName() + ".");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        List<String> seatNumbers = new ArrayList<>();
        if (booking.getSeatNumbers() != null && !booking.getSeatNumbers().isBlank()) {
            for (String seatNumber : booking.getSeatNumbers().split(", ")) {
                seatNumbers.add(seatNumber);
            }
        }
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getName(),
                booking.getTickets(),
                booking.getPricePerTicket(),
                booking.getTotalAmount(),
                booking.getBookingDate(),
                booking.getStatus().name(),
                seatNumbers);
    }
}
