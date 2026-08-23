package com.marketinghub.pde.service;

import java.util.Map;
import java.util.Set;

/** Centraliza as escolhas categoriais permitidas em toda a superfície do produto MUSA v7. */
public final class MusaV7CategoricalContract {
    private static final String NEUTRAL_CHOICE = "Manter como está por enquanto";
    private static final Map<String, Set<String>> ALLOWED_VALUES_BY_KEY = Map.ofEntries(
            Map.entry("mainObstacle", Set.of(
                    "Falta presença", "Pareço comum", "Estou improvisada", "Não comunica meu momento",
                    "Minha imagem está coerente; quero apenas organizar minhas escolhas", "Falta acabamento",
                    "Nada conversa entre si", "Sinto que exagerei")),
            Map.entry("presenceFocus", Set.of(
                    "Trabalho ou reunião", "Encontro ou saída", "Rotina comum", "Foto ou conteúdo")),
            Map.entry("desiredSignal", Set.of(
                    "Elegância discreta", "Segurança calma", "Segurança", "Cuidado percebido",
                    "Leveza feminina", "Imagem mais marcante")),
            Map.entry("startingResource", Set.of(
                    "Roupa que já tenho", "Cabelo e pele", "Cor e acabamento", "Acessório ou perfume",
                    "Postura e presença")),
            Map.entry("mainConstraint", Set.of(
                    "Pouco tempo", "Dúvida na roupa", "Vontade de comprar", "Falta de constância")),
            Map.entry("finishSignal", Set.of(
                    "Cabelo polido", "Pele iluminada", "Maquiagem leve", "Roupa com caimento limpo")),
            Map.entry("baseColor", Set.of(
                    "Vinho discreto", "Preto limpo", "Off-white", "Verde oliva", "Jeans escuro")),
            Map.entry("memorableSignal", Set.of(
                    "Perfume assinatura", "Brinco luminoso", "Batom discreto", "Bolsa estruturada",
                    "Lenço ou textura suave")),
            Map.entry("pieces", Set.of("Calça e camisa", "Vestido", "Jeans e terceira peça", "Saia e blusa")),
            Map.entry("accessories", Set.of(
                    "Brinco e perfume", "Bolsa e sapato", "Lenço e textura", "Cabelo e maquiagem leve")),
            Map.entry("realOccasion", Set.of(
                    "Trabalho ou reunião", "Encontro ou saída", "Rotina comum", "Foto ou conteúdo")),
            Map.entry("availableMinutes", Set.of("5 minutos", "10 minutos", "15 minutos", "20 minutos")),
            Map.entry("weakestFinish", Set.of("Cabelo", "Pele", "Roupa", "Postura e presença")),
            Map.entry("desiredFeeling", Set.of("Mais cuidada", "Mais segura", "Mais leve", "Mais marcante")),
            Map.entry("desiredItem", Set.of("Roupa", "Sapato", "Bolsa", "Acessório ou perfume")),
            Map.entry("buyingReason", Set.of(
                    "Resolver uma ocasião", "Sentir mais presença", "Substituir item gasto", "Impulso ou novidade")),
            Map.entry("fitWithSignature", Set.of(
                    "Combina e será repetido", "Combina pouco", "Já tenho algo equivalente", "Ainda não sei")),
            Map.entry("occasion", Set.of("Trabalho ou reunião", "Encontro ou saída", "Evento", "Foto ou conteúdo")),
            Map.entry("plannedLook", Set.of(
                    "Base neutra e detalhe", "Peça-sinal e acabamento", "Cor-base e acessório", "Ainda vou escolher")),
            Map.entry("presenceRisk", Set.of(
                    "Pressa", "Excesso de informação", "Falta de acabamento", "Desconforto")),
            Map.entry("bestSignal", Set.of("Acabamento", "Cor-base", "Peça-sinal", "Postura e presença")),
            Map.entry("hardestPoint", Set.of(
                    "Pouco tempo", "Combinar cores", "Evitar compras", "Manter constância")),
            Map.entry("weeklyRitual", Set.of(
                    "Separar 3 combinações", "Revisar acabamentos", "Planejar uma ocasião", "Repetir a peça-sinal")));

    /** Impede instanciação porque o contrato é imutável e compartilhado. */
    private MusaV7CategoricalContract() {}

    /** Rejeita chave, texto livre ou valor pertencente a outra pergunta da experiência. */
    public static void validate(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("A versão MUSA v7 exige escolhas categoriais documentadas");
        }
        answers.forEach((key, value) -> {
            Set<String> allowedValues = ALLOWED_VALUES_BY_KEY.get(key);
            if (allowedValues == null) {
                throw new IllegalArgumentException("Chave categorial MUSA v7 inválida: " + key);
            }
            if (value == null || value.isBlank() || (!NEUTRAL_CHOICE.equals(value) && !allowedValues.contains(value))) {
                throw new IllegalArgumentException("Escolha categorial MUSA v7 inválida para: " + key);
            }
        });
    }
}
