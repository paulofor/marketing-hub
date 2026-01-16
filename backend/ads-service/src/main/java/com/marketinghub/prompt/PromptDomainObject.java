package com.marketinghub.prompt;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prompt_domain_object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptDomainObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_domain_id", nullable = false)
    private PromptDomain domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 64)
    private PromptDomainObjectType objectType;
}
