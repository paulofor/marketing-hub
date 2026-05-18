package com.marketinghub.geralanding;

public record GeraLandingPublishResponse(
        Long experimentId,
        Long flowId,
        String iframeUrl,
        String standaloneUrl,
        String message) {
}
