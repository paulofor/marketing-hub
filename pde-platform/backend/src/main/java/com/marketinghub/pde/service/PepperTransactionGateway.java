package com.marketinghub.pde.service;

import java.util.List;

/** Porta de consulta das transacoes pagas da Pepper usadas pelo fallback comercial PDE. */
public interface PepperTransactionGateway {

    /** Busca transacoes pagas na Pepper para reconciliar compras sem webhook. */
    PepperTransactionSearchResult findPaidTransactions(String search);

    /** Busca uma transacao Pepper especifica para reconciliar uma compra conhecida. */
    PepperTransactionSearchResult findPaidTransactionByHash(String transactionHash);

    /** Consulta o estado atual de uma transação para comprovar pagamento, reembolso ou chargeback. */
    PepperTransactionSnapshot findTransactionByHash(String transactionHash);

    /** Resultado da consulta de transacoes pagas na Pepper. */
    record PepperTransactionSearchResult(int scannedTransactions, List<PepperPaidTransaction> paidTransactions) {}
}
