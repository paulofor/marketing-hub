package com.marketinghub.imagedeliverable.web;

import com.marketinghub.imagedeliverable.dto.CreateImageDeliverablePackageRequest;
import com.marketinghub.imagedeliverable.dto.ImageDeliverablePackageDto;
import com.marketinghub.imagedeliverable.dto.UpdateImageDeliverablePackageRequest;
import com.marketinghub.imagedeliverable.mapper.ImageDeliverablePackageMapper;
import com.marketinghub.imagedeliverable.service.ImageDeliverablePackageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints to manage image deliverable packages.
 */
@RestController
@RequestMapping("/api/image-deliverable-packages")
public class ImageDeliverablePackageController {
    private final ImageDeliverablePackageService service;
    private final ImageDeliverablePackageMapper mapper;

    public ImageDeliverablePackageController(ImageDeliverablePackageService service,
                                             ImageDeliverablePackageMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ImageDeliverablePackageDto> list() {
        return service.listAll().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ImageDeliverablePackageDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PostMapping
    public ImageDeliverablePackageDto create(@RequestBody CreateImageDeliverablePackageRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public ImageDeliverablePackageDto update(@PathVariable Long id,
                                             @RequestBody UpdateImageDeliverablePackageRequest request) {
        return mapper.toDto(service.update(id, request));
    }
}
