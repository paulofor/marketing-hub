package com.marketinghub.prompt;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "prompt_domain")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class PromptDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 191)
    private String name;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PromptDomainObject> objects = new ArrayList<>();

    public List<PromptDomainObjectType> getObjectTypes() {
        return objects == null ? List.of() : objects.stream()
                .map(PromptDomainObject::getObjectType)
                .collect(Collectors.toList());
    }

    public void setObjectTypes(List<PromptDomainObjectType> types) {
        if (objects == null) {
            objects = new ArrayList<>();
        } else {
            objects.clear();
        }
        if (types == null || types.isEmpty()) {
            return;
        }
        for (PromptDomainObjectType type : types) {
            objects.add(PromptDomainObject.builder()
                    .domain(this)
                    .objectType(type)
                    .build());
        }
    }
}
