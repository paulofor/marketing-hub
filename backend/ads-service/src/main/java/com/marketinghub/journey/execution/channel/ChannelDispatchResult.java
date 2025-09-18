package com.marketinghub.journey.execution.channel;

import java.time.Instant;
import java.util.Map;

/**
 * Result of dispatching a journey stimulus through an external provider.
 */
public record ChannelDispatchResult(ChannelDispatchStatus status,
                                    String providerMessageId,
                                    Instant nextAttemptAt,
                                    String errorMessage,
                                    Map<String, Object> metadata) {
    public static ChannelDispatchResult success(String providerMessageId, Map<String, Object> metadata) {
        return new ChannelDispatchResult(ChannelDispatchStatus.OK, providerMessageId, null, null, metadata == null ? Map.of() : metadata);
    }

    public static ChannelDispatchResult transientFailure(String message, Instant nextAttemptAt, Map<String, Object> metadata) {
        return new ChannelDispatchResult(ChannelDispatchStatus.TRANSIENT_ERROR, null, nextAttemptAt, message, metadata == null ? Map.of() : metadata);
    }

    public static ChannelDispatchResult permanentFailure(String message, Map<String, Object> metadata) {
        return new ChannelDispatchResult(ChannelDispatchStatus.PERMANENT_ERROR, null, null, message, metadata == null ? Map.of() : metadata);
    }
}
