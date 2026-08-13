package com.marketinghub.repository.jpa.financialagent;

import com.marketinghub.financialagent.StudioProviderCreditPurchase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar recargas de créditos dos provedores do Estúdio. */
public interface StudioProviderCreditPurchaseRepository
    extends JpaRepository<StudioProviderCreditPurchase, Long> {
  /** Lista as recargas mais recentes do provedor. */
  List<StudioProviderCreditPurchase> findByProviderOrderByPurchasedAtDesc(String provider);

  /** Lista recargas legadas ou atuais pertencentes à mesma família de provedor. */
  @Query(
      "select p from StudioProviderCreditPurchase p where upper(p.provider) = upper(:provider) or upper(p.provider) like concat(upper(:provider), '%') order by p.purchasedAt desc")
  List<StudioProviderCreditPurchase> findByProviderFamily(@Param("provider") String provider);

  /** Lista todos os provedores que possuem saldo comprado auditável. */
  @Query("select distinct p.provider from StudioProviderCreditPurchase p order by p.provider")
  List<String> findDistinctProviders();

  /** Localiza um lançamento equivalente para impedir duplicidade em reenvios. */
  Optional<StudioProviderCreditPurchase>
      findByProviderAndPurchasedAtAndAmountAndCurrencyAndCreditsPurchased(
          String provider,
          Instant purchasedAt,
          BigDecimal amount,
          String currency,
          Integer creditsPurchased);
}
