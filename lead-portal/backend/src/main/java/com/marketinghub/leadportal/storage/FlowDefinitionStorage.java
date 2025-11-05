package com.marketinghub.leadportal.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.config.FlowStorageProperties;
import com.marketinghub.leadportal.model.Flow;
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

/**
 * Handles persistence of flow definitions in a JSON file so they survive
 * application restarts.
 */
@Component
public class FlowDefinitionStorage {

    private static final TypeReference<List<Flow>> LIST_OF_FLOWS = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path storageFile;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FlowDefinitionStorage(ObjectMapper objectMapper, FlowStorageProperties properties) {
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
            throw new FlowStorageException("Failed to prepare flow storage directory", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Flow> loadAll() {
        lock.readLock().lock();
        try {
            if (!Files.exists(storageFile)) {
                return new LinkedHashMap<>();
            }
            try (InputStream input = Files.newInputStream(storageFile)) {
                List<Flow> flows = objectMapper.readValue(input, LIST_OF_FLOWS);
                Map<String, Flow> indexed = new LinkedHashMap<>();
                for (Flow flow : flows) {
                    indexed.put(flow.slug(), flow);
                }
                return indexed;
            }
        } catch (IOException ex) {
            throw new FlowStorageException("Failed to read flow storage", ex);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveAll(Iterable<Flow> flows) {
        lock.writeLock().lock();
        try (OutputStream output = Files.newOutputStream(
                storageFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            List<Flow> snapshot = new ArrayList<>();
            for (Flow flow : flows) {
                snapshot.add(flow);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output, snapshot);
        } catch (IOException ex) {
            throw new FlowStorageException("Failed to persist flow definitions", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
