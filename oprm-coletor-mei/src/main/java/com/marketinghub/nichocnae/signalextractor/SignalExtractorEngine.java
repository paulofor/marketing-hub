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
    private static final int MAX_SIGNALS = 16;
    private static final int MAX_EVIDENCE_LENGTH = 280;

    /** Analisa título, snippet e trecho curto para produzir sinais classificados da etapa cinco. */
    public List<ExtractedSignal> extract(SignalExtractorPending pending) {
        String evidence = normalizeEvidence(pending);
        String normalized = evidence.toLowerCase(Locale.ROOT);
        Map<String, ExtractedSignal> signals = new LinkedHashMap<>();
        addIfPresent(signals, normalized, evidence, List.of("mei", "autônom", "por conta própria", "dono-operador", "profissional independente"),
                "AUTONOMOUS_WORK_MODE", "Trabalho executado diretamente pelo MEI ou profissional autônomo", 86);
        addSpecificRoutineTaskSignals(signals, normalized, evidence);
        if (!containsSignalType(signals, "ROUTINE_TASK")) {
            addIfPresent(signals, normalized, evidence, List.of("agenda", "atendimento", "cliente", "serviço", "rotina", "material", "entrega"),
                    "ROUTINE_TASK", "Executar rotina diária de atendimento, agenda, materiais e entrega do serviço", 82);
        }
        addIfPresent(signals, normalized, evidence, List.of("whatsapp", "instagram", "facebook", "google", "indicação", "boca a boca", "rede social", "mensagem", "direct", "comentário"),
                "CHANNEL_USAGE", "Usar canais digitais, mensagens ou indicações para contato com clientes", 86);
        addIfPresent(signals, normalized, evidence, List.of("cliente", "conseguir clientes", "captar", "divulgar", "orçamento", "fidel", "recorr", "retorno", "reativar", "pacote", "relacionamento", "agenda cheia", "cliente novo"),
                "CUSTOMER_ACQUISITION_BEHAVIOR", "Conseguir, atender, fidelizar ou recuperar clientes na rotina autônoma", 84);
        addIfPresent(signals, normalized, evidence, List.of("falta", "cancelamento", "desmarcou", "remarcação", "atraso", "caos", "falha", "problema", "dificuldade", "retrabalho", "agenda vazia", "material acabou"),
                "OPERATIONAL_PAIN", "Dificuldade prática que gera perda de agenda, retrabalho ou falha no atendimento", 84);
        addIfPresent(signals, normalized, evidence, List.of("cansaço", "ansiedade", "estresse", "sobrecarga", "insegurança", "medo", "vergonha", "frustração"),
                "EMOTIONAL_PAIN", "Dor emocional ligada à pressão, insegurança ou sobrecarga do trabalho autônomo", 82);
        addIfPresent(signals, normalized, evidence, List.of("sonho", "objetivo", "meta", "crescer", "agenda cheia", "independência", "realização"),
                "DREAM_SIGNAL", "Sonho ou objetivo pessoal/profissional observado no público MEI/autônomo", 80);
        addIfPresent(signals, normalized, evidence, List.of("medo", "receio", "risco", "preocupação", "insegurança", "perder cliente", "não conseguir"),
                "FEAR_SIGNAL", "Medo ou insegurança que influencia decisões do profissional autônomo", 80);
        addIfPresent(signals, normalized, evidence, List.of("reconhecimento", "respeito", "profissionalismo", "confiança", "reputação", "indicação"),
                "STATUS_DESIRE", "Desejo de reconhecimento, confiança e reputação profissional", 78);
        addIfPresent(signals, normalized, evidence, List.of("correria", "pressa", "prazo", "urgente", "sem tempo", "horário", "atrasado", "tempo de atendimento"),
                "TIME_PRESSURE", "Pressão de tempo ou conflito de horários na execução do serviço", 78);
        addIfPresent(signals, normalized, evidence, List.of("retrabalho", "refazer", "conserto", "unha quebrada", "esmalte descascou", "reclamação", "cliente reclama"),
                "REWORK_OR_COMPLAINT", "Retrabalho, falha ou reclamação prática depois do atendimento", 84);
        addIfPresent(signals, normalized, evidence, List.of("material acabou", "insumo", "maleta", "alicate", "esmalte", "reposição", "produto acabou"),
                "MATERIAL_OR_SUPPLY_PAIN", "Dificuldade com materiais, maleta, insumos ou reposição", 82);
        addIfPresent(signals, normalized, evidence, List.of("higiene", "esterilização", "esteriliz", "biossegurança", "desinfecção", "alicate esterilizado"),
                "HYGIENE_OR_SAFETY_ROUTINE", "Rotina de higiene, esterilização ou biossegurança no atendimento", 82);
        addIfPresent(signals, normalized, evidence, List.of("renda", "faturamento", "ganho", "dinheiro", "instável", "mês fraco", "previsibilidade"),
                "INCOME_INSTABILITY", "Instabilidade de renda ou baixa previsibilidade financeira do autônomo", 80);
        addIfPresent(signals, normalized, evidence, List.of("qualidade", "higiene", "segurança", "confiança", "avaliação", "reclamação", "reputação"),
                "TRUST_REPUTATION_CONCERN", "Preocupação com confiança, qualidade percebida e reputação perante clientes", 78);
        addIfPresent(signals, normalized, evidence, List.of("preço", "cobrar", "barato", "caro", "orçamento", "desconto", "valor"),
                "PRICE_INSECURITY", "Insegurança para precificar, cobrar ou defender o valor do serviço", 80);
        addIfPresent(signals, normalized, evidence, List.of("falta", "não apareceu", "desmarcou", "cancelou", "remarcação", "no-show"),
                "CLIENT_NO_SHOW_OR_CANCELLATION", "Cliente que falta, cancela ou remarca prejudicando agenda e renda", 82);
        addIfPresent(signals, normalized, evidence, List.of("pergunta", "como", "dúvida", "orientação", "cliente perguntou", "cliente pede"),
                "CUSTOMER_QUESTION", "Pergunta prática que bloqueia decisão ou execução", 76);
        addIfPresent(signals, normalized, evidence, List.of("organizar", "controle", "processo", "minha rotina", "meus clientes", "linguagem", "apelido"),
                "CONTEXT_MARKER", "Sinal de organização e controle observado na rotina", 74);
        addSolutionRiskIfPresent(signals, normalized, evidence);
        if (signals.isEmpty() && StringUtils.hasText(evidence)) {
            signals.put("LANGUAGE_MARKER|fallback", new ExtractedSignal(
                    "LANGUAGE_MARKER", "Vocabulário público do nicho identificado para síntese", evidence, 60));
        }
        return signals.values().stream().limit(MAX_SIGNALS).toList();
    }

    /** Adiciona sinais de rotina preservando ações concretas encontradas na evidência pública. */
    private void addSpecificRoutineTaskSignals(Map<String, ExtractedSignal> signals, String normalized, String evidence) {
        addRoutineTaskIfPresent(
                signals,
                normalized,
                evidence,
                List.of("esteriliz", "higieniz", "desinfect"),
                List.of("alicate", "material", "instrument"),
                "Esterilizar alicates e materiais antes do atendimento",
                88);
        addRoutineTaskIfPresent(
                signals,
                normalized,
                evidence,
                List.of("lixar", "cutícula", "cuticula", "esmaltar", "unha"),
                List.of("manicure", "unha", "cutícula", "cuticula", "esmalte"),
                "Lixar, retirar cutícula e esmaltar unhas",
                88);
        addRoutineTaskIfPresent(
                signals,
                normalized,
                evidence,
                List.of("lavar", "cortar", "escovar", "finalizar"),
                List.of("cabelo", "cabeleireiro", "salão", "salao"),
                "Lavar, cortar, escovar e finalizar cabelo",
                88);
        addRoutineTaskIfPresent(
                signals,
                normalized,
                evidence,
                List.of("preparar", "misturar", "aplicar"),
                List.of("tintura", "química", "quimica", "hidratação", "hidratacao"),
                "Preparar tintura, química ou hidratação",
                86);
        addRoutineTaskIfPresent(
                signals,
                normalized,
                evidence,
                List.of("confirmar", "remarcar"),
                List.of("horário", "horario", "whatsapp", "cliente", "mensagem"),
                "Confirmar horários e remarcar clientes pelo WhatsApp",
                86);
    }

    /** Adiciona uma tarefa de rotina quando existe verbo de ação e objeto concreto na evidência. */
    private void addRoutineTaskIfPresent(
            Map<String, ExtractedSignal> signals,
            String normalized,
            String evidence,
            List<String> actionKeywords,
            List<String> objectKeywords,
            String signalText,
            Integer confidenceScore) {
        if (!containsAny(normalized, actionKeywords) || !containsAny(normalized, objectKeywords)) {
            return;
        }
        signals.putIfAbsent("ROUTINE_TASK|" + signalText, new ExtractedSignal("ROUTINE_TASK", signalText, evidence, confidenceScore));
    }

    /** Verifica se algum sinal do tipo informado já foi extraído. */
    private boolean containsSignalType(Map<String, ExtractedSignal> signals, String signalType) {
        return signals.values().stream().anyMatch(signal -> signal.signalType().equals(signalType));
    }

    /** Verifica se o texto contém qualquer indicador textual da lista informada. */
    private boolean containsAny(String normalized, List<String> keywords) {
        return keywords.stream().anyMatch(normalized::contains);
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
