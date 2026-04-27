package com.marketinghub.mds.repository;

import com.marketinghub.mds.MdsRequest;
import com.marketinghub.mds.MdsRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MdsRequestRepository extends JpaRepository<MdsRequest, Long>, JpaSpecificationExecutor<MdsRequest> {
    List<MdsRequest> findTop50ByStatusOrderByCreatedAtAsc(MdsRequestStatus status);
}
