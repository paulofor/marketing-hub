package com.marketinghub.oprm.integration.contract;

public enum OprmJobStatus {
    PENDING,
    CLAIMED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_WAIT,
    CANCELLED
}
