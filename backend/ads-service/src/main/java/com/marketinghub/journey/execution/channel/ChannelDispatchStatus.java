package com.marketinghub.journey.execution.channel;

/**
 * Normalised status returned by channel handlers after attempting a dispatch.
 */
public enum ChannelDispatchStatus {
    OK,
    TRANSIENT_ERROR,
    PERMANENT_ERROR
}
