package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.service.LeadPortalImagePackageEngagementService;
import java.net.URI;
import java.util.Base64;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints used to track engagement with Lead Portal image packages.
 */
@RestController
@RequestMapping("/api/public/lead-portal/image-packages")
public class LeadPortalImagePackageEngagementController {

    private static final byte[] TRANSPARENT_PIXEL = Base64.getDecoder()
            .decode("R0lGODlhAQABAPAAAAAAAP///ywAAAAAAQABAAACAUwAOw==");

    private final LeadPortalImagePackageEngagementService engagementService;

    public LeadPortalImagePackageEngagementController(LeadPortalImagePackageEngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping("/{id}/open.gif")
    public ResponseEntity<byte[]> trackEmailOpen(@PathVariable("id") long packageId,
                                                 @RequestParam(name = "sid", required = false) String submissionId) {
        engagementService.markEmailOpened(packageId, submissionId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_GIF)
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(TRANSPARENT_PIXEL);
    }

    @GetMapping("/{id}/previews")
    public ResponseEntity<Void> trackImagesViewed(@PathVariable("id") long packageId,
                                                  @RequestParam(name = "sid", required = false) String submissionId) {
        return engagementService.markImagesViewed(packageId, submissionId)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
