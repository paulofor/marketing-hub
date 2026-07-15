package com.marketinghub.feo.fabricacaov1.redacaoentregaveis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableContent;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableContentPackage;
import com.marketinghub.feo.fabricacaov1.contract.DeliverablePlan;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableSection;
import com.marketinghub.feo.fabricacaov1.contract.DeliverableSpec;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.pipeline.StageArtifact;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Redige entregáveis finais aplicáveis antes da montagem do pacote.
 */
@Component
public class RedacaoEntregaveisProcessor implements StageProcessor<PackageAssemblyInput, PackageAssemblyInput> {

    private final ObjectMapper objectMapper;

    /**
     * Recebe serializador para publicar o pacote de conteúdo auditável.
     */
    public RedacaoEntregaveisProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Retorna a etapa canônica de redação dos entregáveis.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.REDACAO_ENTREGAVEIS;
    }

    /**
     * Transforma o plano em conteúdo de produto final com gate de qualidade.
     */
    @Override
    public StageResult<PackageAssemblyInput> process(StageContext<PackageAssemblyInput> context) {
        PackageAssemblyInput input = context.input();
        if (input.context() == null || input.plan() == null || input.plan().deliverables().isEmpty()) {
            return StageResult.blocked("Redação FEO sem contexto ou plano de entregáveis.", List.of());
        }
        DeliverableContentPackage contentPackage = buildContentPackage(input.context(), input.plan());
        if (contentPackage.qualityScore() < 80) {
            StageArtifact artifact = context.artifactStore().store(
                    "FEO_DELIVERABLE_CONTENT_REJECTED",
                    "feo-deliverable-content-rejected.json",
                    "application/json",
                    toJson(contentPackage));
            return StageResult.blocked("Conteúdo dos entregáveis abaixo do gate mínimo da FEO.", List.of(artifact));
        }
        PackageAssemblyInput output = new PackageAssemblyInput(input.context(), input.plan(), contentPackage);
        StageArtifact artifact = context.artifactStore().store(
                "FEO_DELIVERABLE_CONTENT_PACKAGE",
                "feo-deliverable-content-package.json",
                "application/json",
                toJson(contentPackage));
        return StageResult.completedWithNext(
                output,
                List.of(artifact),
                Map.of(
                        "qualityScore", contentPackage.qualityScore(),
                        "qualityGate", contentPackage.qualityGate(),
                        "deliverableCount", contentPackage.deliverables().size()),
                StageCode.MONTAGEM_PACOTE);
    }

    /**
     * Monta o pacote de conteúdos finais preservando promessa e mecanismo validados.
     */
    private DeliverableContentPackage buildContentPackage(FabricationContext context, DeliverablePlan plan) {
        List<DeliverableContent> contents = plan.deliverables().stream()
                .map(spec -> contentFor(context, spec))
                .toList();
        return new DeliverableContentPackage(
                context.requestId(),
                plan.packageTitle(),
                contents,
                score(contents),
                "PREMIUM_CONTENT_READY",
                List.of(
                        "Cada entregável tem primeira vitória clara para o comprador.",
                        "Cada entregável contém aplicação, checklist, template e critério de conclusão.",
                        "A promessa central foi preservada sem criar garantia nova."));
    }

    /**
     * Redige um entregável com uso prático imediato.
     */
    private DeliverableContent contentFor(FabricationContext context, DeliverableSpec spec) {
        return new DeliverableContent(
                spec.code(),
                spec.title(),
                "Use " + spec.title() + " para transformar " + shortText(context.centralPromise())
                        + " em uma decisão prática.",
                resultText(context, spec),
                firstWin(context, spec),
                List.of(
                        new DeliverableSection(
                                "Diagnóstico de partida",
                                "Antes de executar, o cliente registra a situação real que gera dor, esforço ou indecisão. "
                                        + "Isso evita consumo passivo e cria ponto de comparação.",
                                "Escreva a situação atual em uma frase e destaque o maior custo de não agir."),
                        new DeliverableSection(
                                "Aplicação do mecanismo",
                                "O entregável usa o mecanismo validado para reduzir complexidade e transformar a promessa em uma ação simples.",
                                "Escolha uma ação de baixo esforço que esteja diretamente ligada ao mecanismo: "
                                        + safe(context.coreMechanism())),
                        new DeliverableSection(
                                "Decisão guiada",
                                "O comprador converte leitura em escolha concreta, com prioridade, prazo e evidência mínima.",
                                "Defina o que será feito hoje, o que será ignorado e qual sinal mostrará progresso."),
                        new DeliverableSection(
                                "Revisão de valor percebido",
                                "A revisão mostra ao comprador o que mudou e aumenta a sensação de avanço real.",
                                "Compare antes e depois usando o critério de conclusão do entregável.")),
                List.of(
                        "A promessa validada aparece de forma explícita.",
                        "Existe uma primeira ação executável em até 20 minutos.",
                        "O comprador sabe o que preencher, decidir ou revisar.",
                        "O conteúdo não cria promessa maior que: " + safe(context.promisedResult()),
                        "Há critério objetivo para saber se o entregável foi concluído."),
                List.of(
                        "Situação atual",
                        "Dor ou esforço que será reduzido",
                        "Ação escolhida",
                        "Prazo de execução",
                        "Evidência de progresso",
                        "Próxima decisão"),
                List.of(
                        "Consumir o material como leitura e não preencher nada.",
                        "Tentar resolver todos os problemas ao mesmo tempo.",
                        "Trocar o mecanismo por uma promessa nova não validada.",
                        "Pular o diagnóstico inicial e perder a comparação de progresso."),
                "O entregável está concluído quando o comprador consegue explicar a situação, escolher uma ação, executar o primeiro passo e apontar uma evidência de progresso.");
    }

    /**
     * Define o resultado funcional do entregável para o comprador.
     */
    private String resultText(FabricationContext context, DeliverableSpec spec) {
        return "Ao finalizar este entregável, o comprador deve estar mais perto de "
                + safe(context.promisedResult()) + " usando o papel do ativo: " + safe(spec.role());
    }

    /**
     * Define uma primeira vitória rápida para aumentar valor percebido.
     */
    private String firstWin(FabricationContext context, DeliverableSpec spec) {
        if (spec.format() != null && spec.format().contains("CSV")) {
            return "Preencher a primeira linha com uma situação real e enxergar prioridade sem depender de memória.";
        }
        return "Tomar uma decisão prática alinhada à promessa: " + safe(context.centralPromise());
    }

    /**
     * Calcula pontuação simples de qualidade do conteúdo final.
     */
    private int score(List<DeliverableContent> contents) {
        int points = 100;
        if (contents.isEmpty()) {
            return 0;
        }
        for (DeliverableContent content : contents) {
            if (content.sections().size() < 4) {
                points -= 10;
            }
            if (content.checklist().size() < 5) {
                points -= 10;
            }
            if (content.templateFields().size() < 5) {
                points -= 10;
            }
            if (content.completionCriteria() == null || content.completionCriteria().isBlank()) {
                points -= 20;
            }
        }
        return Math.max(0, points);
    }

    /**
     * Serializa objeto como JSON auditável.
     */
    private byte[] toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar conteúdo FEO", ex);
        }
    }

    /**
     * Encurta texto para uso em título funcional.
     */
    private String shortText(String value) {
        String safe = safe(value);
        return safe.length() <= 90 ? safe : safe.substring(0, 87) + "...";
    }

    /**
     * Normaliza texto nulo para preservar a montagem.
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "a promessa validada" : value.trim();
    }
}
