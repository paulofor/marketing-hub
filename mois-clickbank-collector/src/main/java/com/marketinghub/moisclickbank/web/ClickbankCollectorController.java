package com.marketinghub.moisclickbank.web;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionResponse;
import com.marketinghub.moisclickbank.service.ClickbankCollectorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mois-clickbank")
public class ClickbankCollectorController {

    private final ClickbankCollectorService collectorService;

    public ClickbankCollectorController(ClickbankCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping("/collections")
    public ClickbankCollectionResponse collect(@Valid @RequestBody ClickbankCollectionRequest request) {
        return collectorService.collect(request);
    }
}
