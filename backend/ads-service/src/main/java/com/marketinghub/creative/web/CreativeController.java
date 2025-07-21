package com.marketinghub.creative.web;

import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.dto.CreativeDto;
import com.marketinghub.creative.mapper.CreativeMapper;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.creative.dto.UpdateCreativeLabelsRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for creatives.
 */
@RestController
public class CreativeController {
    private final CreativeService service;
    private final CreativeMapper mapper;

    public CreativeController(CreativeService service, CreativeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/api/experiments/{id}/creatives")
    public CreativeDto create(@PathVariable Long id, @RequestBody CreateCreativeRequest request) {
        return mapper.toDto(service.create(id, request));
    }

    @GetMapping("/api/experiments/{id}/creatives")
    public List<CreativeDto> list(@PathVariable Long id) {
        return StreamSupport.stream(service.listByExperiment(id).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    @PutMapping("/api/creatives/{id}")
    public CreativeDto update(@PathVariable Long id, @RequestBody CreateCreativeRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @DeleteMapping("/api/creatives/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/api/creatives/{id}/labels")
    public CreativeDto patchLabels(@PathVariable Long id,
                                   @RequestBody UpdateCreativeLabelsRequest request) {
        return mapper.toDto(service.updateLabels(id,
                request.getAngles(), request.getVisualProofs(), request.getEmotionalTriggers()));
    }

    @PostMapping("/api/assets")
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        return service.uploadImage(file);
    }

    @GetMapping("/api/creatives/{id}/preview")
    public String preview(@PathVariable Long id) throws Exception {
        return service.preview(id);
    }
}
