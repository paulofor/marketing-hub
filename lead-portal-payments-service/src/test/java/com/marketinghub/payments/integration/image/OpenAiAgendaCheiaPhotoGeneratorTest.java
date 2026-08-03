package com.marketinghub.payments.integration.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Valida o contrato do gerador de lotes fotográficos do Agenda Cheia. */
class OpenAiAgendaCheiaPhotoGeneratorTest {

    /** Confirma o envio do snapshot fixo GPT Image 2 e a leitura da imagem retornada. */
    @Test
    void usesPinnedGptImage2Snapshot() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        String imageResponse = encodedImage();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/images/generations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"data\":[{\"b64_json\":\"" + imageResponse + "\"}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            OpenAiAgendaCheiaPhotoGenerator generator = new OpenAiAgendaCheiaPhotoGenerator(
                    "test-key", "http://localhost:" + server.getAddress().getPort(),
                    "gpt-image-2-2026-04-21", new ObjectMapper());

            BufferedImage image = generator.generate("test-execution", 0);

            assertThat(image.getWidth()).isEqualTo(2);
            assertThat(requestBody.get()).contains("\"model\":\"gpt-image-2-2026-04-21\"");
            assertThat(requestBody.get()).contains("\"quality\":\"high\"");
            assertThat(requestBody.get()).contains("\"size\":\"1024x1024\"");
        } finally {
            server.stop(0);
        }
    }

    /** Cria uma imagem mínima válida para simular a resposta do provedor. */
    private static String encodedImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.PINK.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }
}
