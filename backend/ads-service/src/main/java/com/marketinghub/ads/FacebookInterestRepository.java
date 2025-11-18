package com.marketinghub.ads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacebookInterestRepository extends JpaRepository<FacebookInterest, Long> {
    List<FacebookInterest> findByStatus(FacebookInterestStatus status);

    List<FacebookInterest> findByStatusAndFacebookInterestIdIsNull(FacebookInterestStatus status);
}
