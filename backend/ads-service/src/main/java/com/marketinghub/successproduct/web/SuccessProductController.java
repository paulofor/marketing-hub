package com.marketinghub.successproduct.web;

import com.marketinghub.successproduct.dto.CreateSuccessProductRequest;
import com.marketinghub.successproduct.dto.SuccessProductDto;
import com.marketinghub.successproduct.mapper.SuccessProductMapper;
import com.marketinghub.successproduct.service.SuccessProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for success products.
 */
@RestController
@RequestMapping("/api/success-products")
public class SuccessProductController {
    private final SuccessProductService service;
    private final SuccessProductMapper mapper;

    public SuccessProductController(SuccessProductService service, SuccessProductMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public SuccessProductDto create(@RequestBody CreateSuccessProductRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping("/{id}")
    public SuccessProductDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping
    public List<SuccessProductDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
