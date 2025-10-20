package com.marketinghub.memberarea.web;

import com.marketinghub.memberarea.dto.CreateMemberAreaRequest;
import com.marketinghub.memberarea.dto.MemberAreaDto;
import com.marketinghub.memberarea.mapper.MemberAreaMapper;
import com.marketinghub.memberarea.service.MemberAreaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller exposing member areas.
 */
@RestController
@RequestMapping("/api/member-areas")
public class MemberAreaController {
    private final MemberAreaService service;
    private final MemberAreaMapper mapper;

    public MemberAreaController(MemberAreaService service, MemberAreaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public MemberAreaDto create(@RequestBody CreateMemberAreaRequest request) {
        return mapper.toDto(service.createMemberArea(request));
    }

    @GetMapping("/{id}")
    public MemberAreaDto get(@PathVariable Long id) {
        return mapper.toDto(service.getMemberArea(id));
    }

    @GetMapping
    public List<MemberAreaDto> list() {
        return StreamSupport.stream(service.listMemberAreas().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
