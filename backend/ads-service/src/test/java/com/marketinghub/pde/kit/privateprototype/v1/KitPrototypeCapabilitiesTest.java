package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrototypeCapabilities;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir implementação por nome/ID ou reaproveitamento de contrato bloqueado.
 */
class KitPrototypeCapabilitiesTest {
  /** Aceita o alias explícito do caso original e outro perfil em contexto diferente. */
  @Test
  void acceptsExplicitProfilesWithoutNameOrIdExceptions() {
    assertThat(resolve(7, 5, "público nails-v1", false).profileCode()).isEqualTo("nails-v1");
    assertThat(resolve(75, 51, "público barber-v1", false).profileCode()).isEqualTo("barber-v1");
  }

  /** Não infere o tipo de kit a partir de um texto ambíguo ou da identidade do produto. */
  @Test
  void refusesAbsentOrAmbiguousProfile() {
    assertThat(resolve(7, 5, "Capella Agenda Cheia", false).available()).isFalse();
    assertThat(resolve(17, 15, "nails-v1 ou barber-v1", false).available()).isFalse();
  }

  /** Um bloqueio mais recente de Dédalo não pode herdar READY da tentativa anterior. */
  @Test
  void refusesLaterBlockedContract() {
    assertThat(resolve(7, 5, "público nails-v1", true).available()).isFalse();
  }

  /**
   * Recupera o formato aprovado de Capella e outro perfil sem nova inferência ou exceção por ID.
   */
  @Test
  void acceptsExplicitProfileInV6Format() {
    assertThat(
            resolve(
                    7,
                    12,
                    "Atena #678; experimento 105",
                    "Kit visual personalizado, privado e determinístico para nails-v1, apresentado em uma experiência responsiva de três passos: briefing, resultado e aplicação por copiar ou baixar.",
                    "",
                    false)
                .profileCode())
        .isEqualTo("nails-v1");
    assertThat(
            resolve(775, 512, "Atena #878", "Kit privado para barber-v1.", "", false).profileCode())
        .isEqualTo("barber-v1");
  }

  /** Recusa ausência, código parcial e divergência entre os dois aliases do contrato. */
  @Test
  void refusesMissingPartialOrConflictingFormat() {
    assertThat(resolve(7, 12, "Atena #678", "Kit de unhas", "", false).available()).isFalse();
    assertThat(resolve(777, 512, "Atena #878", "Kit nails-v10", "", false).available()).isFalse();
    assertThat(resolve(7, 12, "nails-v1", "Kit barber-v1", "", false).available()).isFalse();
    assertThat(resolve(777, 512, "Atena #878", "nails-v1 ou barber-v1", "", false).available())
        .isFalse();
  }

  /** Mantém código estruturado válido e bloqueio posterior como autoridades do contrato. */
  @Test
  void preservesStructuredProfileAndLatestBlock() {
    assertThat(resolve(77, 52, "Atena", "Kit", "barber-v1", false).profileCode())
        .isEqualTo("barber-v1");
    assertThat(resolve(7, 12, "Atena", "Kit nails-v1", "unsupported", false).available()).isFalse();
    assertThat(resolve(7, 12, "Atena", "Kit nails-v1", "", true).available()).isFalse();
    assertThat(resolve(7, 12, "nails-v1", "Kit nails-v1", "", false).profileCode())
        .isEqualTo("nails-v1");
  }

  /** Preserva as fixtures anteriores que identificavam o perfil somente pela referência. */
  private com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract
          .Capability
      resolve(long productId, long cycleId, String profile, boolean blocked) {
    return resolve(productId, cycleId, profile, "", "", blocked);
  }

  /**
   * Monta contratos segregados e confirma que a consulta usa o experimento e início do ciclo
   * exatos.
   */
  private com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract
          .Capability
      resolve(
          long productId,
          long cycleId,
          String profile,
          String format,
          String explicit,
          boolean blocked) {
    var tasks = mock(AgentTaskRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(900L + cycleId);
    cycle.setChainDefinitionId(26L);
    cycle.setCreatedAt(Instant.EPOCH);
    cycle.setStage("ADJUSTMENT");
    cycle.setStatus("OPEN");
    var plan = new BusinessProcessDefinition();
    plan.setId(116L);
    plan.setProcessCode("pde-commercial-plan-offer");
    var construction = new BusinessProcessDefinition();
    construction.setId(117L);
    construction.setProcessCode("pde-construction-approval");
    var first = new BusinessProcessChainItem();
    first.setProcessDefinition(plan);
    var second = new BusinessProcessChainItem();
    second.setProcessDefinition(construction);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(first, second));
    when(chains.findById(26L)).thenReturn(Optional.of(chain));
    var designed = new ArrayList<AgentTaskFunctionalSnapshot>();
    for (String stage : List.of("journey", "deliverables", "access"))
      designed.add(
          new AgentTaskFunctionalSnapshot(
              (long) designed.size() + 1,
              117L,
              "pde-construction-approval",
              stage,
              "landing-generator",
              "COMPLETED",
              Instant.EPOCH,
              Instant.EPOCH,
              "{\"decision\":\"READY\"}"));
    if (blocked)
      designed.add(
          new AgentTaskFunctionalSnapshot(
              99L,
              117L,
              "pde-construction-approval",
              "access",
              "landing-generator",
              "BLOCKED",
              Instant.EPOCH,
              null,
              "{\"decision\":\"BLOCKED\"}"));
    String ref = "experiment:" + cycle.getExperimentId();
    when(tasks.findFunctionalSnapshotsByProcessSince(117L, ref, Instant.EPOCH))
        .thenReturn(designed);
    when(tasks.findFunctionalSnapshotsByProcessSince(116L, ref, Instant.EPOCH))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    10L,
                    116L,
                    "pde-commercial-plan-offer",
                    "productArchitecture",
                    "landing-generator",
                    "COMPLETED",
                    Instant.EPOCH,
                    Instant.EPOCH,
                    new ObjectMapper()
                        .createObjectNode()
                        .put("decision", "APPROVE")
                        .set(
                            "productArchitecture",
                            new ObjectMapper()
                                .createObjectNode()
                                .put("strategyReference", profile)
                                .put("format", format)
                                .put("kitProfileCode", explicit))
                        .toString())));
    var result = new KitPrototypeCapabilities(tasks, chains, new ObjectMapper()).resolve(cycle);
    verify(tasks).findFunctionalSnapshotsByProcessSince(117L, ref, Instant.EPOCH);
    return result;
  }
}
