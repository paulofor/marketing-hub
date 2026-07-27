package com.marketinghub.repository.jpa.ads;

import com.marketinghub.ads.FacebookPage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de FacebookPage. */
public interface FacebookPageRepository extends JpaRepository<FacebookPage, Long> {
  List<FacebookPage> findByAccountId(Long accountId);

  long countByAccountId(Long accountId);
}
