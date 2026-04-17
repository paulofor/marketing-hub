package com.marketinghub.mds;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "artifact_lineage_edge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdsArtifactLineageEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_artifact_id", nullable = false)
    private MdsArtifactRecord parentArtifact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_artifact_id", nullable = false)
    private MdsArtifactRecord childArtifact;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
