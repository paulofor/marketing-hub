package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar a geração de sinais Meta Ads a partir de nichos enriquecidos do OPRM. */
class OprmEnrichedNicheMetaSignalServiceTest {
  private final TargetingElementRepository repository = mock(TargetingElementRepository.class);
  private final OprmEnrichedNicheMetaSignalService service = new OprmEnrichedNicheMetaSignalService(repository);

  /** Deve converter o CNAE campeão de beleza em interesses, cargos e comportamentos úteis para a Meta. */
  @Test
  void shouldBuildBeautySignalsFromCnae() {
    OprmEnrichedNicheMetaSignalService.MetaSignalPackage signalPackage = service.buildSignalPackage(cycle(), card());

    assertThat(signalPackage.interests()).contains("Salão de beleza", "Cabeleireiro", "WhatsApp Business");
    assertThat(signalPackage.roles()).contains("Cabeleireiro", "Manicure", "Pedicure");
    assertThat(signalPackage.behaviors()).contains("Small business owners", "Facebook Page admins");
  }

  /** Deve persistir elementos OPRM aprovados para que o worker busque ID e alcance oficiais na Meta. */
  @Test
  void shouldPublishApprovedTargetingElementsForWorker() {
    MarketNiche niche = new MarketNiche();
    niche.setId(18L);
    MarketNicheEnrichmentProfile profile = new MarketNicheEnrichmentProfile();
    profile.setCnaeCode("9602501");
    profile.setNeutralNicheName("Cabeleireiros, manicure e pedicure");
    OprmEnrichedNicheMetaSignalService.MetaSignalPackage signalPackage = new OprmEnrichedNicheMetaSignalService.MetaSignalPackage(
        List.of("Salão de beleza"), List.of("Cabeleireiro"), List.of("Small business owners"));
    when(repository.findByNicheId(18L)).thenReturn(List.of());

    service.publishTargetingElements(niche, profile, signalPackage);

    ArgumentCaptor<List<TargetingElement>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(3);
    assertThat(captor.getValue())
        .allSatisfy(element -> {
          assertThat(element.getNiche()).isSameAs(niche);
          assertThat(element.getSource()).isEqualTo(TargetingElementSource.OPRM_NICHE);
          assertThat(element.getStatus()).isEqualTo(TargetingElementStatus.APPROVED);
          assertThat(element.getDescription()).contains("9602501");
        });
    assertThat(captor.getValue()).extracting(TargetingElement::getType)
        .contains(TargetingElementType.INTEREST, TargetingElementType.JOB_TITLE, TargetingElementType.BEHAVIOR);
  }

  /** Deve aplicar os sinais gerados nas listas legadas do nicho para auditoria e compatibilidade. */
  @Test
  void shouldApplySignalListsToMarketNiche() {
    MarketNiche niche = new MarketNiche();
    OprmEnrichedNicheMetaSignalService.MetaSignalPackage signalPackage = new OprmEnrichedNicheMetaSignalService.MetaSignalPackage(
        List.of("Loja de roupas"), List.of("Lojista"), List.of("Small business owners"));

    service.applySignalsToNiche(niche, signalPackage);

    assertThat(niche.getInterestList()).containsExactly("Loja de roupas");
    assertThat(niche.getRoleList()).containsExactly("Lojista");
    assertThat(niche.getBehaviorList()).containsExactly("Small business owners");
    assertThat(niche.getInterests()).contains("Sinais iniciais Meta Ads gerados pelo OPRM NichoCNAE");
  }

  /** Cria ciclo do CNAE de beleza usado nos testes de sinais. */
  private OprmRoutineResearchCycle cycle() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNeutralNicheName("Cabeleireiros, manicure e pedicure");
    cycle.setSourceScore(new BigDecimal("90.00"));
    return cycle;
  }

  /** Cria cartão de rotina com linguagem operacional para enriquecer sinais comerciais. */
  private OprmNicheRoutineCard card() {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setRoutineSummary("Rotina com agenda e atendimento pelo WhatsApp.");
    card.setPainsSummary("Dificuldade de vender retorno para clientes do salão.");
    card.setMechanismOpportunitiesSummary("Contexto operacional com encaixes, horários vagos e Instagram.");
    return card;
  }
}
