package com.marketinghub.nichocnae.meiaudiencesegmenter.web;

import com.marketinghub.nichocnae.meiaudiencesegmenter.MeiAudienceSegmenterOutput;
import com.marketinghub.nichocnae.meiaudiencesegmenter.MeiAudienceSegmenterPending;
import com.marketinghub.nichocnae.meiaudiencesegmenter.MeiAudienceSegmenterService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller operacional da segmentação comportamental MEI/autônomo no coletor OPRM. */
@RestController
@RequestMapping("/api/oprm-mei/nichocnae/mei-audience-segmenter")
public class MeiAudienceSegmenterController {
    private final MeiAudienceSegmenterService segmenterService;

    /** Inicializa o controller com o serviço operacional da segmentação MEI/autônomo. */
    public MeiAudienceSegmenterController(MeiAudienceSegmenterService segmenterService) {
        this.segmenterService = segmenterService;
    }

    /** Lista ciclos pendentes para diagnóstico operacional. */
    @GetMapping("/pending")
    public ResponseEntity<List<MeiAudienceSegmenterPending>> pending() {
        return ResponseEntity.ok(segmenterService.listPendingCycles());
    }

    /** Executa manualmente a segmentação dos ciclos pendentes. */
    @PostMapping("/process-pending")
    public ResponseEntity<List<MeiAudienceSegmenterOutput>> processPending(
            @RequestHeader(value = "X-Requested-By", defaultValue = "manual") String requestedBy) {
        return ResponseEntity.accepted().body(segmenterService.processPending(requestedBy));
    }
}
