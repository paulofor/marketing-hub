package com.marketinghub.metadata;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entities/{entityName}/attributes")
public class EntityMetadataController {
    private final EntityMetadataService service;

    public EntityMetadataController(EntityMetadataService service) {
        this.service = service;
    }

    @GetMapping
    public List<String> list(@PathVariable String entityName) {
        return service.listAttributes(entityName);
    }
}
