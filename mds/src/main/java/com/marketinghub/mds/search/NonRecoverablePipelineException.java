package com.marketinghub.mds.search;

public class NonRecoverablePipelineException extends RuntimeException {
    public NonRecoverablePipelineException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonRecoverablePipelineException(String message) {
        super(message);
    }
}
