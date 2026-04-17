package com.marketinghub.mds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mds.MdsArtifactLineageEdge;
import com.marketinghub.mds.MdsArtifactRecord;
import com.marketinghub.mds.MdsArtifactStatus;
import com.marketinghub.mds.MdsRequest;
import com.marketinghub.mds.dto.MdsArtifactPublishBatchRequest;
import com.marketinghub.mds.dto.MdsArtifactPublishBatchResponse;
import com.marketinghub.mds.dto.MdsLineageCreateRequest;
import com.marketinghub.mds.dto.MdsLineageResponse;
import com.marketinghub.mds.repository.MdsArtifactLineageEdgeRepository;
import com.marketinghub.mds.repository.MdsArtifactRecordRepository;
import com.marketinghub.mds.repository.MdsRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class MdsArtifactService {
    private final MdsRequestRepository requestRepository;
    private final MdsArtifactRecordRepository artifactRecordRepository;
    private final MdsArtifactLineageEdgeRepository lineageEdgeRepository;
    private final ObjectMapper objectMapper;

    public MdsArtifactService(MdsRequestRepository requestRepository,
                              MdsArtifactRecordRepository artifactRecordRepository,
                              MdsArtifactLineageEdgeRepository lineageEdgeRepository,
                              ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.artifactRecordRepository = artifactRecordRepository;
        this.lineageEdgeRepository = lineageEdgeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MdsArtifactPublishBatchResponse publishBatch(MdsArtifactPublishBatchRequest request) {
        MdsRequest mdsRequest = requestRepository.findById(request.requestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mds request not found"));

        List<Long> ids = new ArrayList<>();
        for (MdsArtifactPublishBatchRequest.MdsArtifactPayload artifactPayload : request.artifacts()) {
            String contentJson = toJson(artifactPayload.content());
            MdsArtifactRecord record = MdsArtifactRecord.builder()
                    .artifactType(artifactPayload.artifactType())
                    .schemaVersion(artifactPayload.schemaVersion())
                    .version(artifactPayload.version())
                    .status(parseStatus(artifactPayload.status()))
                    .producerModule(artifactPayload.producerModule())
                    .ownerModule(artifactPayload.ownerModule())
                    .request(mdsRequest)
                    .contentJson(contentJson)
                    .hash(resolveHash(artifactPayload.hash(), contentJson))
                    .build();
            MdsArtifactRecord saved = artifactRecordRepository.save(record);
            ids.add(saved.getId());

            if (artifactPayload.parentArtifactIds() != null) {
                for (Long parentId : artifactPayload.parentArtifactIds()) {
                    createLineage(parentId, saved.getId(), "DERIVED_FROM");
                }
            }
        }

        return new MdsArtifactPublishBatchResponse(request.requestId(), ids.size(), ids);
    }

    @Transactional
    public MdsLineageResponse createLineage(MdsLineageCreateRequest request) {
        return createLineage(request.parentArtifactId(), request.childArtifactId(), request.relationType());
    }

    private MdsLineageResponse createLineage(Long parentArtifactId, Long childArtifactId, String relationType) {
        MdsArtifactRecord parent = artifactRecordRepository.findById(parentArtifactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent artifact not found"));
        MdsArtifactRecord child = artifactRecordRepository.findById(childArtifactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "child artifact not found"));

        MdsArtifactLineageEdge edge = MdsArtifactLineageEdge.builder()
                .parentArtifact(parent)
                .childArtifact(child)
                .relationType(relationType)
                .build();
        MdsArtifactLineageEdge saved = lineageEdgeRepository.save(edge);

        return new MdsLineageResponse(
                saved.getId(),
                saved.getParentArtifact().getId(),
                saved.getChildArtifact().getId(),
                saved.getRelationType()
        );
    }

    private MdsArtifactStatus parseStatus(String status) {
        try {
            return MdsArtifactStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "artifact status must be one of DRAFT, VALIDATED, APPROVED");
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? java.util.Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid artifact content json");
        }
    }

    private String resolveHash(String hash, String contentJson) {
        if (hash != null && !hash.isBlank()) {
            return hash;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(contentJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
