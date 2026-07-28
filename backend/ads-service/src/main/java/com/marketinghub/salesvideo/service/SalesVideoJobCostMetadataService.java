package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Mantem metadados de custo estimado dos jobs de vídeo para auditoria comercial. */
@Component
public class SalesVideoJobCostMetadataService {
  private static final BigDecimal ZERO_COST = new BigDecimal("0.0000");

  private final ObjectMapper objectMapper;
  private final SalesVideoProductionCostCalculator costCalculator;

  /** Inicializa o serviço com parser JSON e calculadora de tabela de provedores. */
  public SalesVideoJobCostMetadataService(
      ObjectMapper objectMapper, SalesVideoProductionCostCalculator costCalculator) {
    this.objectMapper = objectMapper;
    this.costCalculator = costCalculator;
  }

  /** Enriquece o DTO de job com custo estimado quando o metadata original não informa custo. */
  public SalesVideoJobDto enrichDto(SalesVideoJobDto dto, SalesVideoJob job) {
    if (dto == null || job == null) {
      return dto;
    }
    dto.setMetadataJson(enrichMetadataJson(job, dto.getMetadataJson(), null));
    return dto;
  }

  /** Enriquece metadata persistível de conclusão com custo real ou estimado. */
  public String enrichMetadataJson(
      SalesVideoJob job, String metadataJson, BigDecimal explicitCostUsd) {
    ObjectNode metadata = readObjectNode(metadataJson);
    if (readCost(metadata) != null) {
      markKnownCost(metadata);
      return writeJson(metadata);
    }
    BigDecimal costUsd = normalizeCost(explicitCostUsd);
    if (costUsd == null) {
      costUsd = estimateCost(job, metadata);
    }
    metadata.put("cost_usd", costUsd);
    metadata.put("costUsd", costUsd);
    ObjectNode estimation = metadata.putObject("cost_estimation");
    estimation.put("estimated", explicitCostUsd == null);
    estimation.put(
        "source", explicitCostUsd == null ? "PROVIDER_RATE_CARD_ESTIMATE" : "PROVIDER_REPORTED");
    estimation.put("provider_name", job != null ? job.getProviderName() : null);
    estimation.put(
        "job_type", job != null && job.getJobType() != null ? job.getJobType().name() : null);
    estimation.put("duration_seconds", resolveDurationSeconds(job, metadata));
    estimation.put("resolution", resolveResolution(metadata));
    estimation.put("catalog_version", "2026-07-28");
    estimation.put("calculated_at", Instant.now().toString());
    return writeJson(metadata);
  }

  /** Resolve o custo usado para sincronizar ativos vinculados ao job. */
  public BigDecimal resolveCostUsd(
      SalesVideoJob job, String metadataJson, BigDecimal explicitCostUsd) {
    BigDecimal normalized = normalizeCost(explicitCostUsd);
    if (normalized != null) {
      return normalized;
    }
    ObjectNode metadata = readObjectNode(metadataJson);
    BigDecimal metadataCost = readCost(metadata);
    return metadataCost != null ? metadataCost : estimateCost(job, metadata);
  }

  /**
   * Estima custo pelo provider, duração e resolução; usa zero auditado quando não há tarifa
   * aplicável.
   */
  private BigDecimal estimateCost(SalesVideoJob job, ObjectNode metadata) {
    Integer durationSeconds = resolveDurationSeconds(job, metadata);
    String resolution = resolveResolution(metadata);
    BigDecimal estimated =
        costCalculator.estimateUsd(
            job != null ? job.getProviderName() : null,
            resolveModel(job, metadata),
            durationSeconds,
            resolution);
    return estimated != null ? estimated : ZERO_COST;
  }

  /** Resolve modelo a partir do metadata ou do provider do job. */
  private String resolveModel(SalesVideoJob job, ObjectNode metadata) {
    String metadataModel = readText(metadata, "model", "provider_model");
    if (StringUtils.hasText(metadataModel)) {
      return metadataModel;
    }
    return job != null ? job.getProviderName() : null;
  }

  /** Resolve duração de custo por metadata, alvo do perfil ou padrão operacional do provider. */
  private Integer resolveDurationSeconds(SalesVideoJob job, ObjectNode metadata) {
    Integer duration =
        readInteger(
            metadata,
            "duration_seconds",
            "durationSeconds",
            "expected_clip_duration_seconds",
            "target_duration_seconds");
    if (duration != null && duration > 0) {
      return duration;
    }
    JsonNode providerStrategy = metadata.path("provider_strategy");
    if (providerStrategy.isObject()) {
      JsonNode expected = providerStrategy.path("expected_clip_duration_seconds");
      if (expected.canConvertToInt() && expected.asInt() > 0) {
        return expected.asInt();
      }
    }
    JsonNode assemblyPlan = metadata.path("assembly_plan");
    if (assemblyPlan.isObject()) {
      JsonNode target = assemblyPlan.path("final_target_duration_seconds");
      if (target.canConvertToInt() && target.asInt() > 0) {
        return target.asInt();
      }
    }
    if (job != null
        && job.getProfile() != null
        && job.getProfile().getTargetDurationSeconds() != null) {
      return job.getProfile().getTargetDurationSeconds();
    }
    return defaultDurationSeconds(job);
  }

  /** Define duração padrão quando o job não possui duração explícita. */
  private Integer defaultDurationSeconds(SalesVideoJob job) {
    if (job == null || job.getJobType() == null) {
      return null;
    }
    if (job.getJobType() == SalesVideoJobType.SCRIPT
        || job.getJobType() == SalesVideoJobType.PUBLISH) {
      return null;
    }
    String provider = normalize(job.getProviderName());
    if (provider.contains("luma")) {
      return 30;
    }
    if (provider.contains("veo")) {
      return 8;
    }
    if (provider.contains("kling") || provider.contains("runway") || provider.contains("runaway")) {
      return 10;
    }
    if (provider.contains("heygen")) {
      return 30;
    }
    return null;
  }

  /** Resolve resolução de custo a partir do metadata. */
  private String resolveResolution(ObjectNode metadata) {
    String resolution = readText(metadata, "resolution", "output_resolution");
    return StringUtils.hasText(resolution) ? resolution : "720p";
  }

  /** Marca custo existente como custo conhecido reportado. */
  private void markKnownCost(ObjectNode metadata) {
    ObjectNode estimation = metadata.putObject("cost_estimation");
    estimation.put("estimated", false);
    estimation.put("source", "PROVIDER_REPORTED");
    estimation.put("catalog_version", "2026-07-28");
  }

  /** Lê ou cria um objeto JSON de metadata sem quebrar jobs legados inválidos. */
  private ObjectNode readObjectNode(String metadataJson) {
    if (!StringUtils.hasText(metadataJson)) {
      return objectMapper.createObjectNode();
    }
    try {
      JsonNode parsed = objectMapper.readTree(metadataJson);
      if (parsed != null && parsed.isObject()) {
        return (ObjectNode) parsed;
      }
    } catch (JsonProcessingException ignored) {
      return objectMapper.createObjectNode();
    }
    return objectMapper.createObjectNode();
  }

  /** Serializa metadata enriquecido para persistência ou contrato de leitura. */
  private String writeJson(ObjectNode metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException ex) {
      return "{\"cost_usd\":0.0000,\"cost_estimation\":{\"estimated\":true,\"source\":\"SERIALIZATION_FALLBACK\"}}";
    }
  }

  /** Lê custo já informado por worker ou provider. */
  private BigDecimal readCost(ObjectNode metadata) {
    BigDecimal snake = readDecimal(metadata.path("cost_usd"));
    if (snake != null) {
      return snake;
    }
    return readDecimal(metadata.path("costUsd"));
  }

  /** Normaliza custo explícito para quatro casas decimais. */
  private BigDecimal normalizeCost(BigDecimal costUsd) {
    if (costUsd == null) {
      return null;
    }
    return costUsd.setScale(4, RoundingMode.HALF_UP);
  }

  /** Lê decimal de um nó JSON numérico ou textual. */
  private BigDecimal readDecimal(JsonNode value) {
    if (value == null || value.isMissingNode() || value.isNull()) {
      return null;
    }
    if (value.isNumber()) {
      return value.decimalValue().setScale(4, RoundingMode.HALF_UP);
    }
    if (value.isTextual() && value.asText().trim().matches("-?\\d+(\\.\\d+)?")) {
      return new BigDecimal(value.asText().trim()).setScale(4, RoundingMode.HALF_UP);
    }
    return null;
  }

  /** Lê o primeiro campo inteiro válido do metadata. */
  private Integer readInteger(ObjectNode metadata, String... fields) {
    for (String field : fields) {
      JsonNode value = metadata.path(field);
      if (value.canConvertToInt() && value.asInt() > 0) {
        return value.asInt();
      }
    }
    return null;
  }

  /** Lê o primeiro campo textual válido do metadata. */
  private String readText(ObjectNode metadata, String... fields) {
    for (String field : fields) {
      JsonNode value = metadata.path(field);
      if (value.isTextual() && StringUtils.hasText(value.asText())) {
        return value.asText().trim();
      }
    }
    return null;
  }

  /** Normaliza textos de provider para comparação tolerante. */
  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
  }
}
