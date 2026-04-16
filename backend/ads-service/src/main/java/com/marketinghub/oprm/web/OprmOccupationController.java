package com.marketinghub.oprm.web;

import com.marketinghub.oprm.dto.OprmOccupationResponseDto;
import com.marketinghub.oprm.dto.OprmOccupationUpsertRequestDto;
import com.marketinghub.oprm.service.OprmOccupationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm/occupations")
@RequiredArgsConstructor
public class OprmOccupationController {
    private final OprmOccupationService service;

    @GetMapping
    public List<OprmOccupationResponseDto> list() {
        return service.listOccupations();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OprmOccupationResponseDto create(@Valid @RequestBody OprmOccupationUpsertRequestDto request) {
        return service.createOccupation(request);
    }

    @PutMapping("/{occupationId}")
    public OprmOccupationResponseDto update(@PathVariable UUID occupationId,
                                            @Valid @RequestBody OprmOccupationUpsertRequestDto request) {
        return service.updateOccupation(occupationId, request);
    }

    @DeleteMapping("/{occupationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID occupationId) {
        service.deleteOccupation(occupationId);
    }
}
