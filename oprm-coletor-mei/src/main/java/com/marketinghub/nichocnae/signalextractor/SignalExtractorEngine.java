package com.marketinghub.nichocnae.signalextractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Extrai sinais estruturados de snapshots curtos sem publicar metadados técnicos no artefato final. */
@Component
public class SignalExtractorEngine {
    private static final int MAX_SIGNALS = 8;
    private static final int MAX_EVIDENCE_LENGTH = 280;

    /** Analisa título, snippet e trecho curto para produzir sinais classificados da etapa cinco. */
    public List<ExtractedSignal> extract(SignalExtractorPending pending) {
        String evidence = normalizeEvidence(pending);
        String normalized = evidence.toLowerCase(Locale.ROOT);
        Map<String, ExtractedSignal> signals = new LinkedHashMap<>();
        addIfPresent(signals, normalized, evidence, List.of("agenda", "atendimento", "cliente", "serviço", "rotina"),
                "ROUTINE_TASK", "Gerenciar rotina de atendimento e agenda do nicho", 82);
        addIfPresent(signals, normalized, evidence, List.of("whatsapp", "mensagem", "confirmar", "lembrete"),
                "COMMERCIAL_TASK", "Usar mensagens para confirmar, orientar ou recuperar clientes", 86);
        addIfPresent(signals, normalized, evidence, List.of("falta", "cancelamento", "atraso", "caos", "falha", "problema", "dificuldade"),
                "PAIN_POINT", "Reduzir falhas operacionais que geram perda de agenda ou retrabalho", 84);
        addIfPresent(signals, normalized, evidence, List.of("fidel", "recorr", "retorno", "relacionamento"),
                "RESULT_DESIRED", "Aumentar fidelização e retorno de clientes", 80);
        addIfPresent(signals, normalized, evidence, List.of("pergunta", "como", "dúvida", "orientação"),
                "CUSTOMER_QUESTION", "Responder dúvidas práticas que bloqueiam decisão ou execução", 76);
        addIfPresent(signals, normalized, evidence, List.of("organizar", "controle", "processo"),
                "CONTEXT_MARKER", "Sinal de organização e controle observado na rotina", 74);
        addSolutionRiskIfPresent(signals, normalized, evidence);
        addIfPresent(signals, normalized, evidence, List.of("qualidade", "higiene", "segurança", "confiança"),
                "PROOF_SIGNAL", "Usar qualidade, higiene ou confiança como prova operacional", 78);
        if (signals.isEmpty() && StringUtils.hasText(evidence)) {
            signals.put("LANGUAGE_MARKER|fallback", new ExtractedSignal(
                    "LANGUAGE_MARKER", "Vocabulário público do nicho identificado para síntese", evidence, 60));
        }
        return signals.values().stream().limit(MAX_SIGNALS).toList();
    }

    /** Adiciona risco de solução quando a evidência contém termos explícitos de solução precoce. */
    private void addSolutionRiskIfPresent(Map<String, ExtractedSignal> signals, String normalized, String evidence) {
        boolean present = containsSolutionLanguage(normalized);
        if (!present) {
            return;
        }
        signals.putIfAbsent("SOLUTION_LANGUAGE_RISK|Termo de solução detectado antes da aprovação da rotina", new ExtractedSignal(
                "SOLUTION_LANGUAGE_RISK", "Termo de solução detectado antes da aprovação da rotina", evidence, 70));
    }

    /** Detecta termos de solução com cuidado para não confundir sílabas comuns como "ia" em palavras maiores. */
    private boolean containsSolutionLanguage(String normalized) {
        return normalized.contains("inteligência artificial")
                || normalized.contains("automação")
                || normalized.contains("sistema")
                || normalized.contains("software")
                || normalized.contains("ferramenta")
                || normalized.contains("curso")
                || containsWholeToken(normalized, "ia")
                || containsWholeToken(normalized, "app");
    }

    /** Verifica token inteiro após normalizar pontuação para evitar falso positivo por substring. */
    private boolean containsWholeToken(String text, String token) {
        String tokenized = " " + text.replaceAll("[^\\p{L}\\p{Nd}]+", " ").replaceAll("\\s+", " ").trim() + " ";
        return tokenized.contains(" " + token + " ");
    }

    /** Adiciona um sinal quando o conteúdo contém algum indicador textual do grupo de palavras-chave. */
    private void addIfPresent(
            Map<String, ExtractedSignal> signals,
            String normalized,
            String evidence,
            List<String> keywords,
            String signalType,
            String signalText,
            Integer confidenceScore) {
        boolean present = keywords.stream().anyMatch(normalized::contains);
        if (!present) {
            return;
        }
        signals.putIfAbsent(signalType + "|" + signalText, new ExtractedSignal(signalType, signalText, evidence, confidenceScore));
    }

    /** Monta evidência curta a partir dos campos públicos permitidos, sem carregar HTML completo. */
    private String normalizeEvidence(SignalExtractorPending pending) {
        List<String> parts = new ArrayList<>();
        addPart(parts, pending.sourceTitle());
        addPart(parts, pending.snippet());
        addPart(parts, pending.shortExcerpt());
        String evidence = String.join(" — ", parts).replaceAll("\\s+", " ").trim();
        if (evidence.length() > MAX_EVIDENCE_LENGTH) {
            return evidence.substring(0, MAX_EVIDENCE_LENGTH).trim();
        }
        return evidence;
    }

    /** Inclui parte textual quando ela contém conteúdo útil para extração. */
    private void addPart(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }
}
