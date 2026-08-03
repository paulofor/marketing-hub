package com.marketinghub.payments.controller;

import com.marketinghub.payments.dto.AgendaCheiaBriefingRequest;
import com.marketinghub.payments.dto.AgendaCheiaBriefingResponse;
import com.marketinghub.payments.service.AgendaCheiaPostPurchaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a confirmação de pagamento e o briefing pós-compra do Agenda Cheia. */
@RestController
@RequestMapping("/api/v1/agenda-cheia/post-purchase")
public class AgendaCheiaPostPurchaseController {
    private final AgendaCheiaPostPurchaseService service;

    /** Configura o serviço responsável pelo pós-compra. */
    public AgendaCheiaPostPurchaseController(AgendaCheiaPostPurchaseService service) {
        this.service = service;
    }

    /** Consulta o próximo passo seguro do pagamento. */
    @GetMapping
    public AgendaCheiaBriefingResponse status(@RequestParam("payment_id") String paymentId) {
        return service.paymentStatus(paymentId);
    }

    /** Recebe o briefing que inicia a personalização. */
    @PostMapping("/briefing")
    public AgendaCheiaBriefingResponse submit(@Valid @RequestBody AgendaCheiaBriefingRequest request) {
        return service.submit(request);
    }
}
