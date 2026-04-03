package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BackendAssetClientTest {

    @Test
    void extractAssetUrlReturnsUrlFieldWhenPayloadIsBackendJson() {
        String body = """
                {
                  "stored_file_name": "uploads/2026/04/03/misc/sample.jpg",
                  "url": "https://pub.example.r2.dev/uploads/2026/04/03/misc/sample.jpg",
                  "category": "GENERIC"
                }
                """;

        String parsed = BackendAssetClient.extractAssetUrl(body);

        assertThat(parsed).isEqualTo("https://pub.example.r2.dev/uploads/2026/04/03/misc/sample.jpg");
    }

    @Test
    void extractAssetUrlReturnsImageUrlFieldWhenPresent() {
        String body = "{\"imageUrl\":\"/uploads/image.jpg\"}";

        String parsed = BackendAssetClient.extractAssetUrl(body);

        assertThat(parsed).isEqualTo("/uploads/image.jpg");
    }

    @Test
    void extractAssetUrlReturnsRawBodyWhenResponseIsPlainText() {
        String body = "https://cdn.example.com/uploads/image.jpg";

        String parsed = BackendAssetClient.extractAssetUrl(body);

        assertThat(parsed).isEqualTo(body);
    }
}
