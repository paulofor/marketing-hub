package com.marketinghub.oprm.niche.dto;

public record OprmNicheSnapshotIngestResponseDto(
        String status,
        int received,
        int validated,
        int persisted,
        int discarded,
        String qualityStatus,
        String qualityNotes
) {
}
