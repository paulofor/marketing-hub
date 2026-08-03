package com.marketinghub.payments.controller;

import com.marketinghub.payments.dto.AgendaCheiaBriefingRequest;
import com.marketinghub.payments.dto.AgendaCheiaBriefingResponse;
import com.marketinghub.payments.service.AgendaCheiaKitProductionService;
import com.marketinghub.payments.service.AgendaCheiaPostPurchaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

/** Expõe a confirmação de pagamento e o briefing pós-compra do Agenda Cheia. */
@RestController
@RequestMapping("/api/v1/agenda-cheia/post-purchase")
public class AgendaCheiaPostPurchaseController {
    private final AgendaCheiaPostPurchaseService service;
    private final AgendaCheiaKitProductionService productionService;

    /** Configura o serviço responsável pelo pós-compra. */
    public AgendaCheiaPostPurchaseController(
            AgendaCheiaPostPurchaseService service,
            AgendaCheiaKitProductionService productionService) {
        this.service = service;
        this.productionService = productionService;
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

    /** Entrega o ZIP aprovado usando somente token público opaco. */
    @GetMapping("/deliveries/{token}/download")
    public ResponseEntity<Resource> download(@PathVariable("token") String token) {
        FileSystemResource resource = new FileSystemResource(productionService.artifact(token));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("agenda-cheia-nail-design.zip").build().toString())
                .body(resource);
    }
}
