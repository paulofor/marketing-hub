package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookInterest;
import com.marketinghub.ads.FacebookInterestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de FacebookInterest.
 */
public interface FacebookInterestRepository extends JpaRepository<FacebookInterest, Long> {
    List<FacebookInterest> findByStatus(FacebookInterestStatus status);

    List<FacebookInterest> findByStatusAndFacebookInterestIdIsNull(FacebookInterestStatus status);
}
