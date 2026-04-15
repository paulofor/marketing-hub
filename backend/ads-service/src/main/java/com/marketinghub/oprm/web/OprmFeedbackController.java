package com.marketinghub.oprm.web;

import com.marketinghub.oprm.dto.OprmFeedbackHistoryEntryDto;
import com.marketinghub.oprm.dto.OprmFeedbackPublishRequestDto;
import com.marketinghub.oprm.service.OprmFeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm/feedback")
@RequiredArgsConstructor
@Validated
public class OprmFeedbackController {
    private final OprmFeedbackService service;

    @PostMapping
    public ResponseEntity<Void> publish(@Valid @RequestBody OprmFeedbackPublishRequestDto request) {
        service.publishFeedback(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/history")
    public List<OprmFeedbackHistoryEntryDto> listHistory(@RequestParam @NotBlank String occupationName,
                                                         @RequestParam @NotBlank String personaLabel) {
        return service.listHistory(occupationName, personaLabel);
    }
}
