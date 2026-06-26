package com.marketinghub.moissaleslibraryworker.dossieproduto.v1.controller;

import com.marketinghub.moissaleslibraryworker.dossieproduto.v1.service.StartService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o ponto HTTP para iniciar o pipeline de dossiê de produto MOIS v1. */
@Validated
@RestController
@RequestMapping("/api/internal/mois/dossieproduto/v1")
@RequiredArgsConstructor
public class StartController {

    private final StartService service;

    /** Recebe o código do produto e delega o início do pipeline ao serviço. */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(@RequestParam("codigoProduto") @NotBlank String codigoProduto) {
        service.start(codigoProduto);
    }
}
