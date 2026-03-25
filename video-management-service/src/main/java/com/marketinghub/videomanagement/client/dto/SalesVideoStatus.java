package com.marketinghub.videomanagement.client.dto;

/**
 * Status canônicos refletidos pelo backend.
 */
public enum SalesVideoStatus {
    DRAFT,
    SCRIPT_PENDING,
    SCRIPT_READY,
    STORYBOARD_PENDING,
    STORYBOARD_READY,
    VIDEO_REQUESTED,
    VIDEO_PROCESSING,
    VIDEO_READY,
    VIDEO_FAILED,
    PUBLISHED,
    ARCHIVED
}
