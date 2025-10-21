package com.marketinghub.deliverable;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Package grouping a set of deliverables for an {@link Experiment}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "deliverable_package", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"experiment_id", "name"})
})
public class DeliverablePackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    private Experiment experiment;

    @Column(nullable = false)
    private String name;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @Column(length = 255)
    private String model;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "deliverable_package_item",
            joinColumns = @JoinColumn(name = "deliverable_package_id"),
            inverseJoinColumns = @JoinColumn(name = "deliverable_id"))
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Deliverable> deliverables = new LinkedHashSet<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
