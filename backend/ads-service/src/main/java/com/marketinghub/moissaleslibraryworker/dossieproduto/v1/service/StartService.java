package com.marketinghub.moissaleslibraryworker.dossieproduto.v1.service;

import org.springframework.stereotype.Service;

/** Centraliza a regra de início do pipeline de dossiê de produto MOIS v1. */
@Service
public class StartService {

    /** Inicia o fluxo do pipeline para o código de produto informado. */
    public void start(String codigoProduto) {
        // A persistência e o enfileiramento serão conectados quando o contrato completo do pipeline for definido.
    }
}
