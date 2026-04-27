package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisAutomationDtos;
import com.marketinghub.mois.service.MoisHotmartRobotService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mois/automation/hotmart")
public class MoisHotmartRobotController {

    private final MoisHotmartRobotService service;

    public MoisHotmartRobotController(MoisHotmartRobotService service) {
        this.service = service;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisAutomationDtos.HotmartRobotRunResponse triggerNow() {
        return service.triggerManualRun();
    }

    @GetMapping("/runs")
    public MoisAutomationDtos.HotmartRobotRunListResponse listRuns(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.listRuns(limit);
    }
}
