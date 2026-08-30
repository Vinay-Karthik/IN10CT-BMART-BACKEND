package com.bmart.service;

import com.bmart.entity.SupportTicket;
import com.bmart.entity.User;
import com.bmart.repository.SupportTicketRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;

    public SupportTicket createTicket(String username, String category, String issue, String conversationSummary) {
        User user = null;
        if (username != null && !username.isBlank()) {
            user = userRepository.findByEmail(username)
                    .orElseGet(() -> userRepository.findByUsername(username).orElse(null));
        }

        SupportTicket ticket = SupportTicket.builder()
                .userId(user != null ? user.getUserId() : null)
                .userEmail(user != null ? user.getEmail() : (username != null ? username : "guest@bmart.com"))
                .category(category != null ? category.toUpperCase() : "GENERAL")
                .issue(issue != null ? issue : "Customer Assistance Request")
                .conversationSummary(conversationSummary)
                .status("OPEN")
                .priority("HIGH")
                .build();

        return ticketRepository.save(ticket);
    }

    public List<SupportTicket> getUserTickets(String username) {
        User user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByUsername(username).orElse(null));
        if (user != null) {
            return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
        }
        return ticketRepository.findByUserEmailOrderByCreatedAtDesc(username);
    }

    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public SupportTicket updateTicketStatus(String ticketId, String newStatus) {
        SupportTicket ticket = ticketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new RuntimeException("Support ticket not found: " + ticketId));
        ticket.setStatus(newStatus.toUpperCase());
        return ticketRepository.save(ticket);
    }
}
