package com.marketinghub.pde.visualpersonalization.v1.service;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.service.ImageGenerationUsageCost;
import com.marketinghub.pde.visualpersonalization.v1.service.contract.VisualPreparationContract.*;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: reservar a fila visual e aplicar respostas preservadas sem nova inferência. */
@Service
@Slf4j
public class VisualPreparationExecution {
  private final ImageGenerationRequestRepository images;
  private final ProductRepository products;
  private final VisualPreparationService preparations;
  private final VisualPreparationBudget budgets;
  private final StudioCostLedgerService ledger;
  private final ImageGenerationUsageCost usageCost;
  private final TransactionTemplate tx;
  private static final String IMAGE_MODEL = "gpt-image-2.5-sunburst";

  /** Configura persistência e transações separadas para preservar resposta antes da aplicação. */
  public VisualPreparationExecution(
      ImageGenerationRequestRepository images,
      ProductRepository products,
      VisualPreparationService preparations,
      VisualPreparationBudget budgets,
      StudioCostLedgerService ledger,
      ImageGenerationUsageCost usageCost,
      PlatformTransactionManager manager) {
    this.images = images;
    this.products = products;
    this.preparations = preparations;
    this.budgets = budgets;
    this.ledger = ledger;
    this.usageCost = usageCost;
    this.tx = new TransactionTemplate(manager);
  }

  /** Expõe pendências, respostas para replay e reservas sem request paga auditada. */
  public List<JsonNode> pending() {
    return tx.execute(
        status ->
            java.util.stream.Stream.of(
                    images
                        .findByStatusAndJobIdStartingWithOrderByCreatedAtAsc(
                            "PDE_RAW_RECEIVED", PREFIX, PageRequest.of(0, 4))
                        .stream(),
                    images
                        .findByStatusAndOpenAiRequestBodyIsNullAndJobIdStartingWithOrderByCreatedAtAsc(
                            "PDE_RUNNING", PREFIX, PageRequest.of(0, 4))
                        .stream(),
                    images
                        .findByStatusAndJobIdStartingWithOrderByCreatedAtAsc(
                            "PDE_QUEUED", PREFIX, PageRequest.of(0, 4))
                        .stream())
                .flatMap(stream -> stream)
                .limit(4)
                .map(this::workerView)
                .toList());
  }

  /** Reserva a tentativa uma vez antes da request e mantém o teto total do contexto. */
  public JsonNode claim(String jobId) {
    return tx.execute(
        status -> {
          var row = locked(jobId);
          require(
              List.of("PDE_QUEUED", "PDE_RUNNING").contains(row.getStatus())
                  && row.getOpenAiRequestBody() == null,
              "A tentativa já possui request auditada; não repita a geração.");
          try {
            var scope =
                preparations.scope(
                    row.getProductId(), row.getCommercialPlanId(), row.getExperimentId(), true);
            var budget =
                budgets.read(scope.product(), scope.plan(), row.getExperimentId(), row.getJobId());
            var context = (ObjectNode) preparations.read(row.getPrompt());
            require(
                context.path("authorizationHash").asText().equals(budget.authorizationHash())
                    && context.path("productVersion").asText().equals(budget.productVersion())
                    && budget.costComplete()
                    && budget.availableUsd().compareTo(new BigDecimal("0.50")) >= 0,
                budget.blocker() == null
                    ? "O saldo ou a autorização não cobre a homologação limitada."
                    : budget.blocker());
            context.put("reservedUsd", budget.availableUsd());
            row.setPrompt(preparations.write(context));
            row.setStatus("PDE_RUNNING");
            if (row.getStartedAt() == null) row.setStartedAt(Instant.now());
            images.saveAndFlush(row);
            ledger.recordImage(
                row.getJobId(),
                row.getProductId(),
                row.getCommercialPlanId(),
                row.getExperimentId(),
                IMAGE_MODEL,
                row.getStatus(),
                row.getStartedAt(),
                null);
            return workerView(row);
          } catch (ResponseStatusException ex) {
            log.warn(
                "Preparação visual bloqueada antes do provedor productId={} experimentId={} jobId={}",
                row.getProductId(),
                row.getExperimentId(),
                jobId,
                ex);
            row.setStatus("PDE_BLOCKED");
            row.setErrorMessage(ex.getReason());
            row.setFinishedAt(Instant.now());
            if (row.getStartedAt() != null && row.getOpenAiRequestBody() == null) {
              var context = (ObjectNode) preparations.read(row.getPrompt());
              context.put("providerCalled", false);
              context.put("estimatedCostUsd", BigDecimal.ZERO);
              context.put("costEvidence", "NO_PROVIDER_REQUEST");
              row.setPrompt(preparations.write(context));
              ledger.recordImage(
                  row.getJobId(),
                  row.getProductId(),
                  row.getCommercialPlanId(),
                  row.getExperimentId(),
                  IMAGE_MODEL,
                  row.getStatus(),
                  row.getStartedAt(),
                  row.getFinishedAt(),
                  BigDecimal.ZERO,
                  "NO_PROVIDER_REQUEST");
            }
            images.saveAndFlush(row);
            return workerView(row);
          }
        });
  }

  /** Audita a chamada limitada em Flex antes de o worker acessar o provedor. */
  public void request(String jobId, RequestAudit input) {
    tx.executeWithoutResult(
        status -> {
          var row = locked(jobId);
          require("PDE_RUNNING".equals(row.getStatus()), "A tentativa não está reservada.");
          var request = input.rawRequest();
          var savedContext = preparations.read(row.getPrompt());
          require(
              row.getModel().equals(request.path("model").asText())
                  && "flex".equals(request.path("service_tier").asText())
                  && request.path("max_tool_calls").asInt() == 1
                  && request.path("max_output_tokens").asInt() == 4096
                  && request.path("tools").size() == 1
                  && "image_generation".equals(request.path("tools").path(0).path("type").asText())
                  && "image_generation".equals(request.path("tool_choice").path("type").asText())
                  && IMAGE_MODEL.equals(request.path("tools").path(0).path("model").asText())
                  && "generate".equals(request.path("tools").path(0).path("action").asText())
                  && "png".equals(request.path("tools").path(0).path("output_format").asText())
                  && "1024x1024".equals(request.path("tools").path(0).path("size").asText())
                  && "high".equals(request.path("tools").path(0).path("quality").asText())
                  && jobId.equals(request.path("metadata").path("mh_job_id").asText())
                  && savedContext
                      .path("inputHash")
                      .asText()
                      .equals(request.path("metadata").path("mh_input_hash").asText())
                  && request.path("input").size() == 3
                  && "user".equals(request.path("input").path(2).path("role").asText())
                  && savedContext
                      .path("input")
                      .equals(
                          preparations.read(
                              request.path("input").path(2).path("content").asText())),
              "A chamada não corresponde ao contrato privado limitado de geração.");
          var scope =
              preparations.scope(
                  row.getProductId(), row.getCommercialPlanId(), row.getExperimentId(), true);
          var budget =
              budgets.read(scope.product(), scope.plan(), row.getExperimentId(), row.getJobId());
          var context = preparations.read(row.getPrompt());
          require(
              budget.costComplete()
                  && budget.authorizationHash().equals(context.path("authorizationHash").asText())
                  && budget.availableUsd().compareTo(context.path("reservedUsd").decimalValue())
                      >= 0,
              "O consumo ou a autorização mudou; não iniciar outra chamada.");
          require(
              row.getOpenAiRequestBody() == null,
              "A request já foi registrada; não repita a chamada paga.");
          row.setOpenAiRequestBody(preparations.write(request));
          images.saveAndFlush(row);
        });
  }

  /** Persiste o payload antes da validação; uma falha posterior pode reaplicar a mesma resposta. */
  public JsonNode receive(String jobId, Result input) {
    tx.executeWithoutResult(
        status -> {
          var row = locked(jobId);
          require(
              !input.providerCalled() || row.getOpenAiRequestBody() != null,
              "Resposta de provedor sem request auditada.");
          var context = (ObjectNode) preparations.read(row.getPrompt());
          if (input.providerCalled() && input.httpStatus() == 200) {
            require(
                jobId.equals(input.rawResponse().path("metadata").path("mh_job_id").asText())
                    && context
                        .path("inputHash")
                        .asText()
                        .equals(
                            input.rawResponse().path("metadata").path("mh_input_hash").asText()),
                "A resposta pertence a outra entrada ou execução; preserve-a e corrija a correlação do callback.");
          }
          if (row.getOpenAiResponseBody() != null) {
            require(
                preparations.read(row.getOpenAiResponseBody()).equals(input.rawResponse())
                    && context.path("providerCalled").asBoolean() == input.providerCalled()
                    && context.path("providerHttpStatus").asInt() == input.httpStatus(),
                "A tentativa já possui outra resposta; preserve a auditoria original.");
            return;
          }
          require("PDE_RUNNING".equals(row.getStatus()), "A tentativa não está em execução.");
          context.put("providerCalled", input.providerCalled());
          context.put("providerHttpStatus", input.httpStatus());
          row.setPrompt(preparations.write(context));
          row.setOpenAiResponseBody(preparations.write(input.rawResponse()));
          row.setOpenAiResponseId(input.rawResponse().path("id").asText(null));
          row.setStatus("PDE_RAW_RECEIVED");
          images.saveAndFlush(row);
        });
    return apply(jobId);
  }

  /** Reaplica resposta auditada, custo e entrega; nunca chama o modelo. */
  public JsonNode apply(String jobId) {
    try {
      return tx.execute(
          status -> {
            var row = locked(jobId);
            if (List.of("PDE_COMPLETED", "PDE_FAILED", "PDE_OVER_BUDGET").contains(row.getStatus()))
              return preparations.view(row);
            require(
                List.of("PDE_RAW_RECEIVED", "PDE_COST_PENDING").contains(row.getStatus()),
                "Ainda não há resposta preservada para reaplicar.");
            var context = (ObjectNode) preparations.read(row.getPrompt());
            var raw = preparations.read(row.getOpenAiResponseBody());
            var estimate = usageCost.estimate(raw, IMAGE_MODEL);
            BigDecimal cost =
                context.path("providerCalled").asBoolean()
                    ? estimate.map(ImageGenerationUsageCost.Estimate::estimatedCostUsd).orElse(null)
                    : BigDecimal.ZERO;
            String evidence =
                context.path("providerCalled").asBoolean()
                    ? estimate.map(ImageGenerationUsageCost.Estimate::evidence).orElse(null)
                    : "NO_PROVIDER_REQUEST";
            boolean valid =
                context.path("providerCalled").asBoolean()
                    && context.path("providerHttpStatus").asInt() == 200
                    && validImage(raw, row.getJobId());
            context.put("imageValidated", valid);
            row.setStatus(valid ? "PDE_COMPLETED" : "PDE_FAILED");
            row.setErrorMessage(
                valid
                    ? null
                    : "A resposta preservada não entregou uma imagem válida; não regenerar automaticamente.");
            if (cost == null) {
              row.setStatus("PDE_COST_PENDING");
              row.setErrorMessage(
                  "A resposta está preservada, mas o custo ainda precisa de conciliação; nova inferência bloqueada.");
            } else {
              context.put("estimatedCostUsd", cost);
              context.put("costEvidence", evidence);
              if (context.path("providerCalled").asBoolean()
                  && cost.compareTo(context.path("reservedUsd").decimalValue()) > 0) {
                row.setStatus("PDE_OVER_BUDGET");
                row.setErrorMessage(
                    "O custo excedeu a reserva; preservar resposta e interromper novas chamadas.");
              }
            }
            row.setPrompt(preparations.write(context));
            row.setFinishedAt(Instant.now());
            images.saveAndFlush(row);
            ledger.recordImage(
                row.getJobId(),
                row.getProductId(),
                row.getCommercialPlanId(),
                row.getExperimentId(),
                IMAGE_MODEL,
                row.getStatus(),
                row.getStartedAt(),
                row.getFinishedAt(),
                cost,
                evidence);
            return preparations.view(row);
          });
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao aplicar resposta visual PDE jobId={}; payload preservado para replay.",
          jobId,
          ex);
      throw ex;
    }
  }

  /** Bloqueia produto antes da tentativa para manter a ordem única dos locks da preparação. */
  private ImageGenerationRequest locked(String jobId) {
    var candidate =
        images.findFirstByJobId(jobId).orElseThrow(() -> fail(404, "Tentativa não encontrada."));
    require(jobId.startsWith(PREFIX), "A tentativa não pertence à fila PDE.");
    products.findLockedById(candidate.getProductId()).orElseThrow();
    return images.findByJobId(jobId).orElseThrow();
  }

  /** Entrega ao worker contexto funcional sem credencial privada ou evidência humana fabricada. */
  private JsonNode workerView(ImageGenerationRequest row) {
    ObjectNode result = preparations.view(row);
    result.remove("imageBase64");
    result.put("model", row.getModel());
    if ("PDE_RAW_RECEIVED".equals(row.getStatus())) result.put("replayOnly", true);
    return result;
  }

  /** Exige exatamente uma imagem PNG íntegra no formato contratado antes do sucesso funcional. */
  private boolean validImage(JsonNode raw, String jobId) {
    try {
      if (!"completed".equals(raw.path("status").asText()) || raw.path("id").asText().isBlank())
        return false;
      String encoded = null;
      for (var item : raw.path("output")) {
        if (!"image_generation_call".equals(item.path("type").asText())) continue;
        if (encoded != null || !"completed".equals(item.path("status").asText())) return false;
        encoded = item.path("result").asText();
      }
      if (encoded == null || encoded.length() > 20_000_000) return false;
      byte[] bytes = Base64.getDecoder().decode(encoded);
      if (bytes.length < 8
          || bytes[0] != (byte) 137
          || bytes[1] != 80
          || bytes[2] != 78
          || bytes[3] != 71) return false;
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      return image != null && image.getWidth() == 1024 && image.getHeight() == 1024;
    } catch (Exception ex) {
      log.warn("Imagem inválida na resposta privada PDE jobId={}; payload preservado.", jobId, ex);
      return false;
    }
  }
}
