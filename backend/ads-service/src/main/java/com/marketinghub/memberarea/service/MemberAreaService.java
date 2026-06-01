package com.marketinghub.memberarea.service;

import com.marketinghub.memberarea.MemberArea;
import com.marketinghub.memberarea.dto.CreateMemberAreaRequest;
import com.marketinghub.repository.jpa.memberarea.MemberAreaRepository;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing member areas.
 */
@Service
public class MemberAreaService {
    private final MemberAreaRepository repository;
    private final ProductRepository productRepository;

    public MemberAreaService(MemberAreaRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    @Transactional
    public MemberArea createMemberArea(CreateMemberAreaRequest request) {
        MemberArea memberArea = MemberArea.builder()
                .product(resolveProduct(request.getProductId()))
                .name(request.getName())
                .accessUrl(request.getAccessUrl())
                .description(request.getDescription())
                .build();
        return repository.save(memberArea);
    }

    public MemberArea getMemberArea(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<MemberArea> listMemberAreas() {
        return repository.findAll();
    }

    private Product resolveProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("productId is required");
        }
        return productRepository.findById(id).orElseThrow();
    }
}
