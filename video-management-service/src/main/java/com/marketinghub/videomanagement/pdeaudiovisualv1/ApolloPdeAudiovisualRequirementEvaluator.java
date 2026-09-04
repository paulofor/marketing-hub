package com.marketinghub.videomanagement.pdeaudiovisualv1;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** Responsabilidade: avaliar somente o booleano canônico que governa audiovisual no contrato PDE. */
@Component
public class ApolloPdeAudiovisualRequirementEvaluator {

    /** Decide sem inferir necessidade por texto livre, tipo de componente ou saída de outra etapa. */
    public ApolloPdeAudiovisualDecision evaluate(ApolloPdeAudiovisualTask task) {
        JsonNode requirement = requirement(task);
        if (requirement != null && requirement.isBoolean()) {
            return requirement.booleanValue() ? required() : notRequired();
        }
        return missingContract();
    }

    /** Localiza o campo versionado sem fabricar objeto ausente. */
    private JsonNode requirement(ApolloPdeAudiovisualTask task) {
        if (task == null || task.taskTarget() == null || task.taskTarget().pdeContext() == null) {
            return null;
        }
        return task.taskTarget().pdeContext().path("harness").path("audiovisualRequired");
    }

    /** Conclui ausência explícita sem criar artefato ou consumir provider. */
    private ApolloPdeAudiovisualDecision notRequired() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.NOT_REQUIRED,
                "O contrato PDE versionado declara audiovisualRequired=false; nenhum audiovisual deve ser produzido.",
                "Liberar a próxima atividade da construção pelo sequenciamento exclusivo do backend.",
                null);
    }

    /** Bloqueia produção obrigatória até o Estúdio comprovar autorização e orçamento. */
    private ApolloPdeAudiovisualDecision required() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION,
                "O contrato PDE exige audiovisual, mas a atividade BPM não constitui autorização de gasto ou geração.",
                "Crie e aprove no Estúdio um ciclo audiovisual com preflight, orçamento de Plutus e autorização humana antes de reiniciar a atividade.",
                "AUTHORIZATION_REQUIRED");
    }

    /** Bloqueia contrato ambíguo antes de qualquer interpretação ou chamada externa. */
    private ApolloPdeAudiovisualDecision missingContract() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.MISSING_CONTRACT,
                "O contrato PDE não possui o booleano harness.audiovisualRequired.",
                "Corrija e versione o contrato do produto com audiovisualRequired=true ou false antes de reiniciar a atividade.",
                "MISSING_EVIDENCE");
    }
}
