package com.marketinghub.pde.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Define a personalização verificável por preferência declarada, sem inferir corpo ou personalidade. */
public final class MusaPersonalStyleContract {
    public static final String VERSION = "musa-pde-entry-v15-estilo-pessoal";
    public static final String MODEL = "MUSA_PERSONAL_STYLE_RULES_V1";
    public static final List<String> PREFERENCES = List.of(
            "Conforto e praticidade", "Cor e criatividade", "Discrição", "Delicadeza",
            "Elegância", "Ainda estou descobrindo", "Quero manter como está");
    private static final Set<String> REQUIRED = Set.of("occasion", "existingSelection", "stylePreference");
    private static final Set<String> ALLOWED = Set.of("occasion", "existingSelection", "stylePreference", "reflection");

    /** Mantém o contrato independente de provedor, sessão e persistência. */
    private MusaPersonalStyleContract() {}

    /** Rejeita omissões, preferências desconhecidas e dados fora do contrato antes de persistir. */
    public static void validate(Map<String, String> answers) {
        if (answers == null || !answers.keySet().containsAll(REQUIRED)
                || !ALLOWED.containsAll(answers.keySet())) {
            throw invalid("Informe ocasião, roupas disponíveis e sua preferência de estilo.");
        }
        for (var entry : answers.entrySet()) {
            int limit = "occasion".equals(entry.getKey()) ? 160 : 300;
            if (entry.getValue() == null || entry.getValue().length() > limit
                    || (REQUIRED.contains(entry.getKey()) && entry.getValue().isBlank())) {
                throw invalid("Confira o campo " + entry.getKey() + " e seu limite de " + limit + " caracteres.");
            }
        }
        if (!PREFERENCES.contains(answers.get("stylePreference"))) {
            throw invalid("Escolha uma das preferências disponíveis; você pode mudar depois.");
        }
    }

    /** Aplica a preferência explícita e o dia da jornada, mantendo rastreável o contexto usado. */
    public static Guidance guide(String missionId, Map<String, String> answers, Map<String, String> previous) {
        validate(answers);
        String preference = answers.get("stylePreference");
        String clothes = answers.get("existingSelection");
        String occasion = answers.get("occasion");
        String direction = switch (preference) {
            case "Conforto e praticidade" -> "Com " + clothes + ", sente, caminhe e alcance algo. Mantenha a forma de usar que acompanha seus movimentos, sem precisar ajeitar a roupa toda hora.";
            case "Cor e criatividade" -> "Em " + clothes + ", escolha a cor, estampa ou textura de que mais gosta. Experimente deixá-la visível e compare com a forma habitual, usando somente o que já está disponível.";
            case "Discrição" -> "Em " + clothes + ", observe qual detalhe chama mais a sua atenção. Compare deixá-lo visível ou menos destacado; escolha a leitura mais tranquila para você, sem eliminar o que gosta.";
            case "Delicadeza" -> "Com " + clothes + ", procure um detalhe que você considera delicado: textura, cor, acabamento ou forma de usar. Deixe esse detalhe aparecer, sem precisar de salto, vestido ou uma nova peça.";
            case "Elegância" -> "Com " + clothes + ", confira o acabamento que importa para você: alinhamento, dobra ou caimento. Ajuste um ponto e compare, sem transformar elegância em obrigação de formalidade.";
            case "Ainda estou descobrindo" -> "Vista " + clothes + " do seu jeito. Compare duas formas de usar um detalhe que já existe e anote em qual delas você se reconhece mais; não precisa escolher um rótulo de estilo.";
            default -> "Mantenha " + clothes + " como está. Não há correção obrigatória: registre o que já funciona para você e siga sem alterar o conjunto.";
        };
        List<String> actions = new ArrayList<>();
        actions.add(direction);
        actions.add(switch (missionId) {
            case "estilo-dia-2" -> "Relembre o primeiro teste abaixo. Repita o detalhe de que gostou e registre o que funcionou na prática; se não gostou, descarte sem compromisso.";
            case "estilo-dia-3" -> "Com as peças que você informou, mude apenas a forma de usar um detalhe. Compare com o registro anterior antes de decidir o que repetir.";
            case "estilo-dia-4" -> "Pense no que você fará em " + occasion + ". Confira movimento, clima e conforto dessa combinação; preserve sua preferência ao adaptar o que for necessário.";
            case "estilo-dia-5" -> "Observe as cores e texturas das peças informadas. Escolha uma relação que gostaria de repetir e registre-a; não é necessário adicionar cor ou comprar algo.";
            case "estilo-dia-6" -> "Escreva sua fórmula: combinação que gostei + detalhe que quero repetir + ocasião em que funciona. Use os registros anteriores como referência, não como regra fixa.";
            case "estilo-dia-7" -> "Revise seus registros abaixo e escolha uma fórmula para repetir na próxima semana. Baixe o resumo para consultar depois e ajuste-o quando seu gosto ou rotina mudar.";
            default -> "Teste em " + occasion + ". Sua medida é simples: isso combina com o que eu gosto e com o que preciso fazer hoje?";
        });
        List<String> signals = new ArrayList<>(List.of("Sua preferência: " + preference,
                "Sua ocasião: " + occasion, "Suas peças: " + clothes));
        previous.forEach((key, value) -> {
            if (key.endsWith(".reflection") && !value.isBlank()) {
                signals.add("Seu registro do " + key.substring(0, key.indexOf('.')).replace("estilo-dia-", "dia ") + ": " + value);
            }
        });
        if (!previous.isEmpty() && signals.size() == 3) {
            previous.entrySet().stream().filter(entry -> entry.getKey().endsWith(".stylePreference"))
                    .reduce((first, last) -> last)
                    .ifPresent(entry -> signals.add("Preferência registrada anteriormente: " + entry.getValue()));
        }
        return new Guidance("Quero manter como está".equals(preference)
                ? "Seu jeito de vestir está preservado" : "Um jeito de usar que combina com você",
                "A sugestão considera seu pedido de " + preference.toLowerCase(java.util.Locale.ROOT)
                        + " para " + occasion + ". Você decide o que vale manter; não existe um único jeito certo de se vestir.",
                List.copyOf(signals), List.copyOf(actions));
    }

    /** Padroniza a falha funcional sem aceitar uma preferência inventada. */
    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /** Transporta a orientação aplicada e os dados declarados que a sustentam. */
    public record Guidance(String headline, String summary, List<String> signals, List<String> actions) {}
}
