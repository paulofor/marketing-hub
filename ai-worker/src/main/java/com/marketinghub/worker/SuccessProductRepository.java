package com.marketinghub.worker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuccessProductRepository extends JpaRepository<SuccessProduct, Long> {
    List<SuccessProduct> findByNovoTrue();
}
