package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.exception.FlowNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.storage.FlowDefinitionStorage;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class FlowService {

    private final FlowDefinitionStorage storage;
    private final Map<String, Flow> flows;
    private final Object writeLock = new Object();

    public FlowService(FlowDefinitionStorage storage) {
        this.storage = storage;
        this.flows = new ConcurrentHashMap<>(storage.loadAll());
    }

    public Flow save(Flow flow) {
        synchronized (writeLock) {
            flows.put(flow.slug(), flow);
            persist();
            return flow;
        }
    }

    public Flow get(String slug) {
        Flow flow = flows.get(slug);
        if (flow == null) {
            throw new FlowNotFoundException(slug);
        }
        return flow;
    }

    public void delete(String slug) {
        synchronized (writeLock) {
            flows.remove(slug);
            persist();
        }
    }

    public Collection<Flow> list() {
        return List.copyOf(flows.values());
    }

    private void persist() {
        storage.saveAll(new java.util.ArrayList<>(flows.values()));
    }
}
