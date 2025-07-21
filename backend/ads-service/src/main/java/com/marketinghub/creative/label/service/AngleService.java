package com.marketinghub.creative.label.service;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.dto.AngleDto;
import com.marketinghub.creative.label.dto.CreateAngleRequest;
import com.marketinghub.creative.label.repository.AngleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AngleService {
    private final AngleRepository repository;

    public AngleService(AngleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Angle create(CreateAngleRequest request) {
        Angle angle = Angle.builder()
                .name(request.getName())
                .description(request.getDescription())
                .frameType(request.getFrameType())
                .build();
        return repository.save(angle);
    }

    public Angle get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Angle> list() {
        return repository.findAll();
    }

    @Transactional
    public Angle update(Long id, AngleDto dto) {
        Angle angle = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        angle.setName(dto.getName());
        return repository.save(angle);
    }
}
