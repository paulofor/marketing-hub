package com.marketinghub.experiment.salespageab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTest;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTestStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariant;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantType;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbTestDto;
import com.marketinghub.experiment.salespageab.dto.UpdateExperimentSalesPageAbVariantRequest;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.salespageab.ExperimentSalesPageAbTestRepository;
import com.marketinghub.repository.jpa.experiment.salespageab.ExperimentSalesPageAbVariantRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** Valida a estrutura operacional de teste A/B de pagina de venda. */
@ExtendWith(MockitoExtension.class)
class ExperimentSalesPageAbTestServiceTest {
  @Mock private ExperimentSalesPageAbTestRepository testRepository;
  @Mock private ExperimentSalesPageAbVariantRepository variantRepository;
  @Mock private ExperimentRepository experimentRepository;
  @Mock private GeraSalesPagePublicationAuditRepository publicationAuditRepository;
  @Mock private ExperimentVideoAssetRepository videoAssetRepository;
  @Mock private JdbcTemplate jdbcTemplate;

  private ExperimentSalesPageAbTestService service;

  /** Inicializa o servico com repositorios simulados para testes unitarios. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentSalesPageAbTestService(
            testRepository,
            variantRepository,
            experimentRepository,
            publicationAuditRepository,
            videoAssetRepository,
            jdbcTemplate);
  }

  /** Garante que o plano padrao nasce com duas variantes equivalentes e metrica de checkout. */
  @Test
  void shouldCreateMetaVideoVsTraditionalTest() {
    Experiment experiment = Experiment.builder().id(60L).name("Agenda Fechada").build();
    given(experimentRepository.findById(60L)).willReturn(Optional.of(experiment));
    given(testRepository.save(any(ExperimentSalesPageAbTest.class)))
        .willAnswer(
            invocation -> {
              ExperimentSalesPageAbTest saved = invocation.getArgument(0);
              saved.setId(11L);
              saved.getVariants().get(0).setId(21L);
              saved.getVariants().get(1).setId(22L);
              return saved;
            });

    ExperimentSalesPageAbTestDto dto = service.createMetaVideoVsTraditional(60L);

    assertThat(dto.id()).isEqualTo(11L);
    assertThat(dto.experimentId()).isEqualTo(60L);
    assertThat(dto.status()).isEqualTo(ExperimentSalesPageAbTestStatus.DRAFT);
    assertThat(dto.primaryMetric()).isEqualTo("checkout_click_rate");
    assertThat(dto.metaSplitTestRecommended()).isTrue();
    assertThat(dto.variants()).hasSize(2);
    assertThat(dto.variants())
        .extracting("variantType")
        .containsExactly(
            ExperimentSalesPageAbVariantType.TRADITIONAL,
            ExperimentSalesPageAbVariantType.HUMAN_VIDEO);
    assertThat(dto.variants())
        .extracting("trafficWeight")
        .containsExactly(new BigDecimal("50.00"), new BigDecimal("50.00"));
  }

  /** Garante que uma variante pronta precisa de pagina, checkout, destino e coletores. */
  @Test
  void shouldPromoteTestToReadyWhenBothVariantsAreReady() {
    Experiment experiment = Experiment.builder().id(60L).build();
    ExperimentSalesPageAbTest test =
        ExperimentSalesPageAbTest.builder()
            .id(11L)
            .experiment(experiment)
            .name("A/B")
            .status(ExperimentSalesPageAbTestStatus.DRAFT)
            .hypothesis("Video aumenta confianca")
            .primaryMetric("checkout_click_rate")
            .winnerRule("menor custo por checkout")
            .minimumRuntimeDays(7)
            .minimumSampleSize(100)
            .metaSplitTestRecommended(true)
            .build();
    ExperimentSalesPageAbVariant variantA =
        readyVariant(test, 21L, "A", ExperimentSalesPageAbVariantType.TRADITIONAL);
    ExperimentSalesPageAbVariant variantB =
        readyVariant(test, 22L, "B", ExperimentSalesPageAbVariantType.HUMAN_VIDEO);
    variantB.setExperimentVideoAsset(readyApprovedVideo(experiment));
    test.getVariants().add(variantA);
    test.getVariants().add(variantB);
    given(experimentRepository.findById(60L)).willReturn(Optional.of(experiment));
    given(variantRepository.findByIdAndTestExperimentId(22L, 60L))
        .willReturn(Optional.of(variantB));
    given(variantRepository.save(variantB)).willReturn(variantB);

    ExperimentSalesPageAbTestDto dto =
        service.updateVariant(
            60L,
            22L,
            new UpdateExperimentSalesPageAbVariantRequest(
                ExperimentSalesPageAbVariantStatus.READY,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true));

    assertThat(dto.status()).isEqualTo(ExperimentSalesPageAbTestStatus.READY);
  }

  /** Garante que variante de video humano nao fica pronta sem video pronto e aprovado. */
  @Test
  void shouldNotPromoteHumanVideoVariantWithoutReadyApprovedVideo() {
    Experiment experiment = Experiment.builder().id(60L).build();
    ExperimentSalesPageAbTest test =
        ExperimentSalesPageAbTest.builder()
            .id(11L)
            .experiment(experiment)
            .name("A/B")
            .status(ExperimentSalesPageAbTestStatus.DRAFT)
            .hypothesis("Video aumenta confianca")
            .primaryMetric("checkout_click_rate")
            .winnerRule("menor custo por checkout")
            .minimumRuntimeDays(7)
            .minimumSampleSize(100)
            .metaSplitTestRecommended(true)
            .build();
    ExperimentSalesPageAbVariant variantA =
        readyVariant(test, 21L, "A", ExperimentSalesPageAbVariantType.TRADITIONAL);
    ExperimentSalesPageAbVariant variantB =
        readyVariant(test, 22L, "B", ExperimentSalesPageAbVariantType.HUMAN_VIDEO);
    test.getVariants().add(variantA);
    test.getVariants().add(variantB);
    given(experimentRepository.findById(60L)).willReturn(Optional.of(experiment));
    given(variantRepository.findByIdAndTestExperimentId(22L, 60L))
        .willReturn(Optional.of(variantB));
    given(variantRepository.save(variantB)).willReturn(variantB);

    ExperimentSalesPageAbTestDto dto =
        service.updateVariant(
            60L,
            22L,
            new UpdateExperimentSalesPageAbVariantRequest(
                ExperimentSalesPageAbVariantStatus.READY,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true));

    assertThat(dto.status()).isEqualTo(ExperimentSalesPageAbTestStatus.DRAFT);
  }

  /**
   * Garante que os resultados A/B calculam taxas por variante e sugerem vencedor apenas com
   * amostra.
   */
  @Test
  void shouldSummarizeAbTestResultsByVariant() throws Exception {
    Experiment experiment = Experiment.builder().id(60L).build();
    ExperimentSalesPageAbTest test =
        ExperimentSalesPageAbTest.builder()
            .id(11L)
            .experiment(experiment)
            .name("A/B")
            .status(ExperimentSalesPageAbTestStatus.READY)
            .hypothesis("Video aumenta confianca")
            .primaryMetric("checkout_click_rate")
            .winnerRule("maior checkout")
            .minimumRuntimeDays(7)
            .minimumSampleSize(100)
            .metaSplitTestRecommended(true)
            .build();
    test.getVariants()
        .add(readyVariant(test, 21L, "A", ExperimentSalesPageAbVariantType.TRADITIONAL));
    test.getVariants()
        .add(readyVariant(test, 22L, "B", ExperimentSalesPageAbVariantType.HUMAN_VIDEO));
    given(experimentRepository.findById(60L)).willReturn(Optional.of(experiment));
    given(testRepository.findByExperimentIdOrderByCreatedAtDesc(60L)).willReturn(List.of(test));
    given(
            jdbcTemplate.query(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.<RowMapper<Object>>any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()))
        .willAnswer(
            invocation -> List.of(mapAggregation(invocation.getArgument(1), 120L, 60L, 90000L, 3L)))
        .willAnswer(
            invocation ->
                List.of(mapAggregation(invocation.getArgument(1), 130L, 70L, 150000L, 8L)));

    var results = service.results(60L);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).winnerVariantKey()).isEqualTo("B");
    assertThat(results.get(0).status()).isEqualTo("VENCEDOR_SUGERIDO");
    assertThat(results.get(0).variants()).extracting("checkoutClicks").containsExactly(3L, 8L);
    assertThat(results.get(0).variants())
        .extracting("averageVisibleMsPerSession")
        .containsExactly(90000L, 150000L);
    assertThat(results.get(0).variants())
        .extracting(variant -> variant.variant().metricsSafeUrl())
        .containsExactly(
            "https://example.com/sales-a?mh_test=1", "https://example.com/sales-b?mh_test=1");
  }

  /** Cria uma agregacao simulada para o JdbcTemplate do resumo A/B. */
  private Object mapAggregation(
      RowMapper<?> mapper,
      long pageViews,
      long sessions,
      long averageVisibleMsPerSession,
      long checkoutClicks)
      throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    given(resultSet.getLong("page_views")).willReturn(pageViews);
    given(resultSet.getLong("sessions")).willReturn(sessions);
    given(resultSet.getLong("average_visible_ms_per_session"))
        .willReturn(averageVisibleMsPerSession);
    given(resultSet.getLong("checkout_clicks")).willReturn(checkoutClicks);
    given(resultSet.getLong("purchases")).willReturn(0L);
    given(resultSet.getTimestamp("last_event_at"))
        .willReturn(Timestamp.from(Instant.parse("2026-07-10T10:00:00Z")));
    return mapper.mapRow(resultSet, 0);
  }

  /** Cria uma variante pronta para uso em testes do servico. */
  private ExperimentSalesPageAbVariant readyVariant(
      ExperimentSalesPageAbTest test, Long id, String key, ExperimentSalesPageAbVariantType type) {
    return ExperimentSalesPageAbVariant.builder()
        .id(id)
        .test(test)
        .variantKey(key)
        .name("Variante " + key)
        .variantType(type)
        .status(ExperimentSalesPageAbVariantStatus.READY)
        .trafficWeight(new BigDecimal("50.00"))
        .salesPageUrl("https://example.com/sales-" + key.toLowerCase())
        .checkoutUrl("https://checkout.example.com/" + key.toLowerCase())
        .adDestinationUrl("https://example.com/sales-" + key.toLowerCase())
        .analyticsVariantParam("ab=" + key.toLowerCase())
        .requiredCollectorsPresent(true)
        .build();
  }

  /** Cria um ativo de video pronto e revisado para variantes que dependem de video humano. */
  private ExperimentVideoAsset readyApprovedVideo(Experiment experiment) {
    return ExperimentVideoAsset.builder()
        .id(501L)
        .experiment(experiment)
        .status(ExperimentVideoStatus.READY)
        .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
        .build();
  }
}
