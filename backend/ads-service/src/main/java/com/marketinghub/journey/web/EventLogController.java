package com.marketinghub.journey.web;

import com.marketinghub.journey.dto.EventLogRequest;
import com.marketinghub.journey.dto.EventLogResponse;
import com.marketinghub.journey.mapper.JourneyMapper;
import com.marketinghub.journey.service.EventLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint ingesting events across channels.
 */
@RestController
@RequestMapping("/api/events")
public class EventLogController {
    private final EventLogService eventLogService;
    private final JourneyMapper mapper;

    public EventLogController(EventLogService eventLogService, JourneyMapper mapper) {
        this.eventLogService = eventLogService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventLogResponse record(@Valid @RequestBody EventLogRequest request) {
        return mapper.toEventLogResponse(eventLogService.record(request));
    }
}
