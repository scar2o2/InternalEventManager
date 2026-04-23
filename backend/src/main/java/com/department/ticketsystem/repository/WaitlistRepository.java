package com.department.ticketsystem.repository;

import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.model.WaitlistEntry;
import com.department.ticketsystem.model.WaitlistStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    List<WaitlistEntry> findByEventAndStatusOrderByCreatedAtAsc(Event event, WaitlistStatus status);

    long countByEventAndStatus(Event event, WaitlistStatus status);

    Optional<WaitlistEntry> findByEventAndUserAndStatus(Event event, User user, WaitlistStatus status);

    void deleteByEvent(Event event);
}
