package com.marketinghub.sampleemail;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Email de amostra gerado para um {@link Experiment} específico.
 */
@Entity
@Table(name = "experiment_sample_email")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Experiment experiment;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "preview_text", length = 255)
    private String previewText;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String body;

    @Column(name = "call_to_action", length = 500)
    private String callToAction;

    @Column(length = 128)
    private String model;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
