package com.marketinghub.mois.biblioteca.service;

import com.marketinghub.mois.biblioteca.dto.MoisSalesLibraryDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoisSalesLibrarySnapshotScheduler {

    private final MoisSalesLibrarySnapshotService snapshotService;

    @Scheduled(cron = "0 */30 * * * *")
    public void captureMissingSnapshots() {
        var response = snapshotService.captureSnapshots(
                new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest("workspace-001", 5, false));
        log.info("MOIS sales-library snapshot scheduler finished. workspaceId={}, processed={}, captured={}, failed={}",
                response.workspaceId(), response.processed(), response.captured(), response.failed());
    }
}
