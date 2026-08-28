package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.ImportedCompletedAgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.dto.CreateCommercialPlanVisualAssetRequest;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.storage.AssetStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a governança da biblioteca audiovisual do plano comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanVisualAssetServiceTest {
  @Mock private CommercialPlanService planService;
  @Mock private CommercialPlanVisualAssetRepository repository;

  private CommercialPlanVisualAssetService service;

  /** Prepara o serviço com persistência simulada antes de cada teste. */
  @BeforeEach
  void setUp() {
    service = new CommercialPlanVisualAssetService(planService, repository);
  }

  /** Deve cadastrar vídeo normalizado como rascunho auditável. */
  @Test
  void createsVideoReference() {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.save(any(CommercialPlanVisualAsset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.create(
            2L,
            new CreateCommercialPlanVisualAssetRequest(
                "https://cdn.example/product.mp4",
                "video",
                "Demonstração do kit",
                "ADS",
                "Produto",
                "Uso autorizado"));

    assertThat(result.mediaType()).isEqualTo("VIDEO");
    assertThat(result.status().name()).isEqualTo("DRAFT");
  }

  /** Deve bloquear tipos de mídia que os executores não conseguem consumir. */
  @Test
  void rejectsUnsupportedMediaType() {
    assertThatThrownBy(
            () ->
                service.create(
                    2L,
                    new CreateCommercialPlanVisualAssetRequest(
                        "https://cdn.example/product.pdf",
                        "DOCUMENT",
                        "Manual",
                        "DELIVERY",
                        "Produto",
                        "Uso autorizado")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("mediaType must be IMAGE or VIDEO");
  }

  /** Cadastra captura fiel do produto como prova distinta de peça comercial ou entrega. */
  @Test
  void createsProductProofReference() {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.save(any(CommercialPlanVisualAsset.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.create(
            2L,
            new CreateCommercialPlanVisualAssetRequest(
                "https://cdn.example/rigel-tasting.png",
                "image",
                "Degustação real de Rigel",
                "product_proof",
                "Homologação local",
                "Uso autorizado"));

    assertThat(result.purpose()).isEqualTo("PRODUCT_PROOF");
    assertThat(result.purposes()).containsExactly("PRODUCT_PROOF");
  }

  /** Bloqueia finalidade livre que fragmentaria a biblioteca e sua linhagem. */
  @Test
  void rejectsUnsupportedPurpose() {
    assertThatThrownBy(
            () ->
                service.create(
                    2L,
                    new CreateCommercialPlanVisualAssetRequest(
                        "https://cdn.example/generic.png",
                        "IMAGE",
                        "Imagem genérica",
                        "MOCKUP",
                        "Desconhecida",
                        "Uso autorizado")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("PRODUCT_PROOF");
  }

  /** Importa peças, hashes e quatro execuções reais sem publicar ou gerar gasto. */
  @Test
  void importsApprovedPackageWithAuditableAgentTasks() throws Exception {
    AssetStorageService storage = org.mockito.Mockito.mock(AssetStorageService.class);
    CommercialPlanVersionService versions =
        org.mockito.Mockito.mock(CommercialPlanVersionService.class);
    BusinessProcessDefinitionRepository processes =
        org.mockito.Mockito.mock(BusinessProcessDefinitionRepository.class);
    AgentTaskService tasks = org.mockito.Mockito.mock(AgentTaskService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    plan.setExperiment(experiment);
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(48L);
    AtomicLong ids = new AtomicLong(100L);
    when(planService.getPlan(4L)).thenReturn(plan);
    when(repository.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(any(), any()))
        .thenReturn(List.of());
    when(storage.storeBytes(any(), any(), any(), any()))
        .thenAnswer(
            invocation ->
                new AssetStorageService.StoredObject(
                    "stored/" + invocation.getArgument(1),
                    "https://cdn.example/" + invocation.getArgument(1),
                    ((byte[]) invocation.getArgument(0)).length,
                    invocation.getArgument(2),
                    true));
    when(repository.save(any(CommercialPlanVisualAsset.class)))
        .thenAnswer(
            invocation -> {
              CommercialPlanVisualAsset asset = invocation.getArgument(0);
              asset.setId(ids.incrementAndGet());
              return asset;
            });
    when(versions.current(4L))
        .thenReturn(
            new CommercialPlanVersionDto(
                10L, 4L, 3, "{}", "USER", "contexto", Instant.parse("2026-08-25T10:00:00Z")));
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "creative-production-approval", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    CommercialPlanVisualAssetService importService =
        new CommercialPlanVisualAssetService(
            planService, repository, new ObjectMapper(), storage, versions, processes, tasks);

    var imported =
        importService.importApprovedPackage(
            4L, ApprovedCreativePackageArchiveTest.validPackageBytes());

    assertThat(imported).hasSize(2);
    assertThat(imported).allMatch(item -> item.status().name().equals("APPROVED"));
    assertThat(imported).allMatch(item -> item.contentSha256().length() == 64);
    assertThat(imported)
        .extracting(item -> item.creativePackageId())
        .containsOnly(imported.getFirst().creativePackageId());
    ArgumentCaptor<ImportedCompletedAgentTask> taskCaptor =
        ArgumentCaptor.forClass(ImportedCompletedAgentTask.class);
    verify(tasks, times(4)).recordImportedCompletedTask(taskCaptor.capture());
    assertThat(taskCaptor.getAllValues())
        .extracting(ImportedCompletedAgentTask::processActivityId)
        .containsExactly("nonAudiovisual", "audiovisual", "customer", "commercial");
    assertThat(taskCaptor.getAllValues())
        .extracting(ImportedCompletedAgentTask::assignedAgentKey)
        .containsExactly(
            "communication-director", "videomaker", "customer-agent", "meta-ad-approver");
  }
}
