package com.marketinghub.oprm.niche.service;

import com.marketinghub.oprm.niche.OprmNicheSnapshot;
import com.marketinghub.oprm.niche.dto.OprmNicheSnapshotIngestRequestDto;
import com.marketinghub.oprm.niche.dto.OprmNicheSnapshotIngestResponseDto;
import com.marketinghub.oprm.niche.repository.OprmNicheSnapshotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OprmNicheIngestionService {
    private final OprmNicheSnapshotRepository repository;

    @Transactional
    public OprmNicheSnapshotIngestResponseDto ingest(OprmNicheSnapshotIngestRequestDto request) {
        List<OprmNicheSnapshot> snapshots = request.records().stream()
                .map(record -> OprmNicheSnapshot.builder()
                        .snapshotDate(request.snapshotDate())
                        .source(request.source().trim())
                        .cnaeCode(record.cnaeCode().trim())
                        .uf(record.uf().trim())
                        .municipio(record.municipio().trim())
                        .meiActive(record.meiActive())
                        .openings(record.openings())
                        .closures(record.closures())
                        .net(record.net())
                        .build())
                .toList();

        List<OprmNicheSnapshot> persisted = repository.saveAll(snapshots);
        int received = request.records().size();

        return new OprmNicheSnapshotIngestResponseDto(
                "ACCEPTED",
                received,
                received,
                persisted.size(),
                0,
                "OK",
                "lote persistido com sucesso"
        );
    }
}
