package com.bmart.repository;

import com.bmart.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SupportTicket> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    Optional<SupportTicket> findByTicketId(String ticketId);
    long countByStatus(String status);
}
