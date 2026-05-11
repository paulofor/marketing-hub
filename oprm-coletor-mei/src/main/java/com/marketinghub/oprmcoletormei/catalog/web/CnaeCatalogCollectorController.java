package com.marketinghub.oprmcoletormei.catalog.web;

import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectRequest;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectResponse;
import com.marketinghub.oprmcoletormei.catalog.service.CnaeCatalogCollectorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm-mei/catalog")
public class CnaeCatalogCollectorController {

    private final CnaeCatalogCollectorService service;

    public CnaeCatalogCollectorController(CnaeCatalogCollectorService service) {
        this.service = service;
    }

    @PostMapping("/collect")
    public ResponseEntity<CnaeCatalogCollectResponse> collect(@Valid @RequestBody CnaeCatalogCollectRequest request) {
        return ResponseEntity.accepted().body(service.collectAndIngest(request));
    }
}
