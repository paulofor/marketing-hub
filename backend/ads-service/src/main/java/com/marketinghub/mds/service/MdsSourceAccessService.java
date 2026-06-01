package com.marketinghub.mds.service;

import com.marketinghub.mds.MdsSourceAccessRecord;
import com.marketinghub.mds.dto.MdsSourceAccessPublishBatchRequest;
import com.marketinghub.mds.dto.MdsSourceAccessPublishBatchResponse;
import com.marketinghub.repository.jpa.mds.MdsSourceAccessRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MdsSourceAccessService {
    private final MdsSourceAccessRecordRepository repository;

    public MdsSourceAccessService(MdsSourceAccessRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MdsSourceAccessPublishBatchResponse publishBatch(MdsSourceAccessPublishBatchRequest request) {
        List<Long> ids = new ArrayList<>();

        for (MdsSourceAccessPublishBatchRequest.SourceAccessPayload payload : request.records()) {
            MdsSourceAccessRecord record = MdsSourceAccessRecord.builder()
                    .sourceDocumentId(payload.sourceDocumentId())
                    .accessClass(payload.accessClass())
                    .permissionState(payload.permissionState())
                    .licenseText(payload.licenseText())
                    .accessUrl(payload.accessUrl())
                    .build();
            ids.add(repository.save(record).getId());
        }

        return new MdsSourceAccessPublishBatchResponse(ids.size(), ids);
    }
}
