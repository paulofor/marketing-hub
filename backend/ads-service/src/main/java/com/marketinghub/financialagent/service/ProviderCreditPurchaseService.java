package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.StudioProviderCreditPurchase;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.ProviderCreditPurchaseResponse;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.RegisterProviderCreditPurchaseRequest;
import com.marketinghub.repository.jpa.financialagent.StudioProviderCreditPurchaseRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: registrar recargas sem misturá-las ao custo consumido por render. */
@Service
public class ProviderCreditPurchaseService {
  private final StudioProviderCreditPurchaseRepository repository;

  /** Inicializa o serviço com o repositório canônico das recargas. */
  public ProviderCreditPurchaseService(StudioProviderCreditPurchaseRepository repository) {
    this.repository = repository;
  }

  /** Registra uma recarga idempotente com os dados comprovados pelo usuário. */
  @Transactional
  public ProviderCreditPurchaseResponse register(
      String provider, RegisterProviderCreditPurchaseRequest request) {
    String normalizedProvider = normalizeProvider(provider);
    String normalizedCurrency = request.currency().trim().toUpperCase(Locale.ROOT);
    StudioProviderCreditPurchase purchase =
        repository
            .findByProviderAndPurchasedAtAndAmountAndCurrencyAndCreditsPurchased(
                normalizedProvider,
                request.purchasedAt(),
                request.amount(),
                normalizedCurrency,
                request.creditsPurchased())
            .orElseGet(StudioProviderCreditPurchase::new);
    purchase.setProvider(normalizedProvider);
    purchase.setPurchasedAt(request.purchasedAt());
    purchase.setAmount(request.amount());
    purchase.setCurrency(normalizedCurrency);
    purchase.setCreditsPurchased(request.creditsPurchased());
    purchase.setEvidenceReference(normalizeEvidence(request.evidenceReference()));
    return toResponse(repository.save(purchase));
  }

  /** Lista o histórico de recargas do provedor selecionado. */
  @Transactional(readOnly = true)
  public List<ProviderCreditPurchaseResponse> list(String provider) {
    return repository.findByProviderOrderByPurchasedAtDesc(normalizeProvider(provider)).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Normaliza a identidade do provedor para impedir históricos paralelos. */
  private String normalizeProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("Provedor é obrigatório");
    }
    return provider.trim().toUpperCase(Locale.ROOT);
  }

  /** Converte referência vazia em ausência explícita. */
  private String normalizeEvidence(String evidenceReference) {
    return evidenceReference == null || evidenceReference.isBlank()
        ? null
        : evidenceReference.trim();
  }

  /** Converte a entidade persistida no contrato público do módulo. */
  private ProviderCreditPurchaseResponse toResponse(StudioProviderCreditPurchase purchase) {
    return new ProviderCreditPurchaseResponse(
        purchase.getId(),
        purchase.getProvider(),
        purchase.getPurchasedAt(),
        purchase.getAmount(),
        purchase.getCurrency(),
        purchase.getCreditsPurchased(),
        purchase.getEvidenceReference(),
        purchase.getCreatedAt());
  }
}
