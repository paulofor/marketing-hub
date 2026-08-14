package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.dto.SalesVideoProviderModelDto;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoProviderModelRequest;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoProviderPricingRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Responsabilidade: administrar modelos de vídeo sob a fachada única do módulo SalesVideo. */
public class SalesVideoProviderCatalogService {
  private static final Set<String> STATUSES = Set.of("DRAFT", "HOMOLOGATION", "ACTIVE", "BLOCKED");
  private static final Set<String> IMPLEMENTED_ADAPTERS =
      Set.of("LUMA", "KLING", "RUNWAY", "VEO", "HEYGEN");
  private static final Set<String> PRICING_UNITS = Set.of("SECOND", "VIDEO", "CREDIT");
  private static final Set<String> RESEARCH_STATUSES =
      Set.of("VERIFIED", "INCOMPARABLE", "BLOCKED");
  private final SalesVideoProviderModelRepository repository;

  /** Inicializa o serviço com o repositório canônico do catálogo. */
  public SalesVideoProviderCatalogService(SalesVideoProviderModelRepository repository) {
    this.repository = repository;
  }

  /** Lista todos os modelos, inclusive rascunhos e bloqueados para transparência administrativa. */
  public List<SalesVideoProviderModelDto> list() {
    return repository.findAllByOrderByDisplayNameAsc().stream().map(this::toDto).toList();
  }

  /** Atualiza a curadoria e impede ativação antes de todos os gates e do adaptador real. */
  public SalesVideoProviderModelDto update(Long id, UpdateSalesVideoProviderModelRequest request) {
    SalesVideoProviderModel model =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Modelo de vídeo não encontrado"));
    String status = request.lifecycleStatus().trim().toUpperCase(Locale.ROOT);
    if (!STATUSES.contains(status)) {
      throw new IllegalArgumentException("Status de homologação inválido");
    }
    boolean implementedAdapter = IMPLEMENTED_ADAPTERS.contains(model.getAdapterKey());
    if (status.equals("ACTIVE")
        && (!implementedAdapter
            || !request.adapterVerified()
            || !request.pricingVerified()
            || !request.commercialLicenseVerified()
            || !request.qualityGateVerified())) {
      throw new IllegalArgumentException(
          "Modelo só pode ser ativado com adaptador, preço, licença e qualidade homologados");
    }
    model.setRecommendedUse(request.recommendedUse().trim());
    model.setLifecycleStatus(status);
    model.setAdapterVerified(request.adapterVerified() && implementedAdapter);
    model.setPricingVerified(request.pricingVerified());
    model.setCommercialLicenseVerified(request.commercialLicenseVerified());
    model.setQualityGateVerified(request.qualityGateVerified());
    model.setNotes(request.notes() == null ? null : request.notes().trim());
    model.setUpdatedAt(Instant.now());
    return toDto(repository.save(model));
  }

  /** Registra a pesquisa auditável de Plutus sem liberar gasto ou ativar o modelo. */
  public SalesVideoProviderModelDto updatePricing(
      Long id, UpdateSalesVideoProviderPricingRequest request) {
    SalesVideoProviderModel model =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Modelo de vídeo não encontrado"));
    String unit = request.unit().trim().toUpperCase(Locale.ROOT);
    String status = request.status().trim().toUpperCase(Locale.ROOT);
    if (!PRICING_UNITS.contains(unit) || !RESEARCH_STATUSES.contains(status)) {
      throw new IllegalArgumentException("Unidade ou estado da pesquisa de preço inválido");
    }
    if ("VERIFIED".equals(status) && (request.amountUsd() == null || request.quantity() == null)) {
      throw new IllegalArgumentException("Preço verificado exige valor e quantidade comparável");
    }
    model.setPricingAmountUsd(request.amountUsd());
    model.setPricingUnit(unit);
    model.setPricingQuantity(request.quantity());
    model.setPricingResolution(trim(request.resolution()));
    model.setPricingIncludesAudio(request.includesAudio());
    model.setPricingSourceUrl(request.sourceUrl().trim());
    model.setPricingObservedAt(request.observedAt());
    model.setPricingResearchStatus(status);
    model.setPricingResearchNotes(trim(request.notes()));
    model.setPricingResearchRawResponse(request.rawResponse());
    model.setPricingResearchModel(trim(request.researchModel()));
    model.setPricingVerified("VERIFIED".equals(status));
    model.setUpdatedAt(Instant.now());
    return toDto(repository.save(model));
  }

  /** Converte a entidade persistida no contrato estável da API. */
  private SalesVideoProviderModelDto toDto(SalesVideoProviderModel model) {
    return new SalesVideoProviderModelDto(
        model.getId(),
        model.getCode(),
        model.getDisplayName(),
        model.getProviderName(),
        model.getProviderFamily(),
        model.getAdapterKey(),
        model.getExternalModelId(),
        model.getRecommendedUse(),
        model.getLifecycleStatus(),
        model.getClipDurationSeconds(),
        model.getMaxDirectDurationSeconds(),
        model.isSupportsHeroVideo(),
        model.isSupportsSceneAssembly(),
        model.isRequiresSourceImage(),
        model.getCreditsUrl(),
        model.getDocumentationUrl(),
        model.isAdapterVerified(),
        model.isPricingVerified(),
        model.isCommercialLicenseVerified(),
        model.isQualityGateVerified(),
        model.getNotes(),
        model.getPricingAmountUsd(),
        model.getPricingUnit(),
        model.getPricingQuantity(),
        model.getPricingResolution(),
        model.getPricingIncludesAudio(),
        model.getPricingSourceUrl(),
        model.getPricingObservedAt(),
        model.getPricingResearchStatus(),
        model.getPricingResearchNotes(),
        normalizedCostPerSecond(model),
        model.getPricingObservedAt() == null
            || model.getPricingObservedAt().isBefore(Instant.now().minus(30, ChronoUnit.DAYS)));
  }

  /** Normaliza ofertas cobradas por segundo ou por vídeo para permitir comparação homogênea. */
  private BigDecimal normalizedCostPerSecond(SalesVideoProviderModel model) {
    if (model.getPricingAmountUsd() == null || model.getPricingQuantity() == null) return null;
    if ("SECOND".equals(model.getPricingUnit())) {
      return model
          .getPricingAmountUsd()
          .divide(model.getPricingQuantity(), 6, RoundingMode.HALF_UP);
    }
    if ("VIDEO".equals(model.getPricingUnit()) && model.getMaxDirectDurationSeconds() > 0) {
      return model
          .getPricingAmountUsd()
          .divide(BigDecimal.valueOf(model.getMaxDirectDurationSeconds()), 6, RoundingMode.HALF_UP);
    }
    return null;
  }

  /** Remove espaços de evidências opcionais preservando ausência real. */
  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
