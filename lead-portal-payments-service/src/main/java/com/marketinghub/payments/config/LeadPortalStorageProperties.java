package com.marketinghub.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lead-portal.storage")
public class LeadPortalStorageProperties {

    private String bucket = "";
    private String endpoint = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";
    private String region = "auto";
    private String publicBaseUrl = "";
    private long maxDownloadBytes = 50 * 1024 * 1024;
    private String originalsPrefix = "originals";

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public long getMaxDownloadBytes() {
        return maxDownloadBytes;
    }

    public void setMaxDownloadBytes(long maxDownloadBytes) {
        this.maxDownloadBytes = maxDownloadBytes;
    }

    public String getOriginalsPrefix() {
        return originalsPrefix;
    }

    public void setOriginalsPrefix(String originalsPrefix) {
        this.originalsPrefix = originalsPrefix;
    }
}
