package com.marketinghub.imagedeliverable;

/**
 * Processing status for an image deliverable package lifecycle.
 */
public enum ImageDeliverableStatus {
    RECEIVED,
    PROCESSED,
    GENERATION_WITH_WATERMARK,
    PURCHASED,
    GENERATION_NO_WATERMARK,
    FAILED
}
