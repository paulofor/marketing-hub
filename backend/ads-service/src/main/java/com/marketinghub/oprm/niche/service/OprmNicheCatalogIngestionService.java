package com.marketinghub.oprm.niche.service;

import com.marketinghub.oprm.niche.OprmNicheCatalogItem;
import com.marketinghub.oprm.niche.dto.OprmNicheCatalogIngestRequestDto;
import com.marketinghub.oprm.niche.dto.OprmNicheSnapshotIngestResponseDto;
import com.marketinghub.repository.jpa.oprm.niche.OprmNicheCatalogItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OprmNicheCatalogIngestionService {

    private final OprmNicheCatalogItemRepository repository;

    @Transactional
    public OprmNicheSnapshotIngestResponseDto ingest(OprmNicheCatalogIngestRequestDto request) {
        List<OprmNicheCatalogItem> items = request.records().stream()
                .map(record -> OprmNicheCatalogItem.builder()
                        .cnaeCode(record.cnaeCode().trim())
                        .cnaeLabel(record.cnaeLabel().trim())
                        .source(request.source().trim())
                        .active(record.active() == null || record.active())
                        .build())
                .toList();

        List<OprmNicheCatalogItem> persisted = repository.saveAll(items);
        int received = request.records().size();

        return new OprmNicheSnapshotIngestResponseDto(
                "ACCEPTED",
                received,
                received,
                persisted.size(),
                0,
                "OK",
                "catálogo CNAE persistido com sucesso"
        );
    }
}
