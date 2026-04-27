package com.marketinghub.mds.web;

import com.marketinghub.mds.dto.*;
import com.marketinghub.mds.service.MdsAdminAuthorizationService;
import com.marketinghub.mds.service.MdsAdminService;
import com.marketinghub.mds.service.MdsArtifactService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/mds")
public class MdsAdminController {
    private final MdsAdminService adminService;
    private final MdsArtifactService artifactService;
    private final MdsAdminAuthorizationService authorizationService;

    public MdsAdminController(MdsAdminService adminService,
                              MdsArtifactService artifactService,
                              MdsAdminAuthorizationService authorizationService) {
        this.adminService = adminService;
        this.artifactService = artifactService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/requests")
    public MdsAdminRequestListResponse listRequests(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, name = "tenantOrProduct") String tenantOrProduct,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        authorizationService.assertAllowed(role);
        return adminService.listRequests(status, from, to, tenantOrProduct, page, size);
    }

    @GetMapping("/requests/{id}")
    public MdsAdminRequestDetailResponse getRequest(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id
    ) {
        authorizationService.assertAllowed(role);
        return adminService.getRequestDetail(id);
    }

    @GetMapping("/requests/{id}/artifacts")
    public MdsAdminArtifactsResponse listArtifacts(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id
    ) {
        authorizationService.assertAllowed(role);
        return adminService.listArtifactsWithLineage(id);
    }

    @GetMapping("/reports/{requestId}")
    public MdsReportResponse getReport(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long requestId
    ) {
        authorizationService.assertAllowed(role);
        return artifactService.getReportByRequest(requestId);
    }

    @PostMapping("/requests/{id}/retry")
    public MdsAdminRetryResponse retry(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id
    ) {
        authorizationService.assertAllowed(role);
        return adminService.retryRequest(id);
    }

    @GetMapping("/health")
    public Map<String, String> health(
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        authorizationService.assertAllowed(role);
        return Map.of("status", "ok", "module", "mds-admin-api");
    }
}
