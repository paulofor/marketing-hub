package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.LeadResponse;
import com.marketinghub.leadportal.dto.ResultResponse;
import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.service.LeadService;
import com.marketinghub.leadportal.storage.FileStorageService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin
@Validated
public class LeadController {

    private final LeadService leadService;
    private final FileStorageService fileStorageService;

    public LeadController(LeadService leadService, FileStorageService fileStorageService) {
        this.leadService = leadService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeadResponse> createLead(
            @RequestParam("name") @NotBlank(message = "Nome é obrigatório") String name,
            @RequestParam("email")
                    @NotBlank(message = "E-mail é obrigatório")
                    @Email(message = "E-mail inválido")
                    String email,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("image") MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Imagem é obrigatória");
        }
        Lead lead = leadService.createLead(name, email, notes, imageFile);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(lead.getId())
                .toUri();
        return ResponseEntity.created(location).body(LeadResponse.from(lead, buildImageUrl(lead)));
    }

    @GetMapping("/{id}")
    public LeadResponse getLead(@PathVariable UUID id) {
        Lead lead = leadService.getLead(id);
        return LeadResponse.from(lead, buildImageUrl(lead));
    }

    @GetMapping("/{id}/result")
    public ResultResponse getLeadResult(@PathVariable UUID id) {
        Lead lead = leadService.getLead(id);
        return ResultResponse.from(lead);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> downloadImage(@PathVariable UUID id) {
        Lead lead = leadService.getLead(id);
        Resource file = fileStorageService.loadAsResource(lead.getStoredFileName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + lead.getOriginalFileName())
                .contentType(MediaType.parseMediaType(
                        lead.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : lead.getContentType()))
                .body(file);
    }

    private String buildImageUrl(Lead lead) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/leads/")
                .path(lead.getId().toString())
                .path("/image")
                .toUriString();
    }
}
