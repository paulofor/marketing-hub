package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalImagePackageAckRequest;
import com.marketinghub.leadportal.dto.LeadPortalImagePackageExportDto;
import com.marketinghub.leadportal.service.LeadPortalImagePackageExportItem;
import com.marketinghub.leadportal.service.LeadPortalPackageNotificationService;
import com.marketinghub.storage.FileStorageService;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos consumidos pelo serviço de e-mail para enviar pacotes de imagens com marca d'água.
 */
@RestController
@RequestMapping("/api/internal/lead-portal/image-packages")
public class LeadPortalImagePackageExportController {

    private final LeadPortalPackageNotificationService notificationService;
    private final FileStorageService fileStorageService;

    public LeadPortalImagePackageExportController(LeadPortalPackageNotificationService notificationService,
                                                  FileStorageService fileStorageService) {
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/export")
    public List<LeadPortalImagePackageExportDto> export(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return notificationService.exportReadyPackages(limit).stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/{packageId}/ack")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledge(@PathVariable long packageId,
                            @Valid @RequestBody LeadPortalImagePackageAckRequest request) {
        notificationService.acknowledgePackage(packageId, request.success(), request.errorMessage());
    }

    private LeadPortalImagePackageExportDto toDto(LeadPortalImagePackageExportItem item) {
        LeadPortalImagePackageExportDto.SampleEmail sampleEmail = new LeadPortalImagePackageExportDto.SampleEmail(
                item.sampleSubject(),
                item.samplePreview(),
                item.sampleBody(),
                item.sampleCallToAction(),
                item.sampleModel(),
                item.samplePrompt(),
                item.sampleUpdatedAt()
        );

        LeadPortalImagePackageExportDto.EmailContent emailContent = new LeadPortalImagePackageExportDto.EmailContent(
                item.emailSubject(),
                item.emailPlainBody(),
                item.emailHtmlBody()
        );


List<LeadPortalImagePackageExportDto.Attachment> attachments = item.attachments().stream()
        .map(att -> {
            byte[] bytes = att.bytes();
            String base64Attachment = bytes != null && bytes.length > 0
                    ? Base64.getEncoder().encodeToString(bytes)
                    : "";
            long sizeBytes = bytes != null ? bytes.length : 0L;
            String downloadUrl = StringUtils.hasText(att.downloadUrl())
                    ? att.downloadUrl()
                    : fileStorageService.resolvePublicUrl(att.storedFileName()).orElse("");
            return new LeadPortalImagePackageExportDto.Attachment(
                    att.fileName(),
                    att.contentType(),
                    base64Attachment,
                    att.imageCount(),
                    sizeBytes,
                    att.storedFileName(),
                    downloadUrl
            );
        })
        .toList();
LeadPortalImagePackageExportDto.Attachment primaryAttachment = attachments.isEmpty() ? null : attachments.get(0);

return new LeadPortalImagePackageExportDto(
        item.packageId(),
        parseUuid(item.submissionId()),
        item.submissionName(),
        item.submissionEmail(),
        item.status(),
        item.experimentId(),
        item.experimentName(),
        sampleEmail,
        emailContent,
        item.sendImagesAsZip(),
        item.imageCount(),
        primaryAttachment,
        attachments,
        item.notificationAttempts(),
        item.notificationLastAttempt()
);
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
