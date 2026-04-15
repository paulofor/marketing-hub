package com.marketinghub.oprm;

public enum OprmJobStatus {
    PENDING,
    CLAIMED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_WAIT,
    CANCELLED
}
