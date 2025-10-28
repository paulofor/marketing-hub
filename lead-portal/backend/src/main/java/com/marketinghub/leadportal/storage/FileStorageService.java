package com.marketinghub.leadportal.storage;

import com.marketinghub.leadportal.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final StorageProperties properties;
    private Path rootLocation;

    public FileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new StorageException("Could not initialize storage directory", ex);
        }
    }

    public String store(MultipartFile file, String identifier) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.isEmpty()) {
            filename = "upload";
        }
        String storedFileName = identifier + "-" + filename;
        Path destinationFile = rootLocation.resolve(storedFileName).normalize();
        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            throw new StorageException("Failed to store file", ex);
        }
    }

    public Resource loadAsResource(String storedFileName) {
        try {
            Path file = rootLocation.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new StorageFileNotFoundException("File not found: " + storedFileName);
        } catch (MalformedURLException ex) {
            throw new StorageFileNotFoundException("File not found: " + storedFileName, ex);
        }
    }
}
