package com.marketinghub.payments.repository;

import com.marketinghub.payments.model.LeadPortalPremiumDelivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface LeadPortalPremiumDeliveryRepository extends JpaRepository<LeadPortalPremiumDelivery, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LeadPortalPremiumDelivery> findByPurchaseId(Long purchaseId);
}
