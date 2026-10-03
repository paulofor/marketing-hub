package com.marketinghub.payments.service.kit;

import java.util.List;

/** Declara o repertório funcional e a identidade visual de uma versão de kit por profissão. */
public record CapellaKitProfile(
        String code, String profession, String productName, String serviceExamples,
        String libraryDirectory, List<String> headlines, List<String> calendarDays) {
    /** Valida e congela o contrato para impedir repertório incompleto ou caminho arbitrário. */
    public CapellaKitProfile {
        if (code == null || !code.matches("[a-z]+-v[0-9]+")
                || profession == null || profession.isBlank()
                || productName == null || productName.isBlank()
                || serviceExamples == null || serviceExamples.isBlank()
                || libraryDirectory == null
                || !(libraryDirectory.isEmpty() || libraryDirectory.equals(code))
                || headlines == null || headlines.size() != 10
                || calendarDays == null || calendarDays.size() != 7
                || headlines.stream().anyMatch(value -> value == null || value.isBlank())
                || calendarDays.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Contrato de kit por profissão inválido");
        }
        headlines = List.copyOf(headlines);
        calendarDays = List.copyOf(calendarDays);
    }
}
