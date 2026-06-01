package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de FacebookPage.
 */
public interface FacebookPageRepository extends JpaRepository<FacebookPage, Long> {
    List<FacebookPage> findByAccountId(Long accountId);

    long countByAccountId(Long accountId);
}
