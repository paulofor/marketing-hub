package com.marketinghub.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Minimiza dados pessoais em payloads financeiros preservados para auditoria técnica. */
@Component
public class PaymentAuditPayloadSanitizer {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuditPayloadSanitizer.class);
    private static final String REDACTED_VALUE = "[REDACTED]";
    private static final Set<String> EMAIL_FIELD_NAMES = Set.of(
            "email", "buyeremail", "payeremail", "recipientemail", "submissionemail");

    private final ObjectMapper objectMapper;

    /** Recebe o serializador usado para interpretar e reconstruir a evidência financeira. */
    public PaymentAuditPayloadSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Remove e-mails do JSON e substitui payload inválido por prova de integridade sem conteúdo pessoal. */
    public String minimize(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return rawPayload;
        }
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            redactEmailFields(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            String payloadHash = sha256Hex(rawPayload);
            log.warn(
                    "Payload financeiro inválido foi minimizado integralmente; payloadLength={}, payloadHash={}",
                    rawPayload.length(),
                    payloadHash,
                    ex);
            return "{\"auditPayloadStatus\":\"UNPARSEABLE_REDACTED\",\"sha256\":\""
                    + payloadHash
                    + "\"}";
        }
    }

    /** Percorre objetos e listas para remover e-mails em qualquer profundidade do contrato do provedor. */
    private void redactEmailFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            node.forEach(this::redactEmailFields);
            return;
        }
        if (!node.isObject()) {
            return;
        }
        ObjectNode objectNode = (ObjectNode) node;
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (isEmailField(field.getKey())) {
                objectNode.put(field.getKey(), REDACTED_VALUE);
            } else {
                redactEmailFields(field.getValue());
            }
        }
    }

    /** Reconhece variações de nome usadas por webhooks e metadados do checkout. */
    private boolean isEmailField(String fieldName) {
        String normalized = fieldName == null
                ? ""
                : fieldName.toLowerCase().replaceAll("[^a-z0-9]", "");
        return EMAIL_FIELD_NAMES.contains(normalized);
    }

    /** Calcula uma impressão segura para provar integridade sem conservar o payload ilegível. */
    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível ao minimizar payload financeiro", ex);
            throw new IllegalStateException("Não foi possível minimizar o payload financeiro", ex);
        }
    }
}
