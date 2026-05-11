package com.marketinghub.oprmcoletormei.catalog.service;

import com.marketinghub.oprmcoletormei.catalog.config.CnaeCatalogCollectorProperties;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectRequest;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectResponse;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogIngestPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CnaeCatalogCollectorService {

    private final RestClient.Builder restClientBuilder;
    private final CnaeCatalogCollectorProperties properties;

    public CnaeCatalogCollectorService(RestClient.Builder restClientBuilder, CnaeCatalogCollectorProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    public CnaeCatalogCollectResponse collectAndIngest(CnaeCatalogCollectRequest request) {
        List<CnaeCatalogIngestPayload.Record> normalized = normalizeAndDeduplicate(request.records());
        List<List<CnaeCatalogIngestPayload.Record>> batches = partition(normalized, properties.batchSize());

        int persisted = 0;
        for (List<CnaeCatalogIngestPayload.Record> batch : batches) {
            CnaeCatalogIngestPayload payload = new CnaeCatalogIngestPayload(request.source().trim(), batch);
            IngestResponse response = sendBatch(payload);
            persisted += response.persisted();
        }

        return new CnaeCatalogCollectResponse(
                request.records().size(),
                normalized.size(),
                request.records().size() - normalized.size(),
                batches.size(),
                persisted
        );
    }

    private IngestResponse sendBatch(CnaeCatalogIngestPayload payload) {
        return restClientBuilder.build()
                .post()
                .uri(properties.backendBaseUrl() + "/api/niches/catalog:ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(IngestResponse.class);
    }

    private List<CnaeCatalogIngestPayload.Record> normalizeAndDeduplicate(List<CnaeCatalogCollectRequest.RawRecord> records) {
        Map<String, CnaeCatalogIngestPayload.Record> dedup = new LinkedHashMap<>();
        for (CnaeCatalogCollectRequest.RawRecord record : records) {
            String normalizedCode = record.cnaeCode().replaceAll("[^0-9]", "").trim();
            if (normalizedCode.isBlank()) {
                continue;
            }
            dedup.putIfAbsent(
                    normalizedCode,
                    new CnaeCatalogIngestPayload.Record(
                            normalizedCode,
                            record.cnaeLabel().trim(),
                            record.active() == null || record.active()
                    )
            );
        }
        return new ArrayList<>(dedup.values());
    }

    private List<List<CnaeCatalogIngestPayload.Record>> partition(List<CnaeCatalogIngestPayload.Record> records, int batchSize) {
        List<List<CnaeCatalogIngestPayload.Record>> batches = new ArrayList<>();
        for (int i = 0; i < records.size(); i += batchSize) {
            batches.add(records.subList(i, Math.min(i + batchSize, records.size())));
        }
        return batches;
    }

    private record IngestResponse(int persisted) {
    }
}
