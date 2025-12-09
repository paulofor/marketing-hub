package com.marketinghub.storage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

class StorageConfigTest {

    private final StorageConfig config = new StorageConfig();

    @Test
    void shouldCreateClientWithAnonymousCredentialsWhenKeysAreMissing() {
        StorageProperties properties = new StorageProperties();
        properties.setRegion("us-east-1");

        S3Client client = config.leadPortalS3Client(properties);

        assertNotNull(client);
        client.close();
    }
}
