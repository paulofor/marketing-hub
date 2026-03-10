package com.marketinghub.emailservice.storage;

import jakarta.annotation.PostConstruct;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Minimal storage service used to fetch lead portal assets hosted on Cloudflare R2.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final StorageProperties properties;
    private final S3Client s3Client;

    public FileStorageService(StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (isBlank(properties.getBucket())
                || isBlank(properties.getEndpoint())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getSecretAccessKey())) {
            log.warn("Lead portal storage is not fully configured; e-mails with anexos may fail");
        }
    }

    public Resource loadAsResource(String storedFileName) {
        if (isBlank(storedFileName)) {
            throw new StorageException("storedFileName must be provided");
        }
        ResponseInputStream<GetObjectResponse> responseStream = null;
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storedFileName)
                    .build();
            responseStream = s3Client.getObject(request, ResponseTransformer.toInputStream());
            Resource resource = toResource(storedFileName, responseStream, properties.getMaxDownloadBytes());
            responseStream = null; // the caller is responsible for closing the stream
            return resource;
        } catch (SdkException | IOException ex) {
            abortQuietly(responseStream);
            throw new StorageException("Failed to load file from bucket", ex);
        }
    }

    public Optional<String> resolvePublicUrl(String storedFileName) {
        if (isBlank(storedFileName)) {
            return Optional.empty();
        }
        String base = properties.getPublicBaseUrl();
        if (isBlank(base)) {
            return Optional.empty();
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return Optional.of(normalized + "/" + storedFileName);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Resource toResource(String storedFileName,
                                ResponseInputStream<GetObjectResponse> responseStream,
                                long maxBytes)
            throws IOException {
        Long contentLength = responseStream.response().contentLength();
        if (maxBytes > 0 && contentLength != null && contentLength > maxBytes) {
            abortQuietly(responseStream);
            throw new IOException("Object '" + storedFileName + "' exceeds max download size of " + maxBytes + " bytes");
        }

        InputStream limitedStream = maxBytes > 0
                ? new SizeBoundedInputStream(responseStream, maxBytes, responseStream::abort)
                : responseStream;

        return new InputStreamResource(limitedStream) {
            @Override
            public String getFilename() {
                return storedFileName;
            }

            @Override
            public long contentLength() {
                return contentLength != null ? contentLength : -1L;
            }
        };
    }

    private void abortQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ex) {
            log.debug("Failed to close S3 stream", ex);
        }
    }

    private static class SizeBoundedInputStream extends FilterInputStream {

        private final long maxBytes;
        private final Runnable abortAction;
        private long totalRead;

        SizeBoundedInputStream(InputStream delegate, long maxBytes, Runnable abortAction) {
            super(delegate);
            this.maxBytes = maxBytes;
            this.abortAction = abortAction != null ? abortAction : () -> {};
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                registerBytesRead(1);
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                registerBytesRead(read);
            }
            return read;
        }

        private void registerBytesRead(int count) throws IOException {
            totalRead += count;
            if (totalRead > maxBytes) {
                abortAction.run();
                throw new IOException("Object exceeded max allowed size of " + maxBytes + " bytes");
            }
        }
    }
}
