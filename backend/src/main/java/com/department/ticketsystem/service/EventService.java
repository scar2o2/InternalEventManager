package com.department.ticketsystem.service;

import com.department.ticketsystem.dto.EventRequest;
import com.department.ticketsystem.dto.EventResponse;
import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.repository.EventRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EventResponse getEvent(Long id) {
        return toResponse(getEventEntity(id));
    }

    public EventResponse createEvent(EventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setDepartment(request.department());
        event.setDateTime(request.dateTime());
        event.setVenue(request.venue());
        event.setTicketPrice(request.ticketPrice());
        event.setAvailableTickets(request.availableTickets());
        event.setTotalTickets(request.availableTickets());
        return toResponse(eventRepository.save(event));
    }

    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = getEventEntity(id);
        int bookedTickets = event.getTotalTickets() - event.getAvailableTickets();
        if (request.availableTickets() < bookedTickets) {
            throw new IllegalArgumentException("Capacity cannot be less than booked tickets");
        }
        event.setName(request.name());
        event.setDepartment(request.department());
        event.setDateTime(request.dateTime());
        event.setVenue(request.venue());
        event.setTicketPrice(request.ticketPrice());
        event.setTotalTickets(request.availableTickets());
        event.setAvailableTickets(request.availableTickets() - bookedTickets);
        return toResponse(eventRepository.save(event));
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    public Event getEventEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    }

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDepartment(),
                event.getDateTime(),
                event.getVenue(),
                event.getTicketPrice(),
                event.getAvailableTickets(),
                event.getTotalTickets());
    }
}
