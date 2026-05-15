package com.marketinghub.mois.biblioteca.web;

import com.marketinghub.mois.biblioteca.dto.MoisSalesLibraryDtos;
import com.marketinghub.mois.biblioteca.service.MoisSalesLibraryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/urls:ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryIngestResponse ingestUrls(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryIngestRequest request
    ) {
        return service.ingestUrls(request);
    }
}
