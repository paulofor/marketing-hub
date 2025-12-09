package com.marketinghub.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Shared storage configuration used to access the Cloudflare R2 bucket that keeps lead portal assets.
 */
@Component
@ConfigurationProperties(prefix = "lead-portal.storage")
public class StorageProperties {

    /** Cloudflare R2 bucket name. */
    private String bucket = "";

    /** R2 endpoint (e.g. https://<account-id>.r2.cloudflarestorage.com). */
    private String endpoint = "";

    /** Access key ID for the R2 bucket. */
    private String accessKeyId = "";

    /** Secret access key for the R2 bucket. */
    private String secretAccessKey = "";

    /** Region understood by the S3 client. */
    private String region = "auto";

    /** Optional public base URL for serving files. */
    private String publicBaseUrl = "";

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
