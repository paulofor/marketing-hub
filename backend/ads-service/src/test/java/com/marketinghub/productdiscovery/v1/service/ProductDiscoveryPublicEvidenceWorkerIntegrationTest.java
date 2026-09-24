package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Valida no backend o callback produzido pelo worker real com integrações externas simuladas. */
@EnabledIfEnvironmentVariable(named = "ARGOS_PUBLIC_EVIDENCE_FIXTURE", matches = ".+")
class ProductDiscoveryPublicEvidenceWorkerIntegrationTest {
  /** Confere contrato, consumo e candidatas do worker e rejeita política divergente no retorno. */
  @Test
  void acceptsWorkerCallbackWithNoInterviewAndPreservesOpenGaps() throws Exception {
    ObjectMapper json =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    var fixture =
        json.readTree(Files.readString(Path.of(System.getenv("ARGOS_PUBLIC_EVIDENCE_FIXTURE"))));
    var request = json.treeToValue(fixture.path("result"), ProductDiscoveryResultRequest.class);
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(fixture.path("job").path("cycleId").asLong());
    cycle.setEvidencePolicy("PUBLIC_SOURCES_V1");
    cycle.setResearchPlanJson(fixture.path("plan").path("planJson").asText());
    var repository = mock(ProductDiscoveryOpportunityRepository.class);
    when(repository.findAllByCycleIdOrderByScoreDesc(cycle.getId()))
        .thenReturn(
            request.opportunities().stream()
                .map(
                    item -> {
                      ProductDiscoveryOpportunity candidate = new ProductDiscoveryOpportunity();
                      candidate.setName(item.name());
                      return candidate;
                    })
                .toList());
    var service = new ProductDiscoveryGapResearchContractService(repository);
    var gate = mock(ProductDiscoveryGapDeepeningResponse.class);
    when(gate.interviewCount()).thenReturn(0);
    service.validatePlan(cycle, cycle.getResearchPlanJson());
    service.validateEvidenceReport(cycle, request, gate);
    assertThat(request.opportunities())
        .hasSize(2)
        .allSatisfy(item -> assertThat(item.decision().name()).isEqualTo("RESEARCH_MORE"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) request.evidenceReport().path("gapDeepening"))
        .put("evidencePolicy", "CONSENTED_INTERVIEWS_V1");
    assertThatThrownBy(() -> service.validateEvidenceReport(cycle, request, gate))
        .hasMessageContaining("política do relatório diverge");
  }
}
