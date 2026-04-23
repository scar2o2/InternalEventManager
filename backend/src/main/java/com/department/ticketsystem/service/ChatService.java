package com.department.ticketsystem.service;

import com.department.ticketsystem.model.Event;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final List<String> ALLOWED_KEYWORDS = List.of(
            "event", "events", "ticket", "tickets", "available", "timing", "time", "date", "venue", "book");

    private final EventService eventService;

    public ChatService(EventService eventService) {
        this.eventService = eventService;
    }

    public String answer(String message) {
        String normalized = message.toLowerCase(Locale.ENGLISH);
        boolean eventRelated = ALLOWED_KEYWORDS.stream().anyMatch(normalized::contains);
        if (!eventRelated) {
            return "I can only answer event-related questions about schedules, venues, and ticket availability.";
        }

        List<Event> events = eventService.getEventEntities();
        if (normalized.contains("available") || normalized.contains("tickets left")) {
            return events.stream()
                    .map(event -> event.getName() + ": " + event.getAvailableTickets() + " seats left")
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("No events are available right now.");
        }
        if (normalized.contains("timing") || normalized.contains("time") || normalized.contains("date")) {
            return events.stream()
                    .map(event -> event.getName() + " starts at " + event.getDateTime())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("No events are scheduled right now.");
        }
        if (normalized.contains("venue")) {
            return events.stream()
                    .map(event -> event.getName() + " is at " + event.getVenue())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("No event venues are available right now.");
        }
        return events.stream()
                .map(event -> event.getName() + " (" + event.getDepartment() + ")")
                .reduce((left, right) -> left + ", " + right)
                .map(summary -> "Available events: " + summary)
                .orElse("No events are available right now.");
    }
}
