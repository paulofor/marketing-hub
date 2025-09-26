package com.marketinghub.ads;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacebookPageRepository extends JpaRepository<FacebookPage, Long> {
    List<FacebookPage> findByAccountId(Long accountId);

    long countByAccountId(Long accountId);
}
