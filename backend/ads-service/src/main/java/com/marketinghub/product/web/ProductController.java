package com.marketinghub.product.web;

import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Responsabilidade: expor endpoints REST do cadastro comercial de produtos.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    private final ProductMapper mapper;

    /** Inicializa o controller com serviço de produto e mapper de resposta. */
    public ProductController(ProductService service, ProductMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Cadastra um novo produto comercial no Marketing Hub. */
    @PostMapping
    public ProductDto create(@RequestBody CreateProductRequest request) {
        return mapper.toDto(service.createProduct(request));
    }

    /** Retorna um produto comercial pelo identificador interno. */
    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return mapper.toDto(service.getProduct(id));
    }

    /** Lista os produtos comerciais cadastrados no Marketing Hub. */
    @GetMapping
    public List<ProductDto> list() {
        return StreamSupport.stream(service.listProducts().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
