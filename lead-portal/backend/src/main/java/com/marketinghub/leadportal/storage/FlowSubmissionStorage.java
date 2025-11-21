package com.marketinghub.leadportal.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.config.SubmissionStorageProperties;
import com.marketinghub.leadportal.model.FlowSubmission;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.stereotype.Component;

@Component
public class FlowSubmissionStorage {

    private static final TypeReference<List<FlowSubmission>> LIST_OF_SUBMISSIONS = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path storageFile;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FlowSubmissionStorage(ObjectMapper objectMapper, SubmissionStorageProperties properties) {
        this.objectMapper = objectMapper;
        this.storageFile = Paths.get(properties.getLocation()).toAbsolutePath();
    }

    @PostConstruct
    void ensureDirectoryExists() {
        lock.writeLock().lock();
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ex) {
            throw new FlowStorageException("Failed to prepare submission storage directory", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<java.util.UUID, FlowSubmission> loadAll() {
        lock.readLock().lock();
        try {
            if (!Files.exists(storageFile)) {
                return new LinkedHashMap<>();
            }
            try (InputStream input = Files.newInputStream(storageFile)) {
                List<FlowSubmission> submissions = objectMapper.readValue(input, LIST_OF_SUBMISSIONS);
                Map<java.util.UUID, FlowSubmission> indexed = new LinkedHashMap<>();
                for (FlowSubmission submission : submissions) {
                    indexed.put(submission.id(), submission);
                }
                return indexed;
            }
        } catch (IOException ex) {
            throw new FlowStorageException("Failed to read submission storage", ex);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveAll(Iterable<FlowSubmission> submissions) {
        lock.writeLock().lock();
        try (OutputStream output = Files.newOutputStream(
                storageFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            List<FlowSubmission> snapshot = new ArrayList<>();
            for (FlowSubmission submission : submissions) {
                snapshot.add(submission);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output, snapshot);
        } catch (IOException ex) {
            throw new FlowStorageException("Failed to persist submissions", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
