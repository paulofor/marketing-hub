package com.marketinghub.niche.description.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.niche.description.repository.NicheDetailedDescriptionRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.prompt.repository.PromptRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NicheDetailedDescriptionServiceTest {

    @Mock
    private NicheDetailedDescriptionRepository repository;
    @Mock
    private MarketNicheRepository nicheRepository;
    @Mock
    private PromptRepository promptRepository;
    @Mock
    private EntityManager em;

    private NicheDetailedDescriptionService service;

    @BeforeEach
    void setup() {
        service = new NicheDetailedDescriptionService(repository, nicheRepository, promptRepository, em);
    }

    @Test
    void createRecomputesNicheTotalCostFromPersistedDescriptions() {
        Long nicheId = 16L;
        MarketNiche niche = new MarketNiche();
        niche.setId(nicheId);

        CreateNicheDetailedDescriptionRequest request = new CreateNicheDetailedDescriptionRequest();
        request.setMarketNicheId(nicheId);
        request.setDescription("Descrição gerada");
        request.setCostUsd(new BigDecimal("0.0015"));

        when(nicheRepository.existsById(nicheId)).thenReturn(true);
        when(em.getReference(MarketNiche.class, nicheId)).thenReturn(niche);
        when(repository.save(any(NicheDetailedDescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.sumCostUsdByMarketNicheId(nicheId)).thenReturn(new BigDecimal("0.0015"));

        service.create(request);

        assertThat(niche.getTotalCost()).isEqualByComparingTo("0.0015");
        verify(repository).sumCostUsdByMarketNicheId(nicheId);
    }
}
