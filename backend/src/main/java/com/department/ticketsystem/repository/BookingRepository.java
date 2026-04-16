package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Booking;
import com.department.ticketsystem.model.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByBookingDateDesc(User user);

    @Query("""
        select b.event.name, sum(b.tickets)
        from Booking b
        group by b.event.id, b.event.name
        order by sum(b.tickets) desc
        """)
    List<Object[]> getBookingTotalsByEvent();

    @Query("""
        select function('date', b.bookingDate), sum(b.tickets)
        from Booking b
        where b.bookingDate >= :startDate
        group by function('date', b.bookingDate)
        order by function('date', b.bookingDate)
        """)
    List<Object[]> getBookingsOverTime(@Param("startDate") LocalDateTime startDate);
}
