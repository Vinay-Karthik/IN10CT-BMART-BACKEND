package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.SupportTicket;
import com.bmart.service.SupportTicketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateTicketRequest request
    ) {
        String username = userDetails != null ? userDetails.getUsername() : request.getEmail();
        SupportTicket ticket = supportTicketService.createTicket(
                username,
                request.getCategory(),
                request.getIssue(),
                request.getConversationSummary()
        );
        return ResponseEntity.ok(ApiResponse.success("Support ticket created successfully", ticket));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getTickets(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.success("Tickets retrieved", supportTicketService.getAllTickets()));
        }
        return ResponseEntity.ok(ApiResponse.success("User support tickets retrieved", supportTicketService.getUserTickets(userDetails.getUsername())));
    }

    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<ApiResponse<SupportTicket>> updateTicketStatus(
            @PathVariable String ticketId,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated", supportTicketService.updateTicketStatus(ticketId, status)));
    }

    @Data
    public static class CreateTicketRequest {
        private String email;
        private String category;
        private String issue;
        private String conversationSummary;
    }
}
