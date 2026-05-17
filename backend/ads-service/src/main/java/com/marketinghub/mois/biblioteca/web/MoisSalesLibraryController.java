package com.marketinghub.mois.biblioteca.web;

import com.marketinghub.mois.biblioteca.dto.MoisSalesLibraryDtos;
import com.marketinghub.mois.biblioteca.service.MoisSalesLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mois/sales-library")
@RequiredArgsConstructor
@Validated
public class MoisSalesLibraryController {

    private final MoisSalesLibraryService service;

    @GetMapping("/entries")
    public MoisSalesLibraryDtos.SalesLibraryEntryPageResponse listEntries(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.listEntries(workspaceId, page, pageSize);
    }

    @GetMapping("/jobs")
    public MoisSalesLibraryDtos.SalesLibraryJobPageResponse listJobs(
            @RequestParam String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.listJobs(workspaceId, status, page, pageSize);
    }

    @GetMapping("/jobs/{jobId}")
    public MoisSalesLibraryDtos.SalesLibraryJobResponse getJob(@PathVariable long jobId) {
        try {
            return service.getJob(jobId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping("/pages")
    public MoisSalesLibraryDtos.SalesLibraryPageListResponse listPages(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.listPages(workspaceId, page, pageSize);
    }

    @GetMapping("/pages/{pageId}")
    public MoisSalesLibraryDtos.SalesLibraryPageResponse getPage(@PathVariable long pageId) {
        try {
            return service.getPage(pageId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping("/pages/{pageId}/analysis")
    public MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse getPageAnalysis(@PathVariable long pageId) {
        try {
            return service.getPageAnalysis(pageId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/pages/{pageId}:reanalyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse reanalyzePage(@PathVariable long pageId) {
        try {
            return service.reanalyzePage(pageId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/urls:ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryIngestResponse ingestUrls(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryIngestRequest request
    ) {
        return service.ingestUrls(request);
    }
}
