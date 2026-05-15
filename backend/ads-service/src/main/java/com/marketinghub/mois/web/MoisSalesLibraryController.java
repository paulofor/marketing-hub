package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisSalesLibraryDtos;
import com.marketinghub.mois.service.MoisSalesLibraryService;
import jakarta.validation.Valid;
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

    @PostMapping("/urls:ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryIngestResponse ingestUrls(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryIngestRequest request
    ) {
        return service.ingestUrls(request);
    }
}
