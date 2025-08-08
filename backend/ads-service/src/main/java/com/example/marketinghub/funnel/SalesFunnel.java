package com.example.marketinghub.funnel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sales funnel definition.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesFunnel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private String name;
    private String objective;

    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "funnel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FunnelStep> steps = new ArrayList<>();
}
