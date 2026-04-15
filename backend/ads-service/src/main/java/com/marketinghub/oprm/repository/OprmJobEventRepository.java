package com.marketinghub.oprm.repository;

import com.marketinghub.oprm.OprmJobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmJobEventRepository extends JpaRepository<OprmJobEvent, Long> {
}
