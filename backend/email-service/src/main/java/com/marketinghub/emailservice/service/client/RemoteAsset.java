package com.marketinghub.emailservice.service.client;

import org.springframework.http.MediaType;

public record RemoteAsset(
        String fileName,
        MediaType mediaType,
        byte[] content
) {
}
