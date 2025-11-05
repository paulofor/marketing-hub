package com.marketinghub.leadportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lead-portal.flow-storage")
public class FlowStorageProperties {

    /**
     * Destination file used to persist published flow definitions.
     */
    private String location = "data/flows.json";

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
