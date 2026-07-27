package com.marketinghub.niche.description.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.niche.description.NicheDetailedDescriptionRepository;
import com.marketinghub.repository.jpa.prompt.PromptRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NicheDetailedDescriptionServiceTest {

  @Mock private NicheDetailedDescriptionRepository repository;
  @Mock private MarketNicheRepository nicheRepository;
  @Mock private PromptRepository promptRepository;
  @Mock private EntityManager em;
  @Mock private CurrencyConversionService currencyConversionService;
  @Mock private CostAttributionService costAttributionService;

  private NicheDetailedDescriptionService service;

  @BeforeEach
  void setup() {
    service =
        new NicheDetailedDescriptionService(
            repository,
            nicheRepository,
            promptRepository,
            em,
            currencyConversionService,
            costAttributionService);
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
    when(repository.save(any(NicheDetailedDescription.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(currencyConversionService.usdToBrl(new BigDecimal("0.0015")))
        .thenReturn(new BigDecimal("0.01"));

    service.create(request);

    verify(costAttributionService).addCostToNiche(niche, new BigDecimal("0.01"));
  }
}
