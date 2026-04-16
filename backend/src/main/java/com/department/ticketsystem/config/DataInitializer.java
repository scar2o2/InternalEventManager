package com.department.ticketsystem.config;

import com.department.ticketsystem.model.Event;
import com.department.ticketsystem.model.Role;
import com.department.ticketsystem.model.User;
import com.department.ticketsystem.repository.EventRepository;
import com.department.ticketsystem.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, EventRepository eventRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@department.edu");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setDepartment("Administration");
            userRepository.save(admin);

            User student = new User();
            student.setName("Student User");
            student.setEmail("student@department.edu");
            student.setPassword(passwordEncoder.encode("Student@123"));
            student.setRole(Role.USER);
            student.setDepartment("Computer Science");
            userRepository.save(student);
        }

        if (eventRepository.count() == 0) {
            Event expo = new Event();
            expo.setName("Innovation Expo");
            expo.setDepartment("Computer Science");
            expo.setDateTime(LocalDateTime.now().plusDays(7));
            expo.setVenue("Main Auditorium");
            expo.setTicketPrice(BigDecimal.valueOf(150));
            expo.setAvailableTickets(120);
            expo.setTotalTickets(120);
            eventRepository.save(expo);

            Event summit = new Event();
            summit.setName("Faculty Research Summit");
            summit.setDepartment("Electronics");
            summit.setDateTime(LocalDateTime.now().plusDays(12));
            summit.setVenue("Seminar Hall A");
            summit.setTicketPrice(BigDecimal.valueOf(250));
            summit.setAvailableTickets(80);
            summit.setTotalTickets(80);
            eventRepository.save(summit);
        }
    }
}
