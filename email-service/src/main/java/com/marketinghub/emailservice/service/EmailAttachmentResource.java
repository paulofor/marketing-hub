package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.service.client.RemoteAsset;

public record EmailAttachmentResource(
        RemoteAsset asset,
        boolean inline,
        String contentId
) {
}
