package com.marketinghub.oprmcoletormei.catalog.service;

import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectResponse;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogExecutionLogEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Service;

@Service
public class CnaeCatalogExecutionLogService {

    private static final int MAX_ENTRIES = 200;
    private final Deque<CnaeCatalogExecutionLogEntry> entries = new ConcurrentLinkedDeque<>();

    public void info(String trigger, String status, String message, CnaeCatalogCollectResponse response) {
        append(new CnaeCatalogExecutionLogEntry(
                Instant.now(),
                trigger,
                status,
                message,
                response == null ? null : response.received(),
                response == null ? null : response.normalized(),
                response == null ? null : response.persisted()
        ));
    }

    public void error(String trigger, String message) {
        append(new CnaeCatalogExecutionLogEntry(Instant.now(), trigger, "ERROR", message, null, null, null));
    }

    public List<CnaeCatalogExecutionLogEntry> latest() {
        return new ArrayList<>(entries);
    }

    private void append(CnaeCatalogExecutionLogEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }
}
