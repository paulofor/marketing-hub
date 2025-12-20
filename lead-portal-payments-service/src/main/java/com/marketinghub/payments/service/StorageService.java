package com.marketinghub.payments.service;

import com.marketinghub.payments.config.LeadPortalStorageProperties;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final LeadPortalStorageProperties properties;
    private final S3Client s3Client;

    public StorageService(LeadPortalStorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public byte[] download(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();
            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(request, ResponseTransformer.toInputStream());
            Resource resource = toResource(key, responseStream, properties.getMaxDownloadBytes());
            return resource.getContentAsByteArray();
        } catch (IOException | SdkException ex) {
            throw new IllegalStateException("Falha ao baixar arquivo do bucket", ex);
        }
    }

    public void upload(String key, byte[] data, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (SdkException ex) {
            throw new IllegalStateException("Falha ao subir arquivo para o bucket", ex);
        }
    }

    public Optional<String> resolvePublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return Optional.empty();
        }
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return Optional.empty();
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return Optional.of(normalized + "/" + objectKey);
    }

    private Resource toResource(String key,
                                ResponseInputStream<GetObjectResponse> responseStream,
                                long maxBytes) throws IOException {
        Long contentLength = responseStream.response().contentLength();
        if (maxBytes > 0 && contentLength != null && contentLength > maxBytes) {
            responseStream.abort();
            throw new IOException("Objeto '" + key + "' excede o tamanho permitido de " + maxBytes + " bytes");
        }

        InputStream limitedStream = maxBytes > 0
                ? new SizeBoundedInputStream(responseStream, maxBytes, responseStream::abort)
                : responseStream;

        return new InputStreamResource(limitedStream) {
            @Override
            public String getFilename() {
                return key;
            }

            @Override
            public long contentLength() {
                return contentLength != null ? contentLength : -1L;
            }
        };
    }

    private static class SizeBoundedInputStream extends FilterInputStream {

        private final long maxBytes;
        private final Runnable abortAction;
        private long totalRead;

        protected SizeBoundedInputStream(InputStream in, long maxBytes, Runnable abortAction) {
            super(in);
            this.maxBytes = maxBytes;
            this.abortAction = abortAction;
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read != -1) {
                registerRead(1);
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            registerRead(read > 0 ? read : 0);
            return read;
        }

        private void registerRead(int bytesRead) throws IOException {
            totalRead += bytesRead;
            if (totalRead > maxBytes) {
                abort();
                throw new IOException("Download excedeu o limite de " + maxBytes + " bytes");
            }
        }

        private void abort() {
            try {
                abortAction.run();
            } catch (Exception ex) {
                log.debug("Falha ao abortar stream", ex);
            }
        }
    }
}
