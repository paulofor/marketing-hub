package com.marketinghub.watermark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermark.storage")
public class StorageProperties {

    /** Cloudflare R2 bucket name. */
    private String bucket;

    /** R2 endpoint (e.g. https://<account>.r2.cloudflarestorage.com). */
    private String endpoint;

    /** Access key ID for the R2 bucket. */
    private String accessKeyId;

    /** Secret access key for the bucket. */
    private String secretAccessKey;

    /** Region understood by the AWS SDK (Cloudflare commonly uses "auto"). */
    private String region = "auto";

    /** Optional public base URL (custom domain) to resolve stored images. */
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
