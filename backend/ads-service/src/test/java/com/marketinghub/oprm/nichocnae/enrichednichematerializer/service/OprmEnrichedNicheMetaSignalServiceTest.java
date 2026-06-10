package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a geração de sinais Meta Ads a partir de nichos enriquecidos do OPRM. */
class OprmEnrichedNicheMetaSignalServiceTest {
  private final OprmEnrichedNicheMetaSignalService service = new OprmEnrichedNicheMetaSignalService();

  /** Deve converter o CNAE campeão de beleza em interesses, cargos e comportamentos úteis para a Meta. */
  @Test
  void shouldBuildBeautySignalsFromCnae() {
    OprmEnrichedNicheMetaSignalService.MetaSignalPackage signalPackage = service.buildSignalPackage(cycle(), card());

    assertThat(signalPackage.interests()).contains("Salão de beleza", "Cabeleireiro", "WhatsApp Business");
    assertThat(signalPackage.roles()).contains("Cabeleireiro", "Manicure", "Pedicure");
    assertThat(signalPackage.behaviors()).contains("Small business owners", "Facebook Page admins");
  }

  /** Deve montar resumo dos sinais para armazenamento no backend sem criar elementos de targeting no OPRM. */
  @Test
  void shouldBuildReadableSummaryForBackendStorage() {
    OprmEnrichedNicheMetaSignalService.MetaSignalPackage signalPackage = new OprmEnrichedNicheMetaSignalService.MetaSignalPackage(
        List.of("Loja de roupas"), List.of("Lojista"), List.of("Small business owners"));

    String summary = service.buildReadableSignalSummary(signalPackage);

    assertThat(summary).contains("Sinais iniciais Meta Ads gerados pelo OPRM NichoCNAE");
    assertThat(summary).contains("Interesses: Loja de roupas");
    assertThat(summary).contains("Cargos: Lojista");
    assertThat(summary).contains("Comportamentos: Small business owners");
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
