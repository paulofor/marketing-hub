package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Responsabilidade: reconciliar a versão privada publicada ao concluir o acesso do PDE. */
@Component
@Slf4j
public class PdeAgentValidationRuntimeAcceptanceHook implements AgentTaskCompletionHook {
  private static final Pattern SOURCE_REFERENCE =
      Pattern.compile("^product:([1-9][0-9]*)@agent-validation-v1$");
  private static final String PROCESS_CODE = "pde-construction-approval";
  private static final String ACTIVITY_ID = "access";
  private final ProductRepository products;
  private final ObjectMapper json;
  private final PdeAgentValidationRuntimeProbe runtimeProbe;
  private final Clock clock;

  /** Configura persistência e prova externa usando o relógio operacional UTC. */
  @Autowired
  public PdeAgentValidationRuntimeAcceptanceHook(
      ProductRepository products, ObjectMapper json, PdeAgentValidationRuntimeProbe runtimeProbe) {
    this(products, json, runtimeProbe, Clock.systemUTC());
  }

  /** Permite testes determinísticos do instante registrado na reconciliação. */
  PdeAgentValidationRuntimeAcceptanceHook(
      ProductRepository products,
      ObjectMapper json,
      PdeAgentValidationRuntimeProbe runtimeProbe,
      Clock clock) {
    this.products = products;
    this.json = json;
    this.runtimeProbe = runtimeProbe;
    this.clock = clock;
  }

  /** Restringe o efeito ao acesso inicial de Dédalo no fluxo multiagente do produto. */
  @Override
  public boolean supports(AgentTask task) {
    return task.getProcessDefinition() != null
        && PROCESS_CODE.equals(task.getProcessDefinition().getProcessCode())
        && ACTIVITY_ID.equals(task.getProcessActivityId())
        && task.getAssignedAgent() != null
        && "landing-generator".equals(task.getAssignedAgent().getAgentKey())
        && task.getSourceReference() != null
        && SOURCE_REFERENCE.matcher(task.getSourceReference()).matches();
  }

  /**
   * Confirma o runtime real e troca atomicamente a identidade aceita antes da próxima atividade.
   */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    try {
      JsonNode result = json.readTree(request.resultJson());
      if (!"READY".equals(result.path("decision").asText())) {
        throw new IllegalArgumentException("Dédalo não confirmou a prontidão do acesso privado.");
      }
      Long productId = productId(task.getSourceReference());
      Product product =
          products
              .findLockedById(productId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Produto do runtime não encontrado."));
      validateProduct(product);
      ObjectNode validation = object(product.getValidationDefinitionJson(), productId);
      ObjectNode experience = object(product.getPdeExperienceJson(), productId);
      JsonNode currentAcceptance = validation.path("privatePrototypeAcceptance");
      String privateAccessUrl = currentAcceptance.path("privateAccessUrl").asText("").trim();
      if (!"READY".equals(currentAcceptance.path("status").asText())
          || privateAccessUrl.isBlank()) {
        throw new IllegalArgumentException(
            "O produto não possui uma origem privada previamente aceita para reconciliação.");
      }
      var identity = runtimeProbe.probe(privateAccessUrl, product.getId(), product.getSlug());
      Instant reconciledAt = clock.instant();
      writeAcceptance(
          validation.withObject("/privatePrototypeAcceptance"),
          task,
          privateAccessUrl,
          identity,
          reconciledAt);
      writeAcceptance(
          experience.withObject("/privatePrototypeAcceptance"),
          task,
          privateAccessUrl,
          identity,
          reconciledAt);
      writeDeploymentEvidence(validation, identity, reconciledAt);
      product.setValidationDefinitionJson(write(validation, productId));
      product.setPdeExperienceJson(write(experience, productId));
      products.save(product);
      log.info(
          "Runtime privado reconciliado antes da homologação. productId={} taskId={} version={} commitSha={}",
          productId,
          task.getId(),
          identity.prototypeVersion(),
          identity.commitSha());
      return CompletionDisposition.COMPLETE;
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao reconciliar runtime no callback de Dédalo. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler contratos do runtime no callback de Dédalo. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalArgumentException(
          "Não foi possível reconciliar a identidade do runtime privado.", ex);
    }
  }

  /** Extrai a identidade numérica sem aceitar outras referências operacionais. */
  private Long productId(String sourceReference) {
    Matcher matcher = SOURCE_REFERENCE.matcher(sourceReference);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("A referência não pertence à validação inicial do PDE.");
    }
    return Long.valueOf(matcher.group(1));
  }

  /** Exige o contrato multiagente e mantém efeitos comerciais desligados durante a troca. */
  private void validateProduct(Product product) {
    if (!"PLANNED".equals(product.getCommercialStatus())
        || !"PDE_AGENT_VALIDATION_V1".equals(product.getValidationDefinitionVersion())
        || product.getSlug() == null
        || product.getSlug().isBlank()) {
      throw new IllegalArgumentException(
          "O produto não está no contrato planejado de validação multiagente.");
    }
  }

  /** Atualiza somente a identidade executável e preserva fontes e limites já congelados. */
  private void writeAcceptance(
      ObjectNode acceptance,
      AgentTask task,
      String privateAccessUrl,
      PdeAgentValidationRuntimeProbe.RuntimeIdentity identity,
      Instant reconciledAt) {
    acceptance.put("status", "READY");
    acceptance.put("prototypeVersion", identity.prototypeVersion());
    acceptance.put("privateAccessUrl", privateAccessUrl);
    acceptance.put(
        "instrumentationReference",
        "PDE_AGENT_VALIDATION_RUNTIME_V1:" + identity.frontendSourceSha256());
    acceptance.put("acceptanceEvidenceReference", "agent-task:" + task.getId());
    acceptance.put("acceptedAt", reconciledAt.toString());
    acceptance.put("runtimeReconciliationVersion", "PDE_AGENT_VALIDATION_RUNTIME_V1");
    acceptance.put("runtimeCommitSha", identity.commitSha());
    acceptance.put("runtimeSourceSha256", identity.frontendSourceSha256());
    acceptance.put("runtimeImage", identity.image());
    acceptance.put("runtimeDeployedAt", identity.deployedAt().toString());
    acceptance.put("intakeConsentVersion", identity.intakeConsentVersion());
    acceptance.put("privateAccessConfirmed", true);
    acceptance.put("paymentEnabled", false);
    acceptance.put("published", false);
    acceptance.put("mediaSpendBrl", 0);
    acceptance.put("eventSource", "FIRST_PARTY_EVENTS");
    acceptance.put("testMarker", "AGENT_VALIDATION");
  }

  /** Persiste o diagnóstico bruto necessário para o alvo e para a auditoria do próximo gate. */
  private void writeDeploymentEvidence(
      ObjectNode validation,
      PdeAgentValidationRuntimeProbe.RuntimeIdentity identity,
      Instant reconciledAt) {
    ObjectNode evidence = validation.putObject("technicalDeploymentEvidence");
    evidence.put("contractVersion", "PDE_TECHNICAL_DEPLOYMENT_EVIDENCE_V1");
    evidence.put("httpStatus", 200);
    evidence.put("observedAt", reconciledAt.toString());
    evidence.set("diagnosticSnapshot", identity.diagnosticSnapshot().deepCopy());
  }

  /** Lê um contrato JSON mutável e rejeita payload ausente ou estruturalmente inválido. */
  private ObjectNode object(String raw, Long productId) {
    try {
      JsonNode node = raw == null ? null : json.readTree(raw);
      if (node instanceof ObjectNode object) return object;
    } catch (Exception ex) {
      log.error("Contrato JSON inválido durante reconciliação. productId={}", productId, ex);
      throw new IllegalArgumentException("O contrato do produto não contém JSON válido.", ex);
    }
    throw new IllegalArgumentException("O produto não contém o contrato JSON obrigatório.");
  }

  /** Serializa o contrato reconciliado sem tolerar perda silenciosa da evidência. */
  private String write(ObjectNode value, Long productId) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar reconciliação do produto. productId={}", productId, ex);
      throw new IllegalStateException("Não foi possível persistir a reconciliação do runtime.", ex);
    }
  }
}
