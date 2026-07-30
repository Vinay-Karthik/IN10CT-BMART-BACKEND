package com.bmart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Column(nullable = false)
    private String action; // e.g., BAN_USER, APPROVE_SELLER, REJECT_PRODUCT, REFUND_ORDER

    @Column(name = "target_type", nullable = false)
    private String targetType; // USER, SELLER, PRODUCT, ORDER, REVIEW

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
