package com.marketinghub.pde.service;

import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Valida a entrada curta da candidata comercial sem substituir o contrato privado ou as missões. */
public final class MusaV14EntryContract {
    public static final String VERSION = "musa-pde-entry-v14-primeiro-ajuste-aplicavel";
    private static final Set<String> REQUIRED = Set.of("occasion", "existingSelection");
    private static final Set<String> ALLOWED = Set.of("occasion", "existingSelection", "optionalNote");

    /** Mantém a validação pura, independente de sessão, provedor e persistência. */
    private MusaV14EntryContract() {}

    /** Exige os dois contextos reais e limita a observação opcional sem inventar personalização. */
    public static void validate(Map<String, String> answers) {
        if (answers == null || !answers.keySet().containsAll(REQUIRED)
                || !ALLOWED.containsAll(answers.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe somente ocasião, combinação disponível e observação opcional.");
        }
        for (var entry : answers.entrySet()) {
            var value = entry.getValue();
            int limit = "occasion".equals(entry.getKey()) ? 160 : 300;
            if (value == null || value.length() > limit
                    || (REQUIRED.contains(entry.getKey()) && value.isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira o campo " + entry.getKey() + " e seu limite de " + limit + " caracteres.");
            }
        }
    }
}
