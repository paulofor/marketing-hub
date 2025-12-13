package com.marketinghub.microservice.exception.web;

import com.marketinghub.microservice.exception.dto.MicroserviceExceptionDto;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionRequest;
import com.marketinghub.microservice.exception.mapper.MicroserviceExceptionMapper;
import com.marketinghub.microservice.exception.service.MicroserviceExceptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class MicroserviceExceptionController {
    private final MicroserviceExceptionService service;
    private final MicroserviceExceptionMapper mapper;

    public MicroserviceExceptionController(MicroserviceExceptionService service,
                                           MicroserviceExceptionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/microservices/{microserviceId}/exceptions")
    public MicroserviceExceptionDto logException(@PathVariable Long microserviceId,
                                                 @Valid @RequestBody MicroserviceExceptionRequest request) {
        return mapper.toDto(service.logException(microserviceId, request));
    }

    @GetMapping("/microservices/{microserviceId}/exceptions")
    public Page<MicroserviceExceptionDto> listForMicroservice(@PathVariable Long microserviceId,
                                                              @RequestParam(value = "severity", required = false) String severity,
                                                              @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(microserviceId, severity, pageable).map(mapper::toDto);
    }

    @GetMapping("/microservice-exceptions")
    public Page<MicroserviceExceptionDto> listAll(@RequestParam(value = "microserviceId", required = false) Long microserviceId,
                                                  @RequestParam(value = "severity", required = false) String severity,
                                                  @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(microserviceId, severity, pageable).map(mapper::toDto);
    }
}
