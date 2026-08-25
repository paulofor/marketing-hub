package com.marketinghub.planning.web;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.planning.mapper.CommercialPlanMapper;
import com.marketinghub.planning.service.CommercialPlanAgentActivityService;
import com.marketinghub.planning.service.CommercialPlanJourneyHomologationService;
import com.marketinghub.planning.service.CommercialPlanOperationalFlowService;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.planning.service.CommercialPlanVisualAssetService;
import com.marketinghub.planning.service.CommercialPlanWeeklyExperimentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: proteger o contrato HTTP do planejamento comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanControllerTest {
  @Mock private CommercialPlanService service;
  @Mock private CommercialPlanWeeklyExperimentService weeklyExperimentService;
  @Mock private CommercialPlanMapper mapper;
  @Mock private CommercialPlanVersionService versionService;
  @Mock private CommercialPlanAgentActivityService agentActivityService;
  @Mock private CommercialPlanJourneyHomologationService journeyHomologationService;
  @Mock private CommercialPlanOperationalFlowService operationalFlowService;
  @Mock private CommercialPlanVisualAssetService visualAssetService;

  private MockMvc mockMvc;

  /** Monta o controller real com colaboradores isolados antes de cada cenário. */
  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new CommercialPlanController(
                    service,
                    weeklyExperimentService,
                    mapper,
                    versionService,
                    agentActivityService,
                    journeyHomologationService,
                    operationalFlowService,
                    visualAssetService))
            .build();
  }

  /** Recebe o ZIP multipart e entrega exatamente seus bytes ao validador do plano. */
  @Test
  void importsApprovedCreativePackageByMultipart() throws Exception {
    byte[] archive = "pacote-zip".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file =
        new MockMultipartFile("file", "rigel-approved.zip", "application/zip", archive);
    when(visualAssetService.importApprovedPackage(eq(4L), aryEq(archive))).thenReturn(List.of());

    mockMvc
        .perform(
            multipart("/api/planning/commercial-plans/4/visual-assets/approved-package").file(file))
        .andExpect(status().isOk());

    verify(visualAssetService).importApprovedPackage(eq(4L), aryEq(archive));
  }
}
