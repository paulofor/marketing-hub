package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.exception.FlowNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class FlowService {

    private final Map<String, Flow> flows = new ConcurrentHashMap<>();

    public Flow save(Flow flow) {
        flows.put(flow.slug(), flow);
        return flow;
    }

    public Flow get(String slug) {
        Flow flow = flows.get(slug);
        if (flow == null) {
            throw new FlowNotFoundException(slug);
        }
        return flow;
    }

    public void delete(String slug) {
        flows.remove(slug);
    }

    public Collection<Flow> list() {
        return flows.values();
    }
}
