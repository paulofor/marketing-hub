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
            "DIAGNOSTICO_GUIADO",
            "MISSOES_7_DIAS",
            "PAINEL_PROGRESSO",
            "PLANO_EXECUCAO_RAPIDA",
            "CHECKLIST_APLICACAO",
            "TEMPLATES_PRONTOS",
            "EXEMPLO_PREENCHIDO",
            "PROVA_TANGIVEL",
            "BIBLIOTECA_APOIO",
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
                        "O PDE contém experiência guiada, método, plano, materiais prontos, prova, ritual e apoio para destravar.",
                        "A experiência guiada é o produto principal; e-book, checklists, templates e exemplos são biblioteca de apoio.",
                        "Os aprendizados foram traduzidos em exercício, decisão e sinal visual de progresso.",
                        "O pacote deve parecer produto final desejável, íntimo, claro e aplicável.",
                        "Cada material tem primeira vitória clara para a mulher que começou a jornada.",
                        "Cada material contém aplicação, checklist, template e sinal de fechamento.",
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
                                + ". Direção visual: editorial feminino sofisticado, elegante, acessível, claro, sem luxo ostensivo, com composição limpa, título legível, sensação de método prático e transformação em 7 dias. Não use termos técnicos, métricas, siglas internas, logos de plataformas ou aparência de relatório.",
                        "1024x1536",
                        "png"),
                new VisualAssetSpec(
                        "VIS-02",
                        "Infográfico do plano de 7 dias",
                        "INFOGRAPHIC",
                        "inside-ebook",
                        "Crie um infográfico vertical em português mostrando uma jornada de 7 dias para aplicar a promessa: "
                                + safe(context.centralPromise())
                                + ". Use blocos claros, ícones simples, setas suaves, espaço para leitura em PDF e linguagem de cliente final. Não inclua CTR, CPL, lead, experimento, FEO, MDS, score, JSON ou qualquer termo técnico.",
                        "1024x1536",
                        "png"),
                new VisualAssetSpec(
                        "VIS-03",
                        "Mapa visual de presença elegante",
                        "CONCEPT_MAP",
                        "inside-ebook",
                        "Crie um mapa visual rico em português conectando cabelo, pele, roupa, perfume, acessórios, ocasião e orçamento para explicar o mecanismo: "
                                + safe(context.coreMechanism())
                                + ". Estética editorial, útil, feminina e aplicável. A imagem deve ajudar a compradora a entender o método de relance, sem parecer slide corporativo e sem usar siglas internas.",
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
                headlineFor(spec),
                appliedPrinciple(context, spec),
                resultText(context, spec),
                firstWin(context, spec),
                readyToUseAsset(context, spec),
                tangibleProof(context, spec),
                ritualStep(context, spec),
                antiObjectionBonus(context, spec),
                List.of(
                        new DeliverableSection(
                                "O momento do espelho",
                                "Você registra como se sente hoje quando está pronta para sair, mas ainda percebe que falta presença, acabamento ou intenção. "
                                        + "O objetivo é transformar esse incômodo em um ponto de partida simples.",
                                "Complete a frase: hoje eu me sinto arrumada, mas pouco marcante quando..."),
                        new DeliverableSection(
                                "Regra simples",
                                "A lógica do método vira uma regra prática: reduzir dúvida, criar contraste com intenção e fazer cabelo, pele, roupa, perfume e acessórios conversarem entre si. "
                                        + "Você não precisa decorar teoria; precisa saber qual detalhe ajustar hoje.",
                                "Transforme a regra em uma decisão concreta usando esta orientação: "
                                        + appliedPrinciple(context, spec)),
                        new DeliverableSection(
                                "Ajuste bonito, possível e barato",
                                "Você escolhe um ajuste visível e possível: cabelo, pele, roupa, perfume, acessório, ocasião ou compra evitada. "
                                        + "A mudança precisa caber na sua rotina, não em uma versão ideal da sua vida.",
                                "Escolha um ajuste de até 20 minutos e registre o que será mantido, removido e combinado."),
                        new DeliverableSection(
                                "Antes e depois de presença",
                                "O exemplo mostra como uma decisão pequena muda a percepção do conjunto sem exigir luxo, compra impulsiva ou transformação radical.",
                                "Preencha com uma situação real e compare antes/depois usando foto, espelho ou descrição."),
                        new DeliverableSection(
                                "Sensação que deve ficar",
                                "A revisão mostra o que mudou, qual esforço foi poupado e qual próximo ajuste mantém a evolução sem virar perfeccionismo.",
                                "Marque uma evidência de progresso e escolha o próximo microajuste de menor esforço.")),
                List.of(
                        "Escolhi uma situação real da minha semana.",
                        "A ação cabe em até 20 minutos.",
                        "Antes de comprar, olhei o que já tenho.",
                        "Registrei um sinal visível de progresso.",
                        "Se deu dúvida, voltei para uma única missão.",
                        "Terminei com uma próxima decisão simples."),
                List.of(
                        "Situação atual",
                        "Regra simples",
                        "Incômodo que será reduzido",
                        "Ação escolhida",
                        "Detalhe visual ou sensorial ajustado",
                        "O que vou reaproveitar antes de comprar",
                        "Prazo de execução",
                        "Evidência de progresso",
                        "Próxima decisão"),
                List.of(
                        "Consumir o material como leitura e não preencher nada.",
                        "Esperar clareza perfeita antes de ajustar um detalhe.",
                        "Tentar resolver todos os problemas ao mesmo tempo.",
                        "Comprar algo novo antes de olhar o que já existe.",
                        "Pular o diagnóstico inicial e perder a comparação de progresso."),
                "Este material cumpriu seu papel quando você consegue explicar a situação, escolher uma ação, executar o primeiro passo e apontar uma evidência de progresso.");
    }

    /**
     * Cria uma chamada humana para cada material do pacote.
     */
    private String headlineFor(DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "COMECE_AQUI" -> "Comece aqui quando quiser abrir o método sem se perder entre arquivos.";
            case "DIAGNOSTICO_GUIADO" -> "Use este espelho para descobrir o que hoje deixa sua presença menos marcante.";
            case "MISSOES_7_DIAS" -> "Siga uma missão por dia para melhorar presença sem gastar mais do que precisa.";
            case "PAINEL_PROGRESSO" -> "Registre o antes e depois para enxergar o que ficou mais coerente.";
            case "PLANO_EXECUCAO_RAPIDA" -> "Use este plano quando quiser saber exatamente o que fazer hoje.";
            case "CHECKLIST_APLICACAO" -> "Passe por este checklist antes de sair, gravar ou entrar em uma situação importante.";
            case "TEMPLATES_PRONTOS" -> "Preencha estes cartões para decidir roupa, beleza e compras com mais intenção.";
            case "EXEMPLO_PREENCHIDO" -> "Veja um exemplo realista antes de preencher o seu.";
            case "PROVA_TANGIVEL" -> "Compare o quase bom com uma presença mais memorável.";
            case "BIBLIOTECA_APOIO" -> "Consulte esta biblioteca quando quiser revisar sem voltar ao começo.";
            case "RITUAL_ACOMPANHAMENTO" -> "Use este ritual para manter sua assinatura em semanas corridas.";
            case "BONUS_ANTI_OBJECAO" -> "Abra este atalho quando bater a sensação de que você não tem roupa, tempo ou ideia.";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Use este guia para perceber sinais sutis de que sua presença mudou.";
            default -> "Use este material para escolher o próximo detalhe com mais intenção.";
        };
    }

    /**
     * Traduz sinais de pesquisa em um princípio comercial aplicável pela compradora.
     */
    private String appliedPrinciple(FabricationContext context, DeliverableSpec spec) {
        String base = safe(context.coreMechanism());
        String proof = safe(context.proofSummary());
        return switch (safe(spec.componentType())) {
            case "PLANO_EXECUCAO_RAPIDA" -> "quebrar a mudança em microdecisões diárias para reduzir esforço mental e aumentar consistência percebida";
            case "DIAGNOSTICO_GUIADO" -> "transformar percepção vaga em ponto de partida visual para orientar a jornada de aplicação";
            case "MISSOES_7_DIAS" -> "converter o método em microações diárias para criar sensação de acompanhamento e progresso";
            case "PAINEL_PROGRESSO" -> "usar evidências simples para você perceber avanço sem depender de perfeccionismo";
            case "CHECKLIST_APLICACAO" -> "usar pistas visuais simples para diminuir esquecimento, dúvida e compra por impulso";
            case "TEMPLATES_PRONTOS" -> "transformar conhecimento em campos preenchíveis para evitar página em branco";
            case "EXEMPLO_PREENCHIDO" -> "modelar uma aplicação realista para acelerar reconhecimento de padrão";
            case "PROVA_TANGIVEL" -> "comparar antes/depois por coerência de sinais, não por luxo ou transformação corporal";
            case "BIBLIOTECA_APOIO" -> "organizar materiais de apoio para reforçar a experiência sem virar excesso de conteúdo";
            case "BONUS_ANTI_OBJECAO" -> "substituir a objeção por uma ação mínima que preserva avanço";
            case "COMECE_AQUI" -> "começar pelo espelho, escolher uma missão e evitar abrir todos os materiais ao mesmo tempo";
            case "RITUAL_ACOMPANHAMENTO" -> "repetir uma revisão curta para manter sua assinatura mesmo quando a semana estiver corrida";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "observar sinais pequenos de coerência antes de buscar uma mudança maior";
            default -> "transformar o método em uma decisão simples e observável";
        };
    }

    /**
     * Define o resultado funcional do entregável para a compradora.
     */
    private String resultText(FabricationContext context, DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "PLANO_EXECUCAO_RAPIDA" -> "Ao finalizar, você tem um plano de 7 dias para chegar mais perto de "
                    + safe(context.promisedResult()) + ".";
            case "DIAGNOSTICO_GUIADO" -> "Ao finalizar, você sabe qual ponto ajustar primeiro na experiência.";
            case "MISSOES_7_DIAS" -> "Ao finalizar, você percorreu uma sequência guiada com ações pequenas e progressivas.";
            case "PAINEL_PROGRESSO" -> "Ao finalizar, você tem evidências marcadas de avanço percebido.";
            case "TEMPLATES_PRONTOS" -> "Ao finalizar, você tem materiais preenchidos que reduzem o esforço de começar.";
            case "PROVA_TANGIVEL" -> "Ao finalizar, você enxerga o antes, o depois e o miniresultado esperado.";
            case "BIBLIOTECA_APOIO" -> "Ao finalizar, você sabe qual arquivo usar em cada momento da experiência.";
            case "RITUAL_ACOMPANHAMENTO" -> "Ao finalizar, você sabe quando agir, revisar e continuar sem suporte manual.";
            case "BONUS_ANTI_OBJECAO" -> "Ao finalizar, você tem resposta prática para a trava que mais atrapalha a aplicação.";
            case "COMECE_AQUI" -> "Ao finalizar, você sabe por onde começar sem se perder entre arquivos, ideias e vontade de mudar tudo.";
            default -> "Ao finalizar este material, você deve estar mais perto de "
                    + safe(context.promisedResult()) + ".";
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
        return switch (safe(spec.componentType())) {
            case "DIAGNOSTICO_GUIADO" -> "Nomear o detalhe que hoje faz você se sentir arrumada, mas pouco marcante.";
            case "MISSOES_7_DIAS" -> "Concluir a primeira missão e sentir que a mudança cabe na sua rotina.";
            case "PAINEL_PROGRESSO" -> "Registrar um antes/depois simples e perceber um sinal de intenção no conjunto.";
            case "PLANO_EXECUCAO_RAPIDA" -> "Saber exatamente o que fazer hoje sem abrir o armário no impulso.";
            case "PROVA_TANGIVEL" -> "Ver a diferença entre estar apenas arrumada e parecer mais memorável.";
            case "BIBLIOTECA_APOIO" -> "Encontrar rapidamente o material certo sem transformar a jornada em estudo.";
            case "RITUAL_ACOMPANHAMENTO" -> "Criar um momento semanal de 15 minutos para manter sua assinatura.";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Reconhecer pequenos sinais de mais presença sem esperar perfeição.";
            default -> "Escolher o primeiro detalhe que deixa sua presença mais intencional hoje.";
        };
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
            case "BONUS_ANTI_OBJECAO" -> "Perguntas rápidas e atalho para continuar mesmo quando bater dúvida.";
            case "GUIA_PRIMEIROS_RESULTADOS" -> "Roteiro para reconhecer progresso em 20 minutos, 24 horas e 7 dias.";
            default -> "Página de apoio para decidir, preencher e seguir sem recomeçar do zero.";
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
            default -> "O sinal mínimo é você conseguir apontar o que ficou mais coerente, mais leve ou mais intencional.";
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
            case "COMECE_AQUI" -> "Ler esta página, escolher o Dia 0 e iniciar sem abrir todos os arquivos de uma vez.";
            default -> "Usar junto da missão do dia, registrando ação, prazo e evidência.";
        };
    }

    /**
     * Define bonus anti-objecao sem criar promessa comercial nova.
     */
    private String antiObjectionBonus(FabricationContext context, DeliverableSpec spec) {
        return switch (safe(spec.componentType())) {
            case "BONUS_ANTI_OBJECAO" -> "Se eu não souber por onde começar, uso o atalho de menor esforço e executo só o primeiro campo.";
            case "TEMPLATES_PRONTOS" -> "Se eu não tiver ideias, copio o modelo base e substituo apenas os campos essenciais.";
            case "PROVA_TANGIVEL" -> "Se eu duvidar do resultado, comparo o antes/depois sem exigir perfeição nem mudança radical.";
            default -> "Se houver dúvida, volte ao começo, escolha uma única missão e conclua o primeiro passo possível.";
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
     * Normaliza texto nulo para preservar a montagem.
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "a promessa do produto" : value.trim();
    }

    /**
     * Indica se um texto obrigatorio esta vazio.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
