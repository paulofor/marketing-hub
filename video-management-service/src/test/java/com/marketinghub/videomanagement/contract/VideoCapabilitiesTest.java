package com.marketinghub.videomanagement.contract;

import static org.assertj.core.api.Assertions.*;
import com.marketinghub.videocontract.v1.VideoCapabilities;
import com.marketinghub.videocontract.v1.ProductionCostPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: prevenir divergência de duração, tarifa e custo integral sem chamadas externas. */
class VideoCapabilitiesTest {
  private static final Instant NOW = Instant.parse("2026-11-05T15:00:00Z");

  /** Reproduz a incompatibilidade original e mantém 32 segundos como quatro tomadas reais. */
  @Test void gen45Rejects15AndPreserves32() {
    var model = VideoCapabilities.model("gen4.5").orElseThrow();
    assertThatThrownBy(() -> model.requireDuration(15)).hasMessageContaining("2–10s");
    var plan = model.plan(32);
    assertThat(plan.clipSeconds()).containsExactly(10, 10, 10, 2);
    assertThat(plan.finalSeconds()).isEqualTo(32);
    assertThat(plan.generatedSeconds()).isEqualTo(32);
    assertThat(plan.trimSeconds()).isZero();
    assertThat(model.plan(31).clipSeconds()).containsExactly(10,10,9,2);
  }

  /** Não cria resto de um segundo em modelo com mínimo de quatro. */
  @Test void seedance25IsNot20AndRebalancesMinimum() {
    var model = VideoCapabilities.provider("RUNWAY_SEEDANCE_2_5").orElseThrow();
    model.requireDuration(30);
    assertThat(model.modelId()).isEqualTo("seedance2_5");
    assertThat(model.plan(32).clipSeconds()).containsExactly(28,4);
    assertThat(VideoCapabilities.provider("RUNWAY_SEEDANCE_2").orElseThrow().maxSeconds()).isEqualTo(15);
    assertThat(VideoCapabilities.provider("PIPPIT_SEEDANCE_2_5")).isEmpty();
  }

  /** Respeita durações discretas e separa excedente cobrado do tempo final. */
  @Test void veoDiscreteDurationsAndAudioTariff() {
    var model = VideoCapabilities.model("veo3.1_fast").orElseThrow();
    assertThatThrownBy(() -> model.requireDuration(5)).hasMessageContaining("[4, 6, 8]");
    assertThat(model.plan(32).clipSeconds()).containsExactly(8,8,8,8);
    assertThat(model.plan(15).generatedSeconds()).isEqualTo(16);
    assertThat(model.plan(15).trimSeconds()).isEqualTo(1);
    assertThat(model.credits(8,"720p",false,0,0,NOW)).isEqualByComparingTo("80");
    assertThat(model.credits(8,"720p",true,0,0,NOW)).isEqualByComparingTo("120");
  }

  /** Calcula resolução e referência de vídeo por tomada, sem tarifa universal de doze créditos. */
  @Test void seedanceReferencesAndResolution() {
    var model = VideoCapabilities.model("seedance2_5").orElseThrow();
    assertThat(model.credits(4,"480p",true,2,0,NOW)).isEqualByComparingTo("80");
    assertThat(model.credits(10,"720p",false,1,6,NOW)).isEqualByComparingTo("390");
    assertThat(model.credits(10,"1080p",false,1,6,NOW)).isEqualByComparingTo("884");
  }

  /** Aplica arredondamento e promoção com prazo e recusa usar uma tarifa já vencida. */
  @Test void klingPromotionAndExpiredPrice() {
    var model = VideoCapabilities.model("kling_4").orElseThrow();
    assertThat(model.credits(5,"720p",true,0,0,NOW)).isEqualByComparingTo("64");
    assertThat(model.adapterModes()).isEmpty();
    assertThatThrownBy(() -> model.credits(5,"720p",true,0,0,Instant.parse("2026-11-12T00:00:00Z"))).hasMessageContaining("vencido");
    assertThatThrownBy(() -> model.credits(5,"720p",false,0,0,NOW)).hasMessageContaining("não precificados");
  }

  /** Exige fontes para zeros e interrompe repetição quando uma tentativa anterior é incerta. */
  @Test void fullCostIsNotAnAuthorizationOrUnknownZero() {
    var complete = costs(List.of());
    assertThat(complete.maximumRemainingUsd(NOW)).isEqualByComparingTo("1.75");
    assertThatThrownBy(() -> costs(List.of("pending-task")).maximumRemainingUsd(NOW)).hasMessageContaining("incerto");
    var missing = new ProductionCostPlan(2L,9L,16L,VideoCapabilities.catalog().revision(),"(RUNWAY)",32,"2026-11-06T00:00:00Z",Map.of(),List.of());
    assertThatThrownBy(() -> missing.maximumRemainingUsd(NOW)).hasMessageContaining("AGENTS");
  }

  /** Verifica todos os pequenos comprimentos para prevenir restos inválidos em outros produtos. */
  @Test void partitionsAreValidAcrossAllCatalogModels() {
    for (var model : VideoCapabilities.catalog().models()) for (int target=6;target<=121;target++) {
      var plan=model.plan(target);
      assertThat(plan.clipSeconds()).allSatisfy(model::requireDuration);
      assertThat(plan.clipSeconds().stream().mapToInt(Integer::intValue).sum()).isEqualTo(plan.generatedSeconds());
      assertThat(plan.generatedSeconds()-plan.trimSeconds()).isEqualTo(target);
      assertThat(plan.trimSeconds()).isLessThan(model.minSeconds()+2);
    }
  }

  /** Cria custos sintéticos de um segundo contexto sem nome ou identidade de produto real. */
  private ProductionCostPlan costs(List<String> unresolved) {
    var values = new java.util.LinkedHashMap<String, ProductionCostPlan.Item>();
    ProductionCostPlan.REQUIRED.forEach(key -> values.put(key,new ProductionCostPlan.Item(new BigDecimal("0.25"),"fixture determinística")));
    return new ProductionCostPlan(2L,9L,16L,VideoCapabilities.catalog().revision(),"(RUNWAY)",32,"2026-11-06T00:00:00Z",values,unresolved);
  }
}
