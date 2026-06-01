package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.MdsSourceAccessRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de MdsSourceAccessRecord.
 */
public interface MdsSourceAccessRecordRepository extends JpaRepository<MdsSourceAccessRecord, Long> {
}
