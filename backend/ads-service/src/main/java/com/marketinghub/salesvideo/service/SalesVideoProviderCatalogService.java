package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.dto.SalesVideoProviderModelDto;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoProviderModelRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Responsabilidade: administrar modelos de vídeo sob a fachada única do módulo SalesVideo. */
public class SalesVideoProviderCatalogService {
  private static final Set<String> STATUSES = Set.of("DRAFT", "HOMOLOGATION", "ACTIVE", "BLOCKED");
  private static final Set<String> IMPLEMENTED_ADAPTERS =
      Set.of("LUMA", "KLING", "RUNWAY", "VEO", "HEYGEN");
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
  public SalesVideoProviderModelDto update(
      Long id, UpdateSalesVideoProviderModelRequest request) {
    SalesVideoProviderModel model =
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Modelo de vídeo não encontrado"));
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

  /** Converte a entidade persistida no contrato estável da API. */
  private SalesVideoProviderModelDto toDto(SalesVideoProviderModel model) {
    return new SalesVideoProviderModelDto(
        model.getId(), model.getCode(), model.getDisplayName(), model.getProviderName(),
        model.getProviderFamily(), model.getAdapterKey(), model.getExternalModelId(),
        model.getRecommendedUse(), model.getLifecycleStatus(), model.getClipDurationSeconds(),
        model.getMaxDirectDurationSeconds(), model.isSupportsHeroVideo(),
        model.isSupportsSceneAssembly(), model.isRequiresSourceImage(), model.getCreditsUrl(),
        model.getDocumentationUrl(), model.isAdapterVerified(), model.isPricingVerified(),
        model.isCommercialLicenseVerified(), model.isQualityGateVerified(), model.getNotes());
  }
}
