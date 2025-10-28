package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.exception.LeadNotFoundException;
import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.model.LeadStatus;
import com.marketinghub.leadportal.storage.FileStorageService;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LeadService {

    private final FileStorageService storageService;
    private final Map<UUID, Lead> leads = new ConcurrentHashMap<>();

    public LeadService(FileStorageService storageService) {
        this.storageService = storageService;
    }

    public Lead createLead(String name, String email, String notes, MultipartFile imageFile) {
        UUID id = UUID.randomUUID();
        String storedFileName = storageService.store(imageFile, id.toString());
        Lead lead =
                new Lead(
                        id,
                        name,
                        email,
                        notes,
                        imageFile.getOriginalFilename(),
                        storedFileName,
                        imageFile.getContentType(),
                        Instant.now());
        leads.put(id, lead);
        return lead;
    }

    public Lead getLead(UUID id) {
        Lead lead = leads.get(id);
        if (lead == null) {
            throw new LeadNotFoundException(id);
        }
        return lead;
    }

    public Collection<Lead> getAllLeads() {
        return leads.values();
    }

    public void completeLead(UUID id, String result) {
        Lead lead = getLead(id);
        if (lead.getStatus() == LeadStatus.COMPLETED) {
            return;
        }
        lead.markCompleted(result, Instant.now());
    }
}
