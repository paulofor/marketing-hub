package com.marketinghub.modelos.openai.catalogo.v1.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: representar um modelo retornado pelo catálogo técnico oficial da OpenAI. */
@Entity
@Table(name = "openai_model_catalog_v1", uniqueConstraints = {
        @UniqueConstraint(name = "uq_openai_model_catalog_v1_code", columnNames = "code")
})
@Getter
@Setter
public class OpenAiCatalogModelV1 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String code;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
}
