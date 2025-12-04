package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalWorkerImageFailureRequest;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImagePackageDto;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageRetryRequest;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageResultRequest;
import com.marketinghub.leadportal.service.LeadPortalImagePackageWorkerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints consumidos pelo Worker AI responsáveis por esvaziar a fila automática do Lead Portal.
 */
@RestController
@RequestMapping("/api/worker/image-packages")
@Validated
public class LeadPortalImagePackageWorkerController {

    private final LeadPortalImagePackageWorkerService workerService;

    public LeadPortalImagePackageWorkerController(LeadPortalImagePackageWorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping("/recent")
    public List<LeadPortalWorkerImagePackageDto> listRecent() {
        return workerService.listRecentPackages();
    }

    @PostMapping("/{id}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startProcessing(@PathVariable("id") long packageId) {
        workerService.markProcessing(packageId);
    }

    @PostMapping("/{id}/fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markFailed(
            @PathVariable("id") long packageId,
            @Valid @RequestBody LeadPortalWorkerImageFailureRequest request) {
        workerService.markFailed(packageId, request.reason());
    }

    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retry(
            @PathVariable("id") long packageId,
            @RequestBody(required = false) LeadPortalWorkerImageRetryRequest request) {
        String reason = request == null ? null : request.reason();
        workerService.retry(packageId, reason);
    }

    @PostMapping("/{id}/results")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitResults(
            @PathVariable("id") long packageId,
            @Valid @RequestBody LeadPortalWorkerImageResultRequest request) {
        workerService.submitResults(packageId, request);
    }
}
