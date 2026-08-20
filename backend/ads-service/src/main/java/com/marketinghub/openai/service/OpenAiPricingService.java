package com.marketinghub.openai.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Calcula custos de uso OpenAI a partir do catálogo canônico de modelos persistido no backend. */
@Component
public class OpenAiPricingService {

  private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

  private final OpenAiModelRepository modelRepository;

  /** Inicializa o serviço com o repositório do catálogo canônico de modelos. */
  public OpenAiPricingService(OpenAiModelRepository modelRepository) {
    this.modelRepository = modelRepository;
  }

  /**
   * Estima custo canônico de execução Flex, usando os preços batch/flex cadastrados no catálogo
   * OpenAI.
   */
  public BigDecimal estimateFlexCost(String modelCode, Integer inputTokens, Integer outputTokens) {
    return estimateBatchCost(
        modelCode, new OpenAiResponse.OpenAiUsage(inputTokens, outputTokens, null, null, null));
  }

  /** Estima custo canônico de execução Standard usando o uso retornado pela OpenAI. */
  public BigDecimal estimateStandardCost(String modelCode, OpenAiResponse.OpenAiUsage usage) {
    return estimateCost(modelCode, usage, PricingMode.STANDARD);
  }

  /** Estima custo canônico de execução Batch usando o uso retornado pela OpenAI. */
  public BigDecimal estimateBatchCost(String modelCode, OpenAiResponse.OpenAiUsage usage) {
    return estimateCost(modelCode, usage, PricingMode.BATCH);
  }

  /** Estima uma chamada vinculada a tarefa, separando entrada comum, entrada em cache e saída. */
  public Optional<BigDecimal> estimateTaskCost(
      String modelCode,
      String serviceTier,
      Long inputTokens,
      Long cachedInputTokens,
      Long outputTokens) {
    if (modelCode == null || modelCode.isBlank()) return Optional.empty();
    Optional<OpenAiModel> model =
        modelRepository
            .findByCode(normalizeModelCode(modelCode))
            .or(() -> modelRepository.findByCode(modelCode));
    if (model.isEmpty()) return Optional.empty();
    long input = inputTokens == null ? 0 : inputTokens;
    long cached = cachedInputTokens == null ? 0 : cachedInputTokens;
    long output = outputTokens == null ? 0 : outputTokens;
    if (input < 0 || cached < 0 || output < 0 || cached > input) return Optional.empty();
    PricingMode mode =
        "FLEX".equalsIgnoreCase(serviceTier) || "BATCH".equalsIgnoreCase(serviceTier)
            ? PricingMode.BATCH
            : PricingMode.STANDARD;
    OpenAiModel price = model.get();
    BigDecimal inputPrice =
        mode == PricingMode.BATCH ? price.getPriceInputBatch() : price.getPriceInputStandard();
    BigDecimal cachedPrice =
        mode == PricingMode.BATCH
            ? price.getPriceInputCachedBatch()
            : price.getPriceInputCachedStandard();
    BigDecimal outputPrice =
        mode == PricingMode.BATCH ? price.getPriceOutputBatch() : price.getPriceOutputStandard();
    if ((input > cached && inputPrice == null)
        || (cached > 0 && cachedPrice == null)
        || (output > 0 && outputPrice == null)) return Optional.empty();
    BigDecimal total =
        multiplyTaskTokens(inputPrice, input - cached)
            .add(multiplyTaskTokens(cachedPrice, cached))
            .add(multiplyTaskTokens(outputPrice, output))
            .setScale(8, RoundingMode.HALF_UP);
    return Optional.of(total);
  }

  /** Resolve modelo e modo de preço para calcular o custo autoritativo. */
  private BigDecimal estimateCost(
      String modelCode, OpenAiResponse.OpenAiUsage usage, PricingMode mode) {
    if (usage == null) {
      return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
    OpenAiModel model =
        modelRepository
            .findByCode(normalizeModelCode(modelCode))
            .or(() -> modelRepository.findByCode(modelCode))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Modelo OpenAI não encontrado no catálogo para cálculo de custo: "
                            + modelCode));
    return calculateCost(model, usage, mode);
  }

  /** Calcula o custo proporcional aos tokens informados para o modo de preço solicitado. */
  private BigDecimal calculateCost(
      OpenAiModel model, OpenAiResponse.OpenAiUsage usage, PricingMode mode) {
    long inputTokens = usage.effectiveInputTokens() != null ? usage.effectiveInputTokens() : 0;
    long outputTokens = usage.effectiveOutputTokens() != null ? usage.effectiveOutputTokens() : 0;
    BigDecimal inputPrice =
        mode == PricingMode.BATCH ? model.getPriceInputBatch() : model.getPriceInputStandard();
    BigDecimal outputPrice =
        mode == PricingMode.BATCH ? model.getPriceOutputBatch() : model.getPriceOutputStandard();
    BigDecimal inputCost = multiplyTokens(inputPrice, inputTokens);
    BigDecimal outputCost = multiplyTokens(outputPrice, outputTokens);
    return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
  }

  /** Multiplica tokens pelo preço por um milhão de tokens. */
  private BigDecimal multiplyTokens(BigDecimal pricePerMillion, long tokens) {
    if (pricePerMillion == null || tokens <= 0) {
      return BigDecimal.ZERO;
    }
    return pricePerMillion
        .multiply(BigDecimal.valueOf(tokens))
        .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
  }

  /** Mantém precisão suficiente para estimar tarefas pequenas antes de arredondar em oito casas. */
  private BigDecimal multiplyTaskTokens(BigDecimal pricePerMillion, long tokens) {
    if (pricePerMillion == null || tokens <= 0) return BigDecimal.ZERO;
    return pricePerMillion
        .multiply(BigDecimal.valueOf(tokens))
        .divide(ONE_MILLION, 12, RoundingMode.HALF_UP);
  }

  /** Normaliza o código do modelo para busca no catálogo persistido. */
  private String normalizeModelCode(String modelCode) {
    if (modelCode == null) {
      return null;
    }
    return modelCode.trim().toLowerCase(Locale.ROOT);
  }

  private enum PricingMode {
    STANDARD,
    BATCH
  }
}
