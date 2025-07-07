package com.marketinghub.ads.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstagramPostRepository extends JpaRepository<InstagramPost, Long> {
    List<InstagramPost> findByAccountId(Long accountId);
}
