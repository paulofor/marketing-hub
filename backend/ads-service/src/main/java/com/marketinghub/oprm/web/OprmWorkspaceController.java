package com.marketinghub.oprm.web;

import com.marketinghub.oprm.dto.OprmRoutineWorkspaceResponseDto;
import com.marketinghub.oprm.service.OprmArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm/workspace")
@RequiredArgsConstructor
public class OprmWorkspaceController {
    private final OprmArtifactService artifactService;

    @GetMapping("/routine/{occupationSeedRef}")
    public OprmRoutineWorkspaceResponseDto getRoutineWorkspace(@PathVariable String occupationSeedRef) {
        return artifactService.getRoutineWorkspace(occupationSeedRef);
    }
}
