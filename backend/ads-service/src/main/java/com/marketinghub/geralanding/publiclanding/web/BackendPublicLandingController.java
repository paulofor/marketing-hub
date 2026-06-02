package com.marketinghub.geralanding.publiclanding.web;

import com.marketinghub.geralanding.publiclanding.service.BackendPublicLandingService;
import com.marketinghub.geralanding.publiclanding.service.approveEndPublish.PublicLandingPublicationResponse;
import com.marketinghub.geralanding.publiclanding.service.pending.RecordPublicLandingPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe endpoints públicos de aprovação e publicação da landing final do GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendPublicLandingController {
    private final BackendPublicLandingService service;

    /** Cria o controller com o service responsável pela publicação da landing pública. */
    public BackendPublicLandingController(BackendPublicLandingService service) {
        this.service = service;
    }

    /** Aprova e publica a landing final no Lead Portal, mantendo alias legado com `and` para compatibilidade. */
    @PostMapping({
        "/experiments/{experimentId}/geralanding/landing/approve-end-publish",
        "/experiments/{experimentId}/geralanding/landing/approve-and-publish"
    })
    public ResponseEntity<PublicLandingPublicationResponse> start(@PathVariable Long experimentId) {
        return ResponseEntity.ok(service.start(experimentId));
    }

    /** Responde à rota canônica de listagem informando que a landing pública não possui fila própria. */
    @GetMapping("/experiments/{experimentId}/geralanding/landing/stage-executions")
    public ResponseEntity<Object> listStageExecutions(@PathVariable Long experimentId) {
        return ResponseEntity.ok(service.listStageExecutions(experimentId));
    }

    /** Responde à rota canônica interna informando que não há pendências assíncronas para landing pública. */
    @GetMapping("/internal/geralanding/landing/stage-executions/pending")
    public List<RecordPublicLandingPending> pending() {
        return service.pending();
    }

    /** Responde à rota canônica interna informando que a landing pública não recebe prompt de IA. */
    @PostMapping("/internal/geralanding/landing/stage-executions/{idJob}/recebe-prompt")
    public ResponseEntity<Void> recebePrompt(@PathVariable String idJob, @RequestBody(required = false) Object payload) {
        service.recebePrompt(idJob, payload);
        return ResponseEntity.accepted().build();
    }

    /** Responde à rota canônica interna informando que a landing pública não recebe resposta de IA. */
    @PostMapping("/internal/geralanding/landing/stage-executions/{idJob}/recebe-resposta")
    public ResponseEntity<Void> recebeResposta(@PathVariable String idJob, @RequestBody(required = false) Object payload) {
        service.recebeResposta(idJob, payload);
        return ResponseEntity.accepted().build();
    }

    /** Responde à rota canônica de detalhe informando que a landing pública não possui job próprio. */
    @GetMapping("/experiments/{experimentId}/geralanding/landing/stage-executions/{idJob}")
    public ResponseEntity<Object> detailStageExecution(@PathVariable Long experimentId, @PathVariable String idJob) {
        return ResponseEntity.ok(service.detailStageExecution(experimentId, idJob));
    }
}
