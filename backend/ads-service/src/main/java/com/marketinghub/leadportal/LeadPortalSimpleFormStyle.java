package com.marketinghub.leadportal;

import com.marketinghub.leadportal.persistence.LeadPortalSimpleFormStyleDefinitionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.math.BigDecimal;

/**
 * Visual style applied to lead portal simple forms.
 */
@Entity
@Table(name = "lead_portal_simple_form_style",
        uniqueConstraints = @UniqueConstraint(name = "uk_lead_portal_simple_form_style_slug", columnNames = "slug"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadPortalSimpleFormStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "text_model", length = 128)
    private String textModel;

    @Column(name = "text_prompt", columnDefinition = "LONGTEXT")
    private String textPrompt;

    @Column(name = "text_parameters", columnDefinition = "LONGTEXT")
    private String textParameters;

    @Column(name = "image_model", length = 128)
    private String imageModel;

    @Column(name = "image_prompt", columnDefinition = "LONGTEXT")
    private String imagePrompt;

    @Column(name = "image_negative_prompt", columnDefinition = "LONGTEXT")
    private String imageNegativePrompt;

    @Column(name = "image_parameters", columnDefinition = "LONGTEXT")
    private String imageParameters;

    @Column(name = "image_batch_size")
    private Integer imageBatchSize;

    @Column(name = "image_aspect_ratio", length = 32)
    private String imageAspectRatio;

    @Column(name = "preview_image_url", length = 512)
    private String previewImageUrl;

    @Column(name = "definition", columnDefinition = "LONGTEXT")
    @Convert(converter = LeadPortalSimpleFormStyleDefinitionConverter.class)
    private LeadPortalSimpleFormStyleDefinition definition;

    @Column(name = "generation_cost_usd", precision = 10, scale = 4)
    private BigDecimal generationCostUsd;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
