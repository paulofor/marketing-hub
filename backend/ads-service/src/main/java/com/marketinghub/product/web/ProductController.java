package com.marketinghub.product.web;

import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for products.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    private final ProductMapper mapper;

    public ProductController(ProductService service, ProductMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ProductDto create(@RequestBody CreateProductRequest request) {
        return mapper.toDto(service.createProduct(request));
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return mapper.toDto(service.getProduct(id));
    }

    @GetMapping
    public List<ProductDto> list() {
        return StreamSupport.stream(service.listProducts().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
