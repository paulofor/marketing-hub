package com.marketinghub.repository.jpa.ads.post;

import com.marketinghub.ads.post.InstagramPost;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de InstagramPost. */
public interface InstagramPostRepository extends JpaRepository<InstagramPost, Long> {
  List<InstagramPost> findByAccountId(Long accountId);
}
