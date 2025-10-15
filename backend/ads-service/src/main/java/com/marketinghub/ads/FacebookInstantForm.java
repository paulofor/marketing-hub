package com.marketinghub.ads;

import com.marketinghub.hypothesis.Hypothesis;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity(name = "fb_instant_form")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FacebookInstantForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hypothesis_id", nullable = false)
    private Hypothesis hypothesis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private FacebookPage page;

    @Column(name = "form_id", length = 128)
    private String formId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String status;

    @Column(length = 12)
    private String locale;

    @Column(name = "leads_count")
    private Long leadsCount;

    @Column(name = "created_time")
    private Instant createdTime;

    @Column(name = "updated_time")
    private Instant updatedTime;

    @Column(name = "follow_up_action_url", length = 512)
    private String followUpActionUrl;

    @Column(name = "privacy_policy_url", length = 512)
    private String privacyPolicyUrl;

    @Column(length = 128)
    private String model;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(nullable = false)
    private boolean approved;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "share_link", length = 512)
    private String shareLink;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
