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
import com.marketinghub.feo.fabricacaov1.contract.VisualAssetSpec;
import com.marketinghub.feo.fabricacaov1.pipeline.StageArtifact;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageContext;
import com.marketinghub.feo.fabricacaov1.pipeline.StageProcessor;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Redige entregáveis finais aplicáveis antes da montagem do pacote.
 */
@Component
public class RedacaoEntregaveisProcessor implements StageProcessor<PackageAssemblyInput, PackageAssemblyInput> {

    private static final Set<String> REQUIRED_COMPONENTS = Set.of(
            "COMECE_AQUI",
            "PLANO_EXECUCAO_RAPIDA",
            "CHECKLIST_APLICACAO",
            "TEMPLATES_PRONTOS",
            "EXEMPLO_PREENCHIDO",
            "PROVA_TANGIVEL",
            "RITUAL_ACOMPANHAMENTO",
            "BONUS_ANTI_OBJECAO",
            "GUIA_PRIMEIROS_RESULTADOS");

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
        List<String> missingComponents = missingComponents(contentPackage);
        if (!missingComponents.isEmpty()) {
            StageArtifact artifact = context.artifactStore().store(
                    "FEO_DELIVERABLE_CONTENT_REJECTED",
                    "feo-deliverable-content-rejected.json",
                    "application/json",
                    toJson(contentPackage));
            return StageResult.blocked(
                    "Kit de Transformação Aplicável incompleto: " + String.join(", ", missingComponents),
                    List.of(artifact));
        }
        if (contentPackage.qualityScore() < 80) {
            StageArtifact artifact = context.artifactStore().store(
                    "FEO_DELIVERABLE_CONTENT_REJECTED",
                    "feo-deliverable-content-rejected.json",
                    "application/json",
                    toJson(contentPackage));
            return StageResult.blocked("Conteúdo dos entregáveis abaixo do gate mínimo da FEO.", List.of(artifact));
        }
        PackageAssemblyInput output = new PackageAssemblyInput(input.context(), input.plan(), contentPackage, List.of());
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
                StageCode.GERACAO_ATIVOS_VISUAIS);
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
                visualAssetsFor(context),
                score(contents),
                "PREMIUM_CONTENT_READY",
                List.of(
                        "O pacote contém método, plano, materiais prontos, prova, ritual e bônus anti-objeção.",
                        "O pacote exige capa, infográficos e figuras internas para aumentar valor percebido.",
                        "Cada entregável tem primeira vitória clara para o comprador.",
                        "Cada entregável contém aplicação, checklist, template e critério de conclusão.",
                        "A promessa central foi preservada sem criar garantia nova."));
    }

    /**
     * Planeja imagens editoriais obrigatórias para o pacote parecer produto premium, não relatório técnico.
     */
    private List<VisualAssetSpec> visualAssetsFor(FabricationContext context) {
        return List.of(
                new VisualAssetSpec(
                        "VIS-01",
                        "Capa editorial do e-book principal",
                        "EBOOK_COVER",
                        "cover",
                        "Crie uma capa vertical premium para um e-book digital chamado '" + safe(context.offerName())
                                + "'. Público: " + safe(context.niche())
                                + ". Promessa: " + safe(context.centralPromise())
                                + ". Direção visual: editorial feminino sofisticado, elegante, acessível, claro, sem luxo ostensivo, com composição limpa, título legível, sensação de método prático e transformação em 7 dias. Não use termos técnicos, métricas, logos de plataformas ou aparência de relatório.",
                        "1024x1536",
                        "png"),
                new VisualAssetSpec(
                        "VIS-02",
                        "Infográfico do plano de 7 dias",
                        "INFOGRAPHIC",
                        "inside-ebook",
                        "Crie um infográfico vertical em português mostrando uma jornada de 7 dias para aplicar a promessa: "
                                + safe(context.centralPromise())
                                + ". Use blocos claros, ícones simples, setas suaves, espaço para leitura em PDF e linguagem de cliente final. Não inclua CTR, CPL, lead, experimento, FEO, score, JSON ou qualquer termo técnico.",
                        "1024x1536",
                        "png"),
                new VisualAssetSpec(
                        "VIS-03",
                        "Mapa visual de presença elegante",
                        "CONCEPT_MAP",
                        "inside-ebook",
                        "Crie um mapa visual rico em português conectando cabelo, pele, roupa, perfume, acessórios, ocasião e orçamento para explicar o mecanismo: "
                                + safe(context.coreMechanism())
                                + ". Estética editorial, útil, feminina e aplicável. A imagem deve ajudar a compradora a entender o método de relance, sem parecer slide corporativo.",
                        "1536x1024",
                        "png"),
                new VisualAssetSpec(
                        "VIS-04",
                        "Antes e depois conceitual da aplicação",
                        "BEFORE_AFTER",
                        "inside-ebook",
                        "Crie uma figura antes/depois conceitual para um produto de presença elegante acessível. Antes: escolhas dispersas, excesso de tentativa, visual sem coerência. Depois: detalhes coordenados, presença intencional, rotina simples. Não mostre transformação corporal, não prometa resultado automático e não use texto técnico.",
                        "1536x1024",
                        "png"));
    }

    /**
     * Redige um entregável com uso prático imediato.
     */
    private DeliverableContent contentFor(FabricationContext context, DeliverableSpec spec) {
        return new DeliverableContent(
                spec.code(),
                spec.title(),
                spec.componentType(),
                "Use " + spec.title() + " para transformar " + shortText(context.centralPromise())
                        + " em uma decisão prática.",
                resultText(context, spec),
                firstWin(context, spec),
                readyToUseAsset(context, spec),
                tangibleProof(context, spec),
                ritualStep(context, spec),
                antiObjectionBonus(context, spec),
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
        return switch (safe(spec.componentType())) {
            case "PLANO_EXECUCAO_RAPIDA" -> "Ao finalizar, o comprador tem um plano de 7 dias para chegar mais perto de "
                    + safe(context.promisedResult()) + ".";
            case "TEMPLATES_PRONTOS" -> "Ao finalizar, o comprador tem materiais preenchidos que reduzem o esforço de começar.";
            case "PROVA_TANGIVEL" -> "Ao finalizar, o comprador enxerga o antes, o depois e o miniresultado esperado.";
            case "RITUAL_ACOMPANHAMENTO" -> "Ao finalizar, o comprador sabe quando agir, revisar e continuar sem suporte manual.";
            case "BONUS_ANTI_OBJECAO" -> "Ao finalizar, o comprador tem resposta prática para a objeção que mais trava a aplicação.";
            default -> "Ao finalizar este entregável, o comprador deve estar mais perto de "
                    + safe(context.promisedResult()) + " usando o papel do ativo: " + safe(spec.role());
        };
    }

    /**
     * Define uma primeira vitória rápida para aumentar valor percebido.
     */
    private String firstWin(FabricationContext context, DeliverableSpec spec) {
        if ("COMECE_AQUI".equals(spec.componentType())) {
            return "Entender a ordem de consumo e iniciar pelo primeiro ativo sem dúvida.";
        }
        if ("EXEMPLO_PREENCHIDO".equals(spec.componentType())) {
            return "Comparar o próprio preenchimento com uma amostra pronta e corrigir a rota.";
        }
        if (spec.format() != null && spec.format().contains("CSV")) {
            return "Preencher a primeira linha com uma situação real e enxergar prioridade sem depender de memória.";
        }
        return "Tomar uma decisão prática alinhada à promessa: " + safe(context.centralPromise());
    }

    /**
     * Define o material pronto que o comprador recebe em cada componente.
     */
    private String readyToUseAsset(FabricationContext context, DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "PLANO_EXECUCAO_RAPIDA" -> "Tabela de 7 dias com ação, tempo estimado, evidência e ajuste.";
            case "CHECKLIST_APLICACAO" -> "Checklist marcável para executar sem esquecer pontos críticos.";
            case "TEMPLATES_PRONTOS" -> "Modelos copiáveis com campos de situação, decisão, prazo e evidência.";
            case "EXEMPLO_PREENCHIDO" -> "Amostra preenchida com cenário realista do nicho: " + safe(context.niche()) + ".";
            case "PROVA_TANGIVEL" -> "Quadro antes/depois e miniresultado demonstrável da promessa.";
            case "RITUAL_ACOMPANHAMENTO" -> "Calendário de checkpoints, lembretes e revisão.";
            case "BONUS_ANTI_OBJECAO" -> "FAQ operacional e atalho para continuar mesmo com pouca clareza.";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Roteiro para reconhecer progresso em 20 minutos, 24 horas e 7 dias.";
            default -> "Material complementar pronto para revisão e aplicação.";
        };
    }

    /**
     * Define a prova tangivel que aumenta valor percebido do produto.
     */
    private String tangibleProof(FabricationContext context, DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "PROVA_TANGIVEL" -> "Antes: esforço disperso. Depois: ação priorizada pelo mecanismo "
                    + safe(context.coreMechanism()) + ".";
            case "EXEMPLO_PREENCHIDO" -> "Exemplo preenchido mostra o nível de detalhe esperado e evita página em branco.";
            case "PLANO_EXECUCAO_RAPIDA" -> "A prova aparece quando o comprador conclui o Dia 1 com uma decisão registrada.";
            default -> "A prova mínima é o comprador conseguir apontar uma evidência de progresso sem depender de opinião externa.";
        };
    }

    /**
     * Define ritual de uso para criar sensação de acompanhamento.
     */
    private String ritualStep(FabricationContext context, DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "RITUAL_ACOMPANHAMENTO" -> "Abrir o kit no mesmo horário por 7 dias, marcar um checkpoint e registrar a próxima ação.";
            case "PLANO_EXECUCAO_RAPIDA" -> "Executar uma ação curta por dia e revisar a evidência antes de avançar.";
            case "CHECKLIST_APLICACAO" -> "Marcar o checklist antes de encerrar cada sessão de aplicação.";
            default -> "Usar após o plano principal, sempre registrando ação, prazo e evidência.";
        };
    }

    /**
     * Define bonus anti-objecao sem criar promessa comercial nova.
     */
    private String antiObjectionBonus(FabricationContext context, DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "BONUS_ANTI_OBJECAO" -> "Se eu não souber por onde começar, uso o atalho de menor esforço e executo só o primeiro campo.";
            case "TEMPLATES_PRONTOS" -> "Se eu não tiver ideias, copio o modelo base e substituo apenas os campos essenciais.";
            case "PROVA_TANGIVEL" -> "Se eu duvidar do resultado, comparo o antes/depois operacional sem assumir garantia automática.";
            default -> "Se houver dúvida, voltar ao manifesto, escolher um único entregável e concluir o critério mínimo.";
        };
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
            if (isBlank(content.readyToUseAsset())
                    || isBlank(content.tangibleProof())
                    || isBlank(content.ritualStep())
                    || isBlank(content.antiObjectionBonus())) {
                points -= 15;
            }
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
     * Lista componentes obrigatorios ausentes do Kit de Transformacao Aplicavel.
     */
    private List<String> missingComponents(DeliverableContentPackage contentPackage) {
        Set<String> present = contentPackage.deliverables().stream()
                .map(DeliverableContent::componentType)
                .collect(java.util.stream.Collectors.toSet());
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_COMPONENTS) {
            if (!present.contains(required)) {
                missing.add(required);
            }
        }
        return missing;
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

    /**
     * Indica se um texto obrigatorio esta vazio.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
