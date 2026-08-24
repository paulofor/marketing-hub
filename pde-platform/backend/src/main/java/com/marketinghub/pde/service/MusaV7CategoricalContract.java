package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.ProductExperienceResponse.MissionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.MissionInteractionFieldDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.PublicDiagnosticQuestionDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Valida as escolhas categoriais da MUSA v7 a partir do contrato canônico entregue à interface. */
public final class MusaV7CategoricalContract {
    private static final String NEUTRAL_CHOICE = "Manter como está por enquanto";

    /** Impede instanciação porque o contrato é imutável e compartilhado. */
    private MusaV7CategoricalContract() {}

    /** Exige exatamente as perguntas e opções declaradas para a missão canônica recebida. */
    public static void validateMission(MissionDto mission, Map<String, String> answers) {
        if (mission == null || mission.interaction() == null || mission.interaction().fields() == null
                || mission.interaction().fields().isEmpty()) {
            throw new IllegalArgumentException("Missão MUSA v7 sem contrato de interação: "
                    + (mission == null ? "desconhecida" : mission.id()));
        }
        Map<String, Set<String>> allowedValues = mission.interaction().fields().stream()
                .collect(Collectors.toMap(
                        MissionInteractionFieldDto::key,
                        field -> Set.copyOf(field.options()),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        validateExactContract(allowedValues, answers, "missão " + mission.id());
    }

    /** Exige exatamente as quatro escolhas declaradas pelo diagnóstico público canônico. */
    public static void validatePublicDiagnostic(
            List<PublicDiagnosticQuestionDto> questions, Map<String, String> answers) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Diagnóstico público MUSA v7 sem perguntas canônicas");
        }
        Map<String, Set<String>> allowedValues = questions.stream()
                .collect(Collectors.toMap(
                        PublicDiagnosticQuestionDto::key,
                        question -> Set.copyOf(question.options()),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        validateExactContract(allowedValues, answers, "diagnóstico público");
    }

    /** Rejeita ausência, chave excedente, texto livre ou opção pertencente a outra pergunta. */
    private static void validateExactContract(
            Map<String, Set<String>> allowedValuesByKey,
            Map<String, String> answers,
            String contractLabel) {
        if (answers == null || !answers.keySet().equals(allowedValuesByKey.keySet())) {
            throw new IllegalArgumentException(
                    "A MUSA v7 exige todas e somente as escolhas categoriais do " + contractLabel);
        }
        answers.forEach((key, value) -> {
            Set<String> allowedValues = allowedValuesByKey.get(key);
            if (value == null || value.isBlank()
                    || (!NEUTRAL_CHOICE.equals(value) && !allowedValues.contains(value))) {
                throw new IllegalArgumentException("Escolha categorial MUSA v7 inválida para: " + key);
            }
        });
    }
}
