package com.marketinghub.oprmcoletormei.catalog.web;

import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectRequest;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectResponse;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogExecutionLogEntry;
import com.marketinghub.oprmcoletormei.catalog.service.CnaeCatalogCollectorService;
import com.marketinghub.oprmcoletormei.catalog.service.CnaeCatalogExecutionLogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm-mei/catalog")
public class CnaeCatalogCollectorController {

    private final CnaeCatalogCollectorService service;
    private final CnaeCatalogExecutionLogService executionLogService;

    public CnaeCatalogCollectorController(
            CnaeCatalogCollectorService service,
            CnaeCatalogExecutionLogService executionLogService
    ) {
        this.service = service;
        this.executionLogService = executionLogService;
    }

    @PostMapping("/collect")
    public ResponseEntity<CnaeCatalogCollectResponse> collect(@Valid @RequestBody CnaeCatalogCollectRequest request) {
        return ResponseEntity.accepted().body(service.collectAndIngest(request));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<CnaeCatalogExecutionLogEntry>> executions() {
        return ResponseEntity.ok(executionLogService.latest());
    }
}
