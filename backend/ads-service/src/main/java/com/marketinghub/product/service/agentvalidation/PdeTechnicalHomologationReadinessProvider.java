package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import java.net.URI;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Responsabilidade: impedir homologação técnica sem um protótipo privado aceito e executável. */
@Service
@Slf4j
public class PdeTechnicalHomologationReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private final AgentTaskTargetContextProvider targets;

  /** Usa a mesma resolução de identidade e versão entregue ao executor pela fila. */
  public PdeTechnicalHomologationReadinessProvider(AgentTaskTargetContextProvider targets) {
    this.targets = targets;
  }

  /** Restringe a verificação à homologação técnica das versões multiagente do processo. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && "pde-construction-approval".equals(process.getProcessCode())
        && process.getVersionNumber() != null
        && process.getVersionNumber() >= 7
        && "technicalHomologation".equals(activity.getActivityId());
  }

  /** Distingue especificação concluída de implementação aceita antes de permitir nova tarefa. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    if (!supports(process, activity) || product == null || product.getId() == null) {
      return blocked(
          "O produto e a atividade de homologação técnica precisam estar identificados.");
    }
    try {
      var target = targets.resolve(sourceReference, process.getProcessCode()).orElse(null);
      if (target == null || target.publicUrl() == null || target.publicUrl().isBlank()) {
        return blocked(
            "O protótipo desta passagem ainda não possui uma URL executável aceita. "
                + "As especificações concluídas por Dédalo não comprovam implementação. "
                + "Conclua a implementação da versão do ciclo e registre sua aceitação privada "
                + "antes de executar Psique com o harness. Repetir a tarefa agora não resolve a pendência.");
      }
      if (!Objects.equals(product.getId(), target.productId())
          || target.productSlug() == null
          || target.productSlug().isBlank()
          || !Objects.equals(product.getSlug(), target.productSlug())
          || !Objects.equals(sourceReference, target.sourceReference())) {
        return blocked("O alvo da homologação diverge do produto ou da passagem selecionada.");
      }
      String expectedReference = "product:" + product.getId() + "@agent-validation-v1";
      JsonNode lineage = target.pdeContext() == null ? null : target.pdeContext().path("lineage");
      boolean cycleReference =
          target.experimentId() != null
              && ("experiment:" + target.experimentId()).equals(sourceReference)
              && lineage != null
              && lineage.path("learningCycleId").asLong() > 0
              && lineage.path("experimentId").asLong() == target.experimentId()
              && lineage.path("productId").asLong() == target.productId();
      if (!expectedReference.equals(sourceReference) && !cycleReference) {
        return blocked(
            "Esta passagem ainda não possui um contrato de homologação compatível com o executor. "
                + "Conclua a integração do protótipo com o ciclo antes de abrir outra tarefa; "
                + "não reutilize a referência ou as provas de um ciclo anterior.");
      }
      URI uri = URI.create(target.publicUrl());
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getRawQuery() != null
          || uri.getRawFragment() != null) {
        return blocked(
            "Registre uma URL HTTPS do protótipo privado, sem credenciais ou parâmetros.");
      }
      JsonNode acceptance =
          target.pdeContext() == null
              ? null
              : target.pdeContext().path("privatePrototypeAcceptance");
      if (acceptance == null
          || !"READY".equals(acceptance.path("status").asText())
          || target.experienceVersion() == null
          || target.experienceVersion().isBlank()
          || !target.experienceVersion().equals(acceptance.path("prototypeVersion").asText())
          || !target.publicUrl().equals(acceptance.path("privateAccessUrl").asText())) {
        return blocked(
            "A aceitação privada precisa comprovar a mesma URL e versão entregue à homologação. "
                + "Conclua a implementação e registre essa aceitação antes de iniciar os testes.");
      }
      return new AgentProductProcessActivityReadiness(
          true,
          "O protótipo privado aceito possui identidade, URL e versão coerentes para os testes.");
    } catch (Exception ex) {
      log.error(
          "Falha ao validar entrada da homologação técnica. productId={} processDefinitionId={} sourceReference={}",
          product.getId(),
          process.getId(),
          sourceReference,
          ex);
      return blocked(
          "O contrato do protótipo não pôde ser validado. Corrija os dados de implementação "
              + "e aceitação da versão antes de abrir outra tarefa.");
    }
  }

  /** Expõe a pendência funcional na tela sem criar tarefa ou alterar evidências anteriores. */
  private AgentProductProcessActivityReadiness blocked(String reason) {
    return new AgentProductProcessActivityReadiness(false, reason);
  }
}
