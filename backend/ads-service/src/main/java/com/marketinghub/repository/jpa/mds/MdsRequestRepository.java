package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.MdsRequest;
import com.marketinghub.mds.MdsRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Repositório JPA responsável pela persistência de MdsRequest. */
public interface MdsRequestRepository
    extends JpaRepository<MdsRequest, Long>, JpaSpecificationExecutor<MdsRequest> {
  List<MdsRequest> findTop50ByStatusOrderByCreatedAtAsc(MdsRequestStatus status);
}
