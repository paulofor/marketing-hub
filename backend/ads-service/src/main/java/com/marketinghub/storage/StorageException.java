package com.marketinghub.storage;

/**
 * Runtime exception thrown when Cloudflare R2 interactions fail.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
