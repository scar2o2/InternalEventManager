package com.department.ticketsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BookingRequest {

    @NotNull
    private Long eventId;

    @Min(1)
    private Integer tickets;

    private List<Long> seatIds;

    public BookingRequest() {
    }

    public BookingRequest(Long eventId, Integer tickets, List<Long> seatIds) {
        this.eventId = eventId;
        this.tickets = tickets;
        this.seatIds = seatIds;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTickets() {
        return tickets;
    }

    public void setTickets(Integer tickets) {
        this.tickets = tickets;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}
