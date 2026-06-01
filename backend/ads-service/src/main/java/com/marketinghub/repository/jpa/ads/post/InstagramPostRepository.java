package com.marketinghub.repository.jpa.ads.post;

import com.marketinghub.ads.post.InstagramPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de InstagramPost.
 */
public interface InstagramPostRepository extends JpaRepository<InstagramPost, Long> {
    List<InstagramPost> findByAccountId(Long accountId);
}
