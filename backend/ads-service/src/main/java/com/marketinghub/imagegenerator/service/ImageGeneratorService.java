package com.marketinghub.imagegenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse.ImageGeneratorFailure;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse.ImageGeneratorResult;
import com.marketinghub.openai.OpenAiProperties;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerar imagens por IA em modo flex e auditar request/response da OpenAI. */
@Service
public class ImageGeneratorService {
  private static final Logger log = LoggerFactory.getLogger(ImageGeneratorService.class);
  private static final String SERVICE_TIER = "flex";
  private static final String OUTPUT_FORMAT = "png";
  private static final String PROMPT_TEMPLATE_PATH =
      "prompts/image-generator/user-image-generation.md";

  private final WebClient openAiWebClient;
  private final OpenAiProperties openAiProperties;
  private final ImageGenerationRequestRepository repository;
  private final ImageDerivativeService derivativeService;
  private final ProductRepository productRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final ExperimentRepository experimentRepository;
  private final StudioCostLedgerService costLedgerService;
  private final ObjectMapper objectMapper;
  private final String model;
  private final String comparisonImageModel;

  /** Inicializa o serviço com cliente OpenAI autenticado e repositório de auditoria. */
  @Autowired
  public ImageGeneratorService(
      @Qualifier("openAiWebClient") WebClient openAiWebClient,
      OpenAiProperties openAiProperties,
      ImageGenerationRequestRepository repository,
      ImageDerivativeService derivativeService,
      ProductRepository productRepository,
      CommercialPlanRepository commercialPlanRepository,
      ExperimentRepository experimentRepository,
      StudioCostLedgerService costLedgerService,
      ObjectMapper objectMapper,
      @Value("${image-generator.openai.model:gpt-5.6}") String model,
      @Value("${image-generator.openai.comparison-image-model:gpt-image-2}")
          String comparisonImageModel) {
    this.openAiWebClient = openAiWebClient;
    this.openAiProperties = openAiProperties;
    this.repository = repository;
    this.derivativeService = derivativeService;
    this.productRepository = productRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.experimentRepository = experimentRepository;
    this.costLedgerService = costLedgerService;
    this.objectMapper = objectMapper;
    this.model = model;
    this.comparisonImageModel = comparisonImageModel;
  }

  /** Inicializa a unidade de geração para testes puros de payload e extração. */
  ImageGeneratorService(
      WebClient openAiWebClient,
      OpenAiProperties openAiProperties,
      ImageGenerationRequestRepository repository,
      ImageDerivativeService derivativeService,
      ObjectMapper objectMapper,
      String model,
      String comparisonImageModel) {
    this(
        openAiWebClient,
        openAiProperties,
        repository,
        derivativeService,
        null,
        null,
        null,
        null,
        objectMapper,
        model,
        comparisonImageModel);
  }

  /**
   * Gera duas imagens comparativas a partir do prompt do usuário usando Responses API com
   * ferramenta de imagem.
   */
  public ImageGeneratorResponse generate(ImageGeneratorRequest request) {
    if (!openAiProperties.isEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "OpenAI não está configurada no backend.");
    }
    validateCommercialContext(request);

    String batchJobId = "img-batch-" + UUID.randomUUID();
    String finalPrompt = buildPrompt(request.prompt());
    CompletableFuture<ImageGeneratorResult> defaultModelGeneration =
        CompletableFuture.supplyAsync(() -> generateSingleImage(request, finalPrompt, model, null));
    CompletableFuture<ImageGeneratorResult> comparisonModelGeneration =
        CompletableFuture.supplyAsync(
            () ->
                generateSingleImage(
                    request, finalPrompt, comparisonImageModel, comparisonImageModel));

    ImageGenerationBatchResult batchResult =
        collectGenerationResults(
            List.of(
                new NamedGeneration(model, defaultModelGeneration),
                new NamedGeneration(comparisonImageModel, comparisonModelGeneration)));

    if (batchResult.images().isEmpty()) {
      ImageGeneratorFailure firstFailure =
          batchResult.failures().isEmpty()
              ? new ImageGeneratorFailure(
                  "desconhecido", "Não foi possível gerar as imagens.", Instant.now())
              : batchResult.failures().get(0);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, firstFailure.message());
    }

    return new ImageGeneratorResponse(batchJobId, batchResult.images(), batchResult.failures());
  }

  /**
   * Coleta sucessos e falhas individuais do lote para permitir exibir resultados parciais na tela.
   */
  ImageGenerationBatchResult collectGenerationResults(List<NamedGeneration> generations) {
    List<ImageGeneratorResult> images = new ArrayList<>();
    List<ImageGeneratorFailure> failures = new ArrayList<>();

    for (NamedGeneration generation : generations) {
      try {
        images.add(generation.future().join());
      } catch (RuntimeException ex) {
        failures.add(
            new ImageGeneratorFailure(
                generation.model(), extractGenerationErrorMessage(ex), Instant.now()));
      }
    }

    return new ImageGenerationBatchResult(List.copyOf(images), List.copyOf(failures));
  }

  /** Extrai a mensagem funcional da falha de uma geração individual. */
  private String extractGenerationErrorMessage(RuntimeException ex) {
    Throwable cause = ex.getCause();
    if (cause instanceof ResponseStatusException responseStatusException) {
      return responseStatusException.getReason();
    }
    if (ex instanceof ResponseStatusException responseStatusException) {
      return responseStatusException.getReason();
    }
    if (StringUtils.hasText(ex.getMessage())) {
      return ex.getMessage();
    }
    return "Não foi possível gerar esta imagem.";
  }

  /** Gera uma variação individual da imagem e registra auditoria da chamada OpenAI. */
  private ImageGeneratorResult generateSingleImage(
      ImageGeneratorRequest request,
      String finalPrompt,
      String outputModel,
      String imageToolModel) {
    String jobId = "img-" + UUID.randomUUID();
    Instant startedAt = Instant.now();
    Map<String, Object> requestBody = buildRequestBody(finalPrompt, imageToolModel);
    ImageGenerationRequest audit =
        repository.save(
            ImageGenerationRequest.builder()
                .jobId(jobId)
                .productId(request.productId())
                .commercialPlanId(request.commercialPlanId())
                .experimentId(request.experimentId())
                .status("RUNNING")
                .model(outputModel)
                .serviceTier(SERVICE_TIER)
                .outputFormat(OUTPUT_FORMAT)
                .prompt(request.prompt())
                .openAiRequestBody(writeJson(requestBody))
                .startedAt(startedAt)
                .build());
    costLedgerService.recordImage(
        jobId,
        request.productId(),
        request.commercialPlanId(),
        request.experimentId(),
        outputModel,
        "RUNNING",
        startedAt,
        null);

    try {
      JsonNode responseBody =
          openAiWebClient
              .post()
              .uri("/responses")
              .bodyValue(requestBody)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block();

      String rawResponse = writeJson(responseBody);
      String imageBase64 = extractImageBase64(responseBody);
      audit.setStatus("COMPLETED");
      audit.setOpenAiResponseBody(rawResponse);
      audit.setOpenAiResponseId(
          responseBody != null && responseBody.hasNonNull("id")
              ? responseBody.get("id").asText()
              : null);
      audit.setFinishedAt(Instant.now());
      repository.save(audit);
      costLedgerService.recordImage(
          jobId,
          request.productId(),
          request.commercialPlanId(),
          request.experimentId(),
          outputModel,
          "COMPLETED",
          startedAt,
          audit.getFinishedAt());

      return new ImageGeneratorResult(
          jobId,
          outputModel,
          SERVICE_TIER,
          OUTPUT_FORMAT,
          imageBase64,
          derivativeService.createVariants(OUTPUT_FORMAT, imageBase64),
          audit.getFinishedAt());
    } catch (RuntimeException ex) {
      String errorMessage = buildUserFacingErrorMessage(ex);
      audit.setStatus("FAILED");
      audit.setErrorMessage(errorMessage);
      audit.setFinishedAt(Instant.now());
      repository.save(audit);
      costLedgerService.recordImage(
          jobId,
          request.productId(),
          request.commercialPlanId(),
          request.experimentId(),
          outputModel,
          "FAILED",
          startedAt,
          audit.getFinishedAt());
      log.error(
          "Falha ao gerar imagem por IA. modulo=image-generator operacao=generate jobId={}",
          jobId,
          ex);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMessage, ex);
    }
  }

  /** Valida os vínculos comerciais antes de consumir crédito do provedor. */
  private void validateCommercialContext(ImageGeneratorRequest request) {
    if (request.productId() == null || request.commercialPlanId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Produto e plano comercial são obrigatórios para gerar ativos.");
    }
    if (!productRepository.existsById(request.productId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto não encontrado.");
    }
    var plan =
        commercialPlanRepository
            .findById(request.commercialPlanId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Plano comercial não encontrado."));
    if (request.experimentId() != null
        && !experimentRepository.existsById(request.experimentId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experimento não encontrado.");
    }
    if (plan.getExperiment() != null
        && request.experimentId() != null
        && !plan.getExperiment().getId().equals(request.experimentId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O experimento não pertence ao plano comercial selecionado.");
    }
  }

  /** Monta o prompt operacional versionado com o texto informado pelo usuário. */
  private String buildPrompt(String userPrompt) {
    try {
      ClassPathResource resource = new ClassPathResource(PROMPT_TEMPLATE_PATH);
      String template = resource.getContentAsString(StandardCharsets.UTF_8);
      return template.replace("{{USER_PROMPT}}", userPrompt.trim());
    } catch (IOException ex) {
      throw new UncheckedIOException(
          "Não foi possível carregar o prompt operacional de geração de imagem.", ex);
    }
  }

  /**
   * Monta o corpo da Responses API com service_tier flex e chamada obrigatória da ferramenta
   * image_generation.
   */
  Map<String, Object> buildRequestBody(String prompt) {
    return buildRequestBody(prompt, null);
  }

  /** Monta o corpo da Responses API podendo definir modelo específico na ferramenta de imagem. */
  Map<String, Object> buildRequestBody(String prompt, String imageToolModel) {
    Map<String, Object> imageTool =
        StringUtils.hasText(imageToolModel)
            ? Map.of(
                "type",
                "image_generation",
                "action",
                "generate",
                "model",
                imageToolModel,
                "output_format",
                OUTPUT_FORMAT)
            : Map.of(
                "type", "image_generation",
                "action", "generate",
                "output_format", OUTPUT_FORMAT);
    return Map.of(
        "model", model,
        "input", prompt,
        "service_tier", SERVICE_TIER,
        "tool_choice", Map.of("type", "image_generation"),
        "tools", List.of(imageTool));
  }

  /** Extrai a primeira imagem base64 retornada pela chamada image_generation_call. */
  String extractImageBase64(JsonNode responseBody) {
    JsonNode output = responseBody == null ? null : responseBody.get("output");
    if (output == null || !output.isArray()) {
      throw new IllegalStateException("OpenAI não retornou saída de imagem.");
    }
    for (JsonNode item : output) {
      if ("image_generation_call".equals(item.path("type").asText())
          && StringUtils.hasText(item.path("result").asText())) {
        return item.path("result").asText();
      }
    }
    throw new IllegalStateException("OpenAI não retornou imagem base64.");
  }

  /** Serializa objetos de auditoria em JSON para persistência e diagnóstico. */
  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException ex) {
      throw new UncheckedIOException("Não foi possível serializar payload de auditoria.", ex);
    }
  }

  /** Monta uma mensagem curta e acionável para a tela sem expor payload bruto ou credenciais. */
  private String buildUserFacingErrorMessage(RuntimeException ex) {
    if (ex instanceof WebClientResponseException responseException) {
      String providerMessage = extractProviderMessage(responseException.getResponseBodyAsString());
      if (StringUtils.hasText(providerMessage)) {
        return "OpenAI recusou a geração da imagem: " + providerMessage;
      }
      return "OpenAI recusou a geração da imagem com status "
          + responseException.getStatusCode().value()
          + ".";
    }
    if (StringUtils.hasText(ex.getMessage())) {
      return "Não foi possível gerar a imagem: " + ex.getMessage();
    }
    return "Não foi possível gerar a imagem. Tente novamente com um prompt mais objetivo.";
  }

  /** Extrai a mensagem de erro retornada pela OpenAI quando o corpo vier em JSON. */
  private String extractProviderMessage(String responseBody) {
    if (!StringUtils.hasText(responseBody)) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      return root.path("error").path("message").asText(null);
    } catch (IOException ex) {
      return null;
    }
  }

  /** Responsabilidade: associar o modelo solicitado ao futuro de geração correspondente. */
  record NamedGeneration(String model, CompletableFuture<ImageGeneratorResult> future) {}

  /** Responsabilidade: transportar sucessos e falhas de um lote comparativo de imagens. */
  record ImageGenerationBatchResult(
      List<ImageGeneratorResult> images, List<ImageGeneratorFailure> failures) {}
}
