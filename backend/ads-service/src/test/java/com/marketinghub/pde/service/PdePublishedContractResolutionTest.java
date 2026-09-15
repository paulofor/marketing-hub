package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.service.publishslotcontract.PublishPdeProductionSlotContractRequest;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.product.web.PdePublicProductController;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.http.HttpClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/** Verifica que contratos publicados não misturam produtos ou versões por fallback. */
class PdePublishedContractResolutionTest {
  private static final String SLUG = "metodo-musa-7-dias";
  private static final String VERSION = "musa-pde-entry-v5-video-explicativo";
  private final PdeProductionSlotRepository repository = mock(PdeProductionSlotRepository.class);
  private final PdeProductionSlotService service =
      new PdeProductionSlotService(
          repository,
          mock(ExperimentVideoAssetRepository.class),
          HttpClient.newHttpClient(),
          new ObjectMapper());

  /** Reproduz a v5 ativa sem snapshot, impedindo o controller de consultar a v7 global. */
  @Test
  void doesNotServeGlobalV7ForUnpublishedV5() throws Exception {
    when(repository.findByProductSlugAndSlotCode(SLUG, "v5")).thenReturn(Optional.of(slot(null)));
    ProductService products = mock(ProductService.class);
    when(products.getPublicPdeExperienceJson(SLUG))
        .thenReturn("{\"slug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"v7\"}");
    var mvc =
        MockMvcBuilders.standaloneSetup(new PdePublicProductController(products, service))
            .setControllerAdvice(new com.marketinghub.web.ApiExceptionHandler())
            .build();

    mvc.perform(get("/api/pde/products/" + SLUG).param("slotCode", "v5"))
        .andExpect(status().isConflict());
    verifyNoInteractions(products);
  }

  /** Mantém a leitura global apenas quando nenhuma versão foi solicitada. */
  @Test
  void preservesUnqualifiedLookup() {
    assertThat(service.findPublishedExperienceJson(SLUG, " ", null)).isEmpty();
    verifyNoInteractions(repository);
  }

  /** Resolve o snapshot exato por versão ou código, normalizando somente espaços. */
  @Test
  void resolvesPublishedSnapshotByEitherSelector() {
    String json = contract(SLUG, VERSION);
    when(repository.findByProductSlugAndSlotCode(SLUG, "v5"))
        .thenReturn(Optional.of(slot(" " + json + " ")));
    when(repository.findFirstByProductSlugAndExperienceVersionOrderByPublishedAtDesc(SLUG, VERSION))
        .thenReturn(Optional.of(slot(json)));

    assertThat(service.findPublishedExperienceJson(SLUG, " V5 ", " " + VERSION + " "))
        .contains(json);
    assertThat(service.findPublishedExperienceJson(SLUG, null, VERSION)).contains(json);
  }

  /** Recusa um código inexistente mesmo quando outra versão válida também foi enviada. */
  @Test
  void rejectsUnknownSlotWithoutSwitchingSelectors() {
    assertFailure("v99", VERSION, 404);
  }

  /** Recusa versão não publicada em vez de resolver o produto global. */
  @Test
  void rejectsUnknownExperienceVersion() {
    assertFailure(null, "unknown", 404);
  }

  /** Recusa seletores que apontam para experiências diferentes. */
  @Test
  void rejectsConflictingSelectors() {
    when(repository.findByProductSlugAndSlotCode(SLUG, "v5"))
        .thenReturn(Optional.of(slot(contract(SLUG, VERSION))));
    assertFailure("v5", "v7", 409);
  }

  /** Impede publicação ausente, JSON inválido e payload sem identidade canônica. */
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "broken-json", "null", "[]", "{}"})
  void rejectsMissingOrInvalidSnapshot(String json) {
    when(repository.findByProductSlugAndSlotCode(SLUG, "v5")).thenReturn(Optional.of(slot(json)));
    assertFailure("v5", null, 409);
  }

  /** Recusa snapshot de outro produto ou outra experiência no registro solicitado. */
  @Test
  void rejectsForeignContractIdentity() {
    for (String json : new String[] {contract("other-product", VERSION), contract(SLUG, "v7")}) {
      when(repository.findByProductSlugAndSlotCode(SLUG, "v5")).thenReturn(Optional.of(slot(json)));
      assertFailure("v5", null, 409);
    }
  }

  /** Impede gravar uma publicação de outro produto antes que ela chegue à URL pública. */
  @Test
  void rejectsForeignProductOnPublication() {
    when(repository.findByProductSlugAndSlotCode(SLUG, "v5")).thenReturn(Optional.of(slot(null)));
    assertThatThrownBy(
            () ->
                service.publishProductionSlotContract(
                    SLUG,
                    "v5",
                    new PublishPdeProductionSlotContractRequest(
                        contract("other-product", VERSION), "qa")))
        .isInstanceOf(ResponseStatusException.class);
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
        .save(org.mockito.ArgumentMatchers.any());
  }

  /** Confere o status funcional da recusa sem depender do texto da mensagem. */
  private void assertFailure(String code, String version, int status) {
    assertThatThrownBy(() -> service.findPublishedExperienceJson(SLUG, code, version))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode().value()).isEqualTo(status));
  }

  /** Monta a versão persistida com a identidade da v5 observada no incidente. */
  private PdeProductionSlot slot(String json) {
    return PdeProductionSlot.builder()
        .slotCode("v5")
        .productSlug(SLUG)
        .experienceVersion(VERSION)
        .publishedExperienceJson(json)
        .build();
  }

  /** Produz um contrato mínimo com identidade explícita para as validações. */
  private String contract(String slug, String version) {
    return "{\"slug\":\"" + slug + "\",\"experienceVersion\":\"" + version + "\"}";
  }
}
