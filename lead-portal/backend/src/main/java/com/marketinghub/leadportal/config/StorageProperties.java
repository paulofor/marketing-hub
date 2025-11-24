package com.marketinghub.leadportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lead-portal.storage")
public class StorageProperties {

    /** Cloudflare R2 bucket name. */
    private String bucket;

    /** R2 endpoint (e.g. https://<account-id>.r2.cloudflarestorage.com). */
    private String endpoint;

    /** Access key ID for the R2 bucket. */
    private String accessKeyId;

    /** Secret access key for the R2 bucket. */
    private String secretAccessKey;

    /**
     * Region understood by the S3 client. Cloudflare R2 commonly uses "auto" but any
     * custom value supported by {@link software.amazon.awssdk.regions.Region#of(String)}
     * can be used.
     */
    private String region = "auto";

    /** Optional public base URL (custom domain or bucket URL) for serving images. */
    private String publicBaseUrl;

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
}
