package com.marketinghub.interactionjourney.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interaction_journey_element")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionJourneyElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    @JsonIgnore
    private InteractionJourneyStep step;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private InteractionJourneyElement parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    @Builder.Default
    private List<InteractionJourneyElement> children = new ArrayList<>();

    @Column(name = "order_index")
    private Integer orderIndex;

    private String label;

    private String type;

    @Column(columnDefinition = "LONGTEXT")
    private String notes;

    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;
}
