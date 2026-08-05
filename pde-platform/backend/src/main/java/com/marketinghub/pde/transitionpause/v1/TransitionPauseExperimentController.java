package com.marketinghub.pde.transitionpause.v1;

import com.marketinghub.pde.dto.FunnelEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o contrato supervisionado da Pausa de Transição sem publicar ou comprar tráfego. */
@RestController
@RequestMapping("/api/pde/transition-pause/v1")
public class TransitionPauseExperimentController {
    private static final Logger log = LoggerFactory.getLogger(TransitionPauseExperimentController.class);
    private final TransitionPauseExperimentService service;

    /** Recebe o serviço que governa o protocolo experimental. */
    public TransitionPauseExperimentController(TransitionPauseExperimentService service) {
        this.service = service;
    }

    /** Retorna o contrato público para revisão antes da participação. */
    @GetMapping("/contract")
    public TransitionPauseContractResponse getContract() {
        return service.getContract();
    }

    /** Inicia uma sessão somente após consentimento e ciência dos limites. */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransitionPauseSessionResponse startSession(@Valid @RequestBody TransitionPauseSessionRequest request) {
        log.info("Payload bruto recebido para início da Pausa de Transição; payload={}", request);
        return service.startSession(request);
    }

    /** Registra conclusão, início da tarefa, saída ou sinal de segurança. */
    @PostMapping("/events")
    public FunnelEventResponse recordEvent(@Valid @RequestBody TransitionPauseEventRequest request) {
        log.info("Payload bruto recebido da sessão Pausa de Transição; payload={}", request);
        return service.recordOutcome(request);
    }
}
