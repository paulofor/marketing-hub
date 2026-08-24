package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Responsabilidade: criar e editar imagens premium com GPT Image 2 dentro de Têmis. */
@Component
@ConditionalOnProperty(name = "meta-ad-approver.execution-role", havingValue = "image-studio")
public class TemisImageStudioOpenAiClient {
  private static final Logger log = LoggerFactory.getLogger(TemisImageStudioOpenAiClient.class);
  private static final int MAX_REFERENCE_BYTES = 20 * 1024 * 1024;
  private static final String PRODUCTION_PROMPT = "prompts/image-studio/v1/production.md";
  private static final BigDecimal TOKENS_PER_MILLION = BigDecimal.valueOf(1_000_000);
  private static final BigDecimal IMAGE_INPUT_USD_PER_MILLION = BigDecimal.valueOf(8);
  private static final BigDecimal TEXT_INPUT_USD_PER_MILLION = BigDecimal.valueOf(5);
  private static final BigDecimal IMAGE_OUTPUT_USD_PER_MILLION = BigDecimal.valueOf(30);
  private static final BigDecimal TEXT_OUTPUT_USD_PER_MILLION = BigDecimal.valueOf(10);
  private final MetaAdApproverProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient downloadClient;

  /** Inicializa o cliente visual com timeout compatível com a latência documentada do modelo. */
  public TemisImageStudioOpenAiClient(
      MetaAdApproverProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.downloadClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  }

  /** Monta o cliente somente ao executar para não exigir segredo em testes sem geração. */
  private RestClient openAiClient() {
    JdkClientHttpRequestFactory requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    requestFactory.setReadTimeout(properties.getImageTimeout());
    return RestClient.builder()
        .baseUrl(normalizeBaseUrl(properties.getOpenAiBaseUrl()))
        .requestFactory(requestFactory)
        .defaultHeader("Authorization", "Bearer " + resolveApiKey(properties))
        .build();
  }

  /** Gera do zero ou edita referências, preservando request, response e usage brutos. */
  public Result execute(TemisImageStudioJob job) {
    String model = canonicalModel(properties.getImageModel());
    List<ReferenceImage> references = downloadReferences(job.referenceImageUrls());
    String requestJson = requestAudit(job, model, references);
    String endpoint = references.isEmpty() ? "/images/generations" : "/images/edits";
    log.info(
        "Têmis enviando request de imagem. jobId={} operation={} url={} model={} references={} request={}",
        job.jobId(),
        job.operation(),
        normalizeBaseUrl(properties.getOpenAiBaseUrl()) + endpoint,
        model,
        references.size(),
        requestJson);
    String raw = references.isEmpty() ? generate(job, model) : edit(job, model, references);
    try {
      JsonNode response = objectMapper.readTree(raw);
      String encoded = response.path("data").path(0).path("b64_json").asText();
      if (!StringUtils.hasText(encoded)) {
        throw new IllegalStateException("GPT Image 2 não retornou a imagem em base64");
      }
      byte[] image = Base64.getDecoder().decode(encoded);
      String usage = response.has("usage") ? response.path("usage").toString() : null;
      BigDecimal costUsd = calculateCost(response.path("usage"));
      String responseAudit = responseAudit(response, image);
      log.info(
          "Têmis recebeu response de imagem. jobId={} url={} responseBytes={} response={}",
          job.jobId(),
          normalizeBaseUrl(properties.getOpenAiBaseUrl()) + endpoint,
          raw.getBytes(StandardCharsets.UTF_8).length,
          responseAudit);
      return new Result(image, model, requestJson, responseAudit, usage, costUsd);
    } catch (IOException | IllegalArgumentException ex) {
      throw new IllegalStateException("Resposta do GPT Image 2 inválida", ex);
    }
  }

  /** Executa geração sem referências pela Image API. */
  private String generate(TemisImageStudioJob job, String model) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", model);
    payload.put("prompt", productionPrompt(job));
    payload.put("size", job.size());
    payload.put("quality", job.quality());
    payload.put("output_format", "png");
    return openAiClient()
        .post()
        .uri("/images/generations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(String.class);
  }

  /** Executa edição ou composição híbrida com uma a quatro referências reais. */
  private String edit(TemisImageStudioJob job, String model, List<ReferenceImage> references) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("model", model);
    body.add("prompt", productionPrompt(job));
    body.add("size", job.size());
    body.add("quality", job.quality());
    body.add("output_format", "png");
    for (int index = 0; index < references.size(); index++) {
      ReferenceImage reference = references.get(index);
      int number = index + 1;
      body.add(
          "image[]",
          new ByteArrayResource(reference.bytes()) {
            @Override
            public String getFilename() {
              return "reference-" + number + "." + reference.extension();
            }
          });
    }
    return openAiClient()
        .post()
        .uri("/images/edits")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        .body(String.class);
  }

  /** Resolve o prompt versionado com o contrato específico do job reservado pelo backend. */
  private String productionPrompt(TemisImageStudioJob job) {
    try (var input = new ClassPathResource(PRODUCTION_PROMPT).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8)
          .replace("{{JOB_PROMPT}}", job.prompt().trim())
          .replace("{{PURPOSES}}", String.join(", ", job.purposes()))
          .replace("{{ASSET_ROLE_CONTRACT}}", assetRoleContract(job))
          .replace("{{LEARNING_PLAYBOOK}}", playbookText(job.visualPlaybook()))
          .replace(
              "{{FORMAT_CONSTRAINT}}",
              job.label().toLowerCase(java.util.Locale.ROOT).contains("story")
                  ? "- Formato Story obrigatório: componha todo o quadro nativo 9:16. Quando a referência tiver outra proporção, expanda organicamente fundo e fotografia e reorganize somente o necessário; nunca acrescente barras, áreas vazias ou preenchimento artificial."
                  : "- Preserve a proporção e o enquadramento funcional solicitados pelo job.")
          .replace(
              "{{EDIT_CONSTRAINT}}",
              "EDIT".equalsIgnoreCase(job.operation())
                  ? "- Esta é uma edição: mantenha todas as regiões não citadas sem mudanças perceptíveis."
                  : job.referenceImageUrls().isEmpty()
                      ? "- Esta é uma criação sem arquivo-base; não simule uma referência inexistente."
                      : "- Esta é uma criação orientada por exemplos premium aprovados: use-os como prova e linguagem visual, preserve o produto real e não os apresente como um arquivo-base fictício.");
    } catch (IOException ex) {
      log.error(
          "Falha ao carregar prompt versionado do Estúdio Visual. resource={}",
          PRODUCTION_PROMPT,
          ex);
      throw new IllegalStateException("Prompt do Estúdio Visual indisponível", ex);
    }
  }

  /** Diferencia entregável real de peça comercial sem permitir prova inventada do produto. */
  private String assetRoleContract(TemisImageStudioJob job) {
    if (job.purposes().contains("DELIVERY")) {
      return "- Este arquivo é um entregável real do produto e deve ser útil para a cliente final.";
    }
    return "- Este arquivo é uma peça comercial, não um entregável. Demonstre somente o produto "
        + "comprovado nas referências aprovadas, sem inventar tela, resultado, depoimento ou recurso.";
  }

  /** Baixa referências autorizadas e bloqueia payloads fora do contrato de imagem. */
  private List<ReferenceImage> downloadReferences(List<String> urls) {
    List<ReferenceImage> result = new ArrayList<>();
    for (String url : urls.stream().distinct().limit(4).toList()) {
      try {
        HttpResponse<byte[]> response =
            downloadClient.send(
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray());
        String contentType = response.headers().firstValue("content-type").orElse("");
        if (response.statusCode() / 100 != 2
            || !contentType.toLowerCase().startsWith("image/")
            || response.body().length == 0
            || response.body().length > MAX_REFERENCE_BYTES) {
          throw new IllegalArgumentException("Referência visual inválida: " + url);
        }
        result.add(
            new ReferenceImage(
                response.body(), extension(contentType), sha256(response.body()), url));
      } catch (IOException ex) {
        log.error("Falha ao baixar referência do Estúdio de Imagens. url={}", url, ex);
        throw new IllegalStateException("Não foi possível baixar referência visual", ex);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        log.error("Download de referência interrompido. url={}", url, ex);
        throw new IllegalStateException("Download de referência interrompido", ex);
      }
    }
    return result;
  }

  /** Cria auditoria sem duplicar os bytes das referências dentro do request persistido. */
  private String requestAudit(
      TemisImageStudioJob job, String model, List<ReferenceImage> references) {
    try {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("model", model);
      value.put("operation", references.isEmpty() ? "generate" : "edit");
      value.put("prompt", productionPrompt(job));
      value.put("size", job.size());
      value.put("quality", job.quality());
      value.put("purposes", job.purposes());
      value.put("visualPlaybook", job.visualPlaybook().audit());
      value.put("output_format", "png");
      value.put(
          "references",
          references.stream()
              .map(reference -> Map.of("url", reference.url(), "sha256", reference.sha256()))
              .toList());
      return objectMapper.writeValueAsString(value);
    } catch (IOException ex) {
      throw new IllegalStateException("Não foi possível auditar request do GPT Image 2", ex);
    }
  }

  /** Separa o binário do JSON e preserva metadados, tamanho e hash para auditoria. */
  private String responseAudit(JsonNode response, byte[] image) throws IOException {
    JsonNode audit = response.deepCopy();
    JsonNode firstImage = audit.path("data").path(0);
    if (firstImage instanceof ObjectNode imageNode) {
      imageNode.put("b64_json", "[BINÁRIO PERSISTIDO SEPARADAMENTE]");
      imageNode.put("image_sha256", sha256(image));
      imageNode.put("image_bytes", image.length);
    }
    return objectMapper.writeValueAsString(audit);
  }

  /** Calcula o custo auditável pelas modalidades detalhadas retornadas pelo GPT Image 2. */
  private BigDecimal calculateCost(JsonNode usage) {
    JsonNode input = usage.path("input_tokens_details");
    JsonNode output = usage.path("output_tokens_details");
    if (!input.isObject() || !output.isObject()) {
      return null;
    }
    BigDecimal total =
        tokenCost(input.path("image_tokens").asLong(0), IMAGE_INPUT_USD_PER_MILLION)
            .add(tokenCost(input.path("text_tokens").asLong(0), TEXT_INPUT_USD_PER_MILLION))
            .add(tokenCost(output.path("image_tokens").asLong(0), IMAGE_OUTPUT_USD_PER_MILLION))
            .add(tokenCost(output.path("text_tokens").asLong(0), TEXT_OUTPUT_USD_PER_MILLION));
    return total.setScale(8, RoundingMode.HALF_UP);
  }

  /** Converte tokens de uma modalidade em dólares pela tabela canônica por milhão. */
  private BigDecimal tokenCost(long tokens, BigDecimal ratePerMillion) {
    return BigDecimal.valueOf(Math.max(0, tokens))
        .multiply(ratePerMillion)
        .divide(TOKENS_PER_MILLION, 12, RoundingMode.HALF_UP);
  }

  /** Resolve a chave direta ou o arquivo secreto sem expor o valor. */
  private String resolveApiKey(MetaAdApproverProperties value) {
    if (StringUtils.hasText(value.getOpenAiApiKey())) {
      return value.getOpenAiApiKey().trim();
    }
    if (StringUtils.hasText(value.getOpenAiApiKeyFile())) {
      try {
        String key = Files.readString(Path.of(value.getOpenAiApiKeyFile())).trim();
        if (StringUtils.hasText(key)) {
          return key;
        }
      } catch (IOException ex) {
        log.error(
            "Falha ao ler o arquivo seguro da chave OpenAI. path={}",
            value.getOpenAiApiKeyFile(),
            ex);
        throw new IllegalStateException("Chave da OpenAI indisponível para Têmis", ex);
      }
    }
    throw new IllegalStateException("Chave da OpenAI indisponível para Têmis");
  }

  /** Impede downgrade silencioso do modelo visual aprovado. */
  private String canonicalModel(String value) {
    if (!"gpt-image-2".equals(StringUtils.hasText(value) ? value.trim() : "")) {
      throw new IllegalArgumentException("Têmis exige o modelo visual gpt-image-2");
    }
    return "gpt-image-2";
  }

  /** Normaliza a base sem barra final para registrar a URL exata. */
  private String normalizeBaseUrl(String value) {
    return value == null ? "" : value.replaceAll("/+$", "");
  }

  /** Resolve extensão segura pelo content type. */
  private String extension(String contentType) {
    String normalized = contentType.toLowerCase();
    if (normalized.contains("jpeg")) return "jpg";
    if (normalized.contains("webp")) return "webp";
    return "png";
  }

  /** Calcula fingerprint auditável sem persistir novamente o binário de entrada. */
  private String sha256(byte[] bytes) {
    try {
      return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Formata somente regras promovidas e exemplos aprovados para o prompt operacional. */
  private String playbookText(TemisVisualPlaybook playbook) {
    StringBuilder value = new StringBuilder();
    value.append("Versão: ").append(playbook.version()).append('\n');
    value.append("Contexto: ").append(playbook.contextKey()).append('\n');
    value.append("Regras válidas:\n");
    playbook.promotedRules().forEach(rule -> value.append("- ").append(rule).append('\n'));
    value.append("Evitar:\n");
    playbook.avoid().forEach(rule -> value.append("- ").append(rule).append('\n'));
    value.append("Exemplos premium aprovados:\n");
    playbook
        .approvedExamples()
        .forEach(
            example ->
                value
                    .append("- assetId=")
                    .append(example.assetId())
                    .append("; formato=")
                    .append(example.format())
                    .append("; rótulo=")
                    .append(example.label())
                    .append('\n'));
    return value.toString().trim();
  }

  /** Resultado técnico da chamada visual entregue ao backend. */
  public record Result(
      byte[] imageBytes,
      String model,
      String requestJson,
      String responseJson,
      String usageJson,
      BigDecimal costUsd) {}

  /** Referência baixada e validada para a edição multimodal. */
  private record ReferenceImage(byte[] bytes, String extension, String sha256, String url) {}
}
