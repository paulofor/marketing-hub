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
@Table(name = "interaction_journey_step")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionJourneyStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id")
    @JsonIgnore
    private InteractionJourney journey;

    @Column(name = "order_index")
    private Integer orderIndex;

    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    @Builder.Default
    private List<InteractionJourneyElement> elements = new ArrayList<>();
}
