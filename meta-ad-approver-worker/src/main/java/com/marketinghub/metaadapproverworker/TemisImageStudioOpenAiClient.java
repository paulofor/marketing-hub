package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
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
    log.info(
        "Têmis recebeu response de imagem. jobId={} url={} responseBytes={} response={}",
        job.jobId(),
        normalizeBaseUrl(properties.getOpenAiBaseUrl()) + endpoint,
        raw.getBytes(StandardCharsets.UTF_8).length,
        responseAuditForLog(raw));
    try {
      JsonNode response = objectMapper.readTree(raw);
      String encoded = response.path("data").path(0).path("b64_json").asText();
      if (!StringUtils.hasText(encoded)) {
        throw new IllegalStateException("GPT Image 2 não retornou a imagem em base64");
      }
      byte[] image = Base64.getDecoder().decode(encoded);
      String usage = response.has("usage") ? response.path("usage").toString() : null;
      return new Result(image, model, requestJson, raw, usage, null);
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
          .replace(
              "{{EDIT_CONSTRAINT}}",
              "EDIT".equalsIgnoreCase(job.operation())
                  ? "- Esta é uma edição: mantenha todas as regiões não citadas sem mudanças perceptíveis."
                  : "- Esta é uma criação sem arquivo-base; não simule uma referência inexistente.");
    } catch (IOException ex) {
      log.error(
          "Falha ao carregar prompt versionado do Estúdio Visual. resource={}",
          PRODUCTION_PROMPT,
          ex);
      throw new IllegalStateException("Prompt do Estúdio Visual indisponível", ex);
    }
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

  /** Remove somente o binário base64 do log e preserva metadados e usage da resposta. */
  private String responseAuditForLog(String raw) {
    return raw.replaceAll(
        "\\\"b64_json\\\"\\s*:\\s*\\\"[^\\\"]*\\\"",
        "\\\"b64_json\\\":\\\"[BINÁRIO PERSISTIDO SEPARADAMENTE]\\\"");
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
