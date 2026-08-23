package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Valida o parser da resposta HTTP real consumida na reconciliação Pepper. */
class PepperTransactionClientTest {

    /** Interpreta hash, pagamento, oferta, valor e cliente na resposta individual do provedor. */
    @Test
    void parsesPaidTransactionFromProviderHttpResponse() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/public/v1/transactions/tx-real-67", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, """
                    {
                      "hash": "tx-real-67",
                      "payment_status": "paid",
                      "amount": 6700,
                      "currency": "BRL",
                      "offer": {"hash": "owm6x", "title": "Método MUSA em 7 dias"},
                      "customer": {"email": "cliente@sandbox.local"}
                    }
                    """);
        });
        server.start();
        try {
            PepperTransactionClient client = new PepperTransactionClient(
                    new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/public/v1",
                    "token-sandbox",
                    "owm6x",
                    14,
                    6700,
                    "BRL");

            var result = client.findPaidTransactionByHash("tx-real-67");

            assertThat(result.scannedTransactions()).isEqualTo(1);
            assertThat(result.paidTransactions()).singleElement().satisfies(transaction -> {
                assertThat(transaction.transactionId()).isEqualTo("tx-real-67");
                assertThat(transaction.buyerEmail()).isEqualTo("cliente@sandbox.local");
                assertThat(transaction.offerHash()).isEqualTo("owm6x");
                assertThat(transaction.amount()).isEqualTo(6700);
                assertThat(transaction.currency()).isEqualTo("BRL");
            });
            assertThat(authorization).hasValue("Bearer token-sandbox");
        } finally {
            server.stop(0);
        }
    }

    /** Rejeita no próprio parser status, oferta e valor que não satisfazem o contrato comercial. */
    @Test
    void filtersInvalidTransactionsFromProviderListResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/public/v1/transactions", exchange -> respond(exchange, 200, """
                {
                  "data": [
                    {"hash":"pendente","payment_status":"pending","amount":6700,"currency":"BRL","offer":{"hash":"owm6x"},"customer":{"email":"a@sandbox.local"}},
                    {"hash":"outra-oferta","payment_status":"paid","amount":6700,"currency":"BRL","offer":{"hash":"outra"},"customer":{"email":"b@sandbox.local"}},
                    {"hash":"valor-baixo","payment_status":"paid","amount":6699,"currency":"BRL","offer":{"hash":"owm6x"},"customer":{"email":"c@sandbox.local"}},
                    {"hash":"valor-alto","payment_status":"paid","amount":6701,"currency":"BRL","offer":{"hash":"owm6x"},"customer":{"email":"e@sandbox.local"}},
                    {"hash":"moeda-errada","payment_status":"paid","amount":6700,"currency":"USD","offer":{"hash":"owm6x"},"customer":{"email":"f@sandbox.local"}},
                    {"hash":"sem-moeda","payment_status":"paid","amount":6700,"offer":{"hash":"owm6x"},"customer":{"email":"g@sandbox.local"}},
                    {"hash":"valida","payment_status":"paid","amount":6700,"currency":"BRL","offer":{"hash":"owm6x","title":"Método MUSA"},"customer":{"email":"d@sandbox.local"}}
                  ]
                }
                """));
        server.start();
        try {
            PepperTransactionClient client = new PepperTransactionClient(
                    new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/public/v1",
                    "token-sandbox",
                    "owm6x",
                    14,
                    6700,
                    "BRL");

            var result = client.findPaidTransactions("d@sandbox.local");

            assertThat(result.scannedTransactions()).isEqualTo(7);
            assertThat(result.paidTransactions()).singleElement()
                    .extracting(PepperPaidTransaction::transactionId)
                    .isEqualTo("valida");
        } finally {
            server.stop(0);
        }
    }

    /** Responde JSON ao cliente HTTP de teste sem depender de integração externa. */
    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }
}
