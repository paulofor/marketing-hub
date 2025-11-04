package com.marketinghub.leadportal;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes the questions and data capture flow that a lead will experience inside the portal.
 */
@Entity
@Table(name = "lead_portal_flow", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lead_portal_flow_slug", columnNames = {"slug"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadPortalFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(length = 128)
    private String model;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String prompt;

    @Builder.Default
    @Column(nullable = false)
    private boolean approved = false;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Builder.Default
    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<LeadPortalFlowQuestion> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
