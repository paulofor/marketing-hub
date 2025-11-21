package com.marketinghub.leadportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lead-portal.submission-storage")
public class SubmissionStorageProperties {

    /**
     * Destination file used to persist flow submissions.
     */
    private String location = "data/submissions.json";

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
