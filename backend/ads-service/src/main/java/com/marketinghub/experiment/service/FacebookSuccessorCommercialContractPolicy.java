package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Centraliza a identidade comercial imutável que permite adotar uma superfície Facebook. */
@Service
public class FacebookSuccessorCommercialContractPolicy {

  /**
   * Confirma que origem e sucessor representam a mesma oferta sem exigir a mesma mensagem criativa.
   */
  public boolean matches(Experiment source, Experiment successor) {
    return source != null
        && successor != null
        && sameIdentity(source.getProduct(), successor.getProduct())
        && sameIdentity(source.getNiche(), successor.getNiche())
        && sameIdentity(source.getHypothesisRef(), successor.getHypothesisRef())
        && sameIdentity(source.getFacebookPage(), successor.getFacebookPage())
        && sameIdentity(source.getInstagramAccount(), successor.getInstagramAccount())
        && Objects.equals(source.getDesireTerritoryCode(), successor.getDesireTerritoryCode())
        && source.getExperimentType() == successor.getExperimentType()
        && source.getProductAiSubtype() == successor.getProductAiSubtype()
        && source.getCampaignObjective() == successor.getCampaignObjective()
        && sameAmount(source.getUnitPrice(), successor.getUnitPrice());
  }

  /** Compara referências persistidas pela identidade sem depender da instância JPA concreta. */
  private boolean sameIdentity(Object source, Object successor) {
    if (source == null || successor == null) {
      return source == successor;
    }
    Object sourceId = identityOf(source);
    Object successorId = identityOf(successor);
    return sourceId != null && Objects.equals(sourceId, successorId);
  }

  /** Extrai a identidade dos tipos que compõem o contrato comercial do sucessor. */
  private Object identityOf(Object entity) {
    if (entity instanceof com.marketinghub.product.Product product) {
      return product.getId();
    }
    if (entity instanceof com.marketinghub.niche.MarketNiche niche) {
      return niche.getId();
    }
    if (entity instanceof com.marketinghub.hypothesis.Hypothesis hypothesis) {
      return hypothesis.getId();
    }
    if (entity instanceof com.marketinghub.ads.FacebookPage page) {
      return page.getId();
    }
    if (entity instanceof com.marketinghub.ads.InstagramAccount account) {
      return account.getId();
    }
    return null;
  }

  /** Compara preço sem tratar diferença de escala decimal como mudança de oferta. */
  private boolean sameAmount(BigDecimal source, BigDecimal successor) {
    return source != null && successor != null && source.compareTo(successor) == 0;
  }
}
