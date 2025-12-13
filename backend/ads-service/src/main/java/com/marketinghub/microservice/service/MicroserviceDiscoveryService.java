package com.marketinghub.microservice.service;

import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class MicroserviceDiscoveryService {
    private final Path composePath;
    private final String defaultHealthPath;

    public MicroserviceDiscoveryService(
            @Value("${microservice.discovery.compose-path:docker-compose.yml}") String composePath,
            @Value("${microservice.discovery.health-path:/actuator/health}") String defaultHealthPath) {
        this.composePath = Paths.get(composePath);
        this.defaultHealthPath = defaultHealthPath;
    }

    public List<DiscoveredMicroserviceDto> discoverFromCompose() {
        if (!Files.exists(composePath)) {
            return List.of();
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(composePath)) {
            Object data = yaml.load(inputStream);
            if (!(data instanceof Map<?, ?> root)) {
                return List.of();
            }

            Object servicesNode = root.get("services");
            if (!(servicesNode instanceof Map<?, ?> services)) {
                return List.of();
            }

            List<DiscoveredMicroserviceDto> discovered = new ArrayList<>();
            for (Map.Entry<?, ?> entry : services.entrySet()) {
                DiscoveredMicroserviceDto dto = toDto(entry);
                if (dto != null) {
                    discovered.add(dto);
                }
            }
            discovered.sort(Comparator.comparing(DiscoveredMicroserviceDto::serviceName));
            return discovered;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read docker-compose file at " + composePath, e);
        }
    }

    private DiscoveredMicroserviceDto toDto(Map.Entry<?, ?> entry) {
        if (!(entry.getKey() instanceof String serviceName)) {
            return null;
        }

        Object value = entry.getValue();
        if (!(value instanceof Map<?, ?> serviceDefinition)) {
            return null;
        }

        PortMapping ports = extractPortMapping(serviceDefinition.get("ports"));
        String baseUrl = buildBaseUrl(serviceName, ports);
        String image = extractImage(serviceDefinition.get("image"));

        return new DiscoveredMicroserviceDto(
                serviceName,
                image,
                ports.hostPort(),
                ports.containerPort(),
                baseUrl,
                defaultHealthPath
        );
    }

    private String extractImage(Object imageNode) {
        if (imageNode instanceof String image) {
            return image;
        }
        return null;
    }

    private PortMapping extractPortMapping(Object portsNode) {
        if (!(portsNode instanceof Iterable<?> ports)) {
            return PortMapping.EMPTY;
        }

        for (Object port : ports) {
            PortMapping parsed = parsePort(port);
            if (parsed != null) {
                return parsed;
            }
        }

        return PortMapping.EMPTY;
    }

    private PortMapping parsePort(Object port) {
        if (port instanceof Number numberPort) {
            int value = numberPort.intValue();
            return new PortMapping(value, value);
        }

        if (!(port instanceof String portString)) {
            return null;
        }

        String sanitized = portString.split("/")[0];
        String[] parts = sanitized.split(":");

        if (parts.length == 2) {
            Integer hostPort = parsePortNumber(parts[0]);
            Integer containerPort = parsePortNumber(parts[1]);
            if (hostPort != null || containerPort != null) {
                return new PortMapping(hostPort, containerPort);
            }
        }

        Integer singlePort = parsePortNumber(parts[0]);
        if (singlePort != null) {
            return new PortMapping(singlePort, singlePort);
        }

        return null;
    }

    private Integer parsePortNumber(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String buildBaseUrl(String serviceName, PortMapping portMapping) {
        if (portMapping.hostPort() != null) {
            return "http://localhost:" + portMapping.hostPort();
        }

        if (portMapping.containerPort() != null) {
            return "http://" + serviceName + ":" + portMapping.containerPort();
        }

        return "http://" + serviceName;
    }

    private record PortMapping(Integer hostPort, Integer containerPort) {
        private static final PortMapping EMPTY = new PortMapping(null, null);
    }
}
