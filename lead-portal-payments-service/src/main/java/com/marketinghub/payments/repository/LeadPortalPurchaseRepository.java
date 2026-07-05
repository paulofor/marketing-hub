package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/** Responsabilidade: acessar compras e preferências de pagamento do Lead Portal. */
public interface LeadPortalPurchaseRepository extends JpaRepository<LeadPortalPurchase, Long> {

    /** Busca uma compra pelo identificador retornado pelo Mercado Pago. */
    Optional<LeadPortalPurchase> findByMercadoPagoPaymentId(String paymentId);

    /** Busca a preferência mais recente vinculada a um pacote de imagem. */
    Optional<LeadPortalPurchase> findTopByPackageIdOrderByCreatedAtDesc(Long packageId);

    /** Lista compras que ainda precisam de entrega pós-pagamento. */
    List<LeadPortalPurchase> findByStatusAndDeliveredAtIsNull(PurchaseStatus status, Pageable pageable);

    /** Carrega uma compra com lock pessimista para evitar processamento concorrente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LeadPortalPurchase p where p.id = :id")
    Optional<LeadPortalPurchase> lockById(@Param("id") Long id);

    /** Marca o primeiro acesso ao checkout sem sobrescrever o acesso original. */
    @Modifying
    @Query(value = """
            UPDATE lead_portal_purchase
               SET checkout_accessed_at = COALESCE(checkout_accessed_at, :accessedAt),
                   updated_at = :accessedAt
             WHERE id = :purchaseId
            """, nativeQuery = true)
    int markCheckoutAccessed(@Param("purchaseId") Long purchaseId, @Param("accessedAt") Instant accessedAt);
}
