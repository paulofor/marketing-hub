package com.marketinghub.targeting;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Responsabilidade: validar consultas JPA dos elementos de segmentação. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class TargetingElementRepositoryTest {

    @Autowired
    private TargetingElementRepository repository;

    @Autowired
    private MarketNicheRepository nicheRepository;

    /** Deve listar interesses, cargos e comportamentos em revisão para validação oficial da Meta Ads. */
    @Test
    void findMetaAdsPendingIncludesNeedsReviewInterestJobTitleAndBehavior() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Moda").build());
        TargetingElement interest = pendingElement(niche, TargetingElementType.INTEREST, "Fashion");
        TargetingElement jobTitle = pendingElement(niche, TargetingElementType.JOB_TITLE, "Fashion Designer");
        TargetingElement behavior = pendingElement(niche, TargetingElementType.BEHAVIOR, "Engaged shoppers");
        TargetingElement draft = pendingElement(niche, TargetingElementType.INTEREST, "Draft interest");
        draft.setStatus(TargetingElementStatus.DRAFT);
        repository.save(interest);
        repository.save(jobTitle);
        repository.save(behavior);
        repository.save(draft);

        var pending = repository.findMetaAdsPending(PageRequest.of(0, 10));

        assertThat(pending)
                .extracting(TargetingElement::getTerm)
                .contains("Fashion", "Fashion Designer", "Engaged shoppers")
                .doesNotContain("Draft interest");
    }

    /** Cria elemento de segmentação em revisão para reduzir duplicação no teste. */
    private TargetingElement pendingElement(MarketNiche niche, TargetingElementType type, String term) {
        return TargetingElement.builder()
                .niche(niche)
                .type(type)
                .term(term)
                .source(TargetingElementSource.AI)
                .status(TargetingElementStatus.NEEDS_REVIEW)
                .metaIdUnavailable(false)
                .build();
    }
}
