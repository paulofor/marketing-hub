package com.marketinghub.pde.validationcontract.v1.service;

import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: entregar ao PDE o contrato candidato exato usado no preflight autenticado. */
@Service
public class PdeValidationContractService {
  private final PdeProductionSlotService slotService;
  private final PublicProductCommercialOfferService offerService;
  private final String internalToken;

  /** Configura as fontes canônicas e o segredo compartilhado sem persistir cópias do contrato. */
  public PdeValidationContractService(
      PdeProductionSlotService slotService,
      PublicProductCommercialOfferService offerService,
      @Value("${integrations.pde-platform.internal-token:}") String internalToken) {
    this.slotService = slotService;
    this.offerService = offerService;
    this.internalToken = internalToken == null ? "" : internalToken;
  }

  /** Retorna o rascunho candidato ou snapshot publicado da mesma versão solicitada. */
  public String experience(
      String informedToken, String productSlug, String slotCode, String experienceVersion) {
    authorize(informedToken);
    return slotService.findValidationExperienceJson(productSlug, slotCode, experienceVersion);
  }

  /** Retorna a oferta canônica da mesma candidata sem marcar prontidão ou publicar o slot. */
  public PublicProductCommercialOfferResponse offer(
      String informedToken, String productSlug, String slotCode, String experienceVersion) {
    authorize(informedToken);
    return offerService.getValidationOffer(productSlug, slotCode, experienceVersion);
  }

  /** Recusa configuração ausente e compara o segredo em tempo constante. */
  private void authorize(String informedToken) {
    if (internalToken.isBlank()
        || informedToken == null
        || !MessageDigest.isEqual(
            internalToken.getBytes(StandardCharsets.UTF_8),
            informedToken.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Preflight interno PDE não autorizado");
    }
  }
}
