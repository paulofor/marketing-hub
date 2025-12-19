package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.LeadPortalPurchase;
import com.marketinghub.payments.model.PurchaseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface LeadPortalPurchaseRepository extends JpaRepository<LeadPortalPurchase, Long> {

    Optional<LeadPortalPurchase> findByMercadoPagoPaymentId(String paymentId);

    Optional<LeadPortalPurchase> findTopByPackageIdOrderByCreatedAtDesc(Long packageId);

    List<LeadPortalPurchase> findByStatusAndDeliveredAtIsNull(PurchaseStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LeadPortalPurchase p where p.id = :id")
    Optional<LeadPortalPurchase> lockById(@Param("id") Long id);
}
