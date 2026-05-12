package com.marketinghub.settings;

import com.marketinghub.settings.dto.GeneralSettingDto;
import com.marketinghub.settings.dto.UpdateGeneralSettingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class GeneralSettingController {
    private final GeneralSettingService service;
    public GeneralSettingController(GeneralSettingService service) {
        this.service = service;
    }

    @GetMapping("/{name}")
    public GeneralSettingDto get(@PathVariable String name) {
        return service.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "setting not found"));
    }

    @PutMapping("/{name}")
    public GeneralSettingDto upsert(@PathVariable String name, @RequestBody UpdateGeneralSettingRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        return service.upsert(name, request.value());
    }

}
