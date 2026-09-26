package com.marketinghub.videomanagement.pdeaudiovisualv1;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** Responsabilidade: avaliar o booleano audiovisual canônico do fluxo que criou a tarefa. */
@Component
public class ApolloPdeAudiovisualRequirementEvaluator {

    /** Decide sem inferir necessidade por texto livre, tipo de componente ou briefing bruto. */
    public ApolloPdeAudiovisualDecision evaluate(ApolloPdeAudiovisualTask task) {
        JsonNode requirement = requirement(task);
        if (requirement != null && requirement.isBoolean()) {
            return requirement.booleanValue()
                    ? materializationReady(task) ? materialized() : required()
                    : notRequired();
        }
        return missingContract();
    }

    /** Confere entrega, autorização, custo e vínculo antes de concluir a atividade bloqueada. */
    private boolean materializationReady(ApolloPdeAudiovisualTask task) {
        if (task == null
                || task.taskTarget() == null
                || task.taskTarget().pdeContext() == null
                || !"creative-production-approval".equals(task.processCode())) return false;
        JsonNode materialization =
                task.taskTarget().pdeContext().path("audiovisualMaterialization");
        JsonNode artifacts = materialization.path("artifactIds");
        if (!"APOLLO_COMMUNICATION_AUDIOVISUAL_MATERIALIZATION_V1"
                        .equals(materialization.path("contractVersion").asText())
                || !task.sourceReference().equals(materialization.path("sourceReference").asText())
                || materialization.path("videoProductionCycleId").asLong() <= 0
                || materialization.path("videoProjectId").asLong() <= 0
                || materialization.path("salesVideoJobId").asLong() <= 0
                || !"VIDEO_READY_FOR_REVIEW".equals(materialization.path("status").asText())
                || !"APPROVED".equals(materialization.path("financialDecision").asText())
                || materialization.path("authorizedBy").asText().isBlank()
                || !materialization.path("spendAuthorized").asBoolean(false)
                || materialization.path("publicationAuthorized").asBoolean(true)
                || !artifacts.isArray()
                || artifacts.isEmpty()
                || materialization.path("budgetLimitUsd").decimalValue().signum() <= 0
                || materialization.path("actualCostUsd").decimalValue().signum() < 0
                || materialization
                                .path("actualCostUsd")
                                .decimalValue()
                                .compareTo(materialization.path("budgetLimitUsd").decimalValue())
                        > 0) return false;
        for (JsonNode artifact : artifacts) {
            if (!artifact.canConvertToLong() || artifact.asLong() <= 0) return false;
        }
        return true;
    }

    /** Localiza o campo versionado próprio da construção do produto ou da produção criativa. */
    private JsonNode requirement(ApolloPdeAudiovisualTask task) {
        if (task == null || task.taskTarget() == null || task.taskTarget().pdeContext() == null) {
            return null;
        }
        if ("creative-production-approval".equals(task.processCode())) {
            return task.taskTarget()
                    .pdeContext()
                    .path("communicationMaterialization")
                    .path("audiovisualRequired");
        }
        return task.taskTarget().pdeContext().path("harness").path("audiovisualRequired");
    }

    /** Conclui ausência explícita sem criar artefato ou consumir provider. */
    private ApolloPdeAudiovisualDecision notRequired() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.NOT_REQUIRED,
                "O contrato versionado da atividade declara audiovisualRequired=false; nenhum audiovisual deve ser produzido.",
                "Liberar a próxima atividade da construção pelo sequenciamento exclusivo do backend.",
                null);
    }

    /** Bloqueia produção obrigatória até o Estúdio comprovar autorização e orçamento. */
    private ApolloPdeAudiovisualDecision required() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.REQUIRES_AUTHORIZATION,
                "O contrato versionado da atividade exige audiovisual, mas a atividade BPM não constitui autorização de gasto ou geração.",
                "Crie e aprove no Estúdio um ciclo audiovisual com preflight, orçamento de Plutus e autorização humana antes de reiniciar a atividade.",
                "AUTHORIZATION_REQUIRED");
    }

    /** Conclui a produção quando o Estúdio devolve um ativo governado da mesma referência. */
    private ApolloPdeAudiovisualDecision materialized() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.MATERIALIZED,
                "O Estúdio concluiu a peça audiovisual da mesma versão, com autorização humana, parecer financeiro e custo dentro do teto.",
                "Liberar as revisões independentes de Psique e Têmis pelo sequenciamento exclusivo do backend.",
                null);
    }

    /** Bloqueia contrato ambíguo antes de qualquer interpretação ou chamada externa. */
    private ApolloPdeAudiovisualDecision missingContract() {
        return new ApolloPdeAudiovisualDecision(
                ApolloPdeAudiovisualDecision.Outcome.MISSING_CONTRACT,
                "O contrato canônico da atividade não possui um booleano audiovisualRequired válido.",
                "Corrija a decisão versionada que originou a atividade antes de reiniciá-la.",
                "MISSING_EVIDENCE");
    }
}
