package com.marketinghub.nichocnaev2.pipeline.candidategenerator;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa plugável que cria candidatos MEI/autônomo para pesquisa NichoCNAE versão 2. */
public final class CandidateGeneratorProcessor implements StageProcessor {
    /** Produz candidatos MEI/autônomo e fontes-semente seguras sem escolher vencedor nem declarar dor validada. */
    @Override
    public StageResult process(StageContext context) {
        String cnaeCode = String.valueOf(context.input().getOrDefault("cnaeCode", "CNAE_DESCONHECIDO"));
        String cnaeReference = cnaeReference(context.input(), cnaeCode);
        List<Map<String, Object>> candidates = candidateSetFor(cnaeReference);
        List<String> candidateUrls = List.of(
                "https://www.gov.br/empresas-e-negocios/pt-br/empreendedor",
                "https://www.gov.br/empresas-e-negocios/pt-br/empreendedor/quero-ser-mei",
                "https://sebrae.com.br/sites/PortalSebrae/mei",
                "https://www.gov.br/receitafederal/pt-br/assuntos/orientacao-tributaria/cadastros/cnpj/classificacao-nacional-de-atividades-economicas-2013-cnae");
        return new StageResult(
                "BOOTSTRAPPED",
                Map.ofEntries(
                        Map.entry("stage", "candidate-generator"),
                        Map.entry("candidateCount", candidates.size()),
                        Map.entry("candidates", candidates),
                        Map.entry("candidateUrls", candidateUrls),
                        Map.entry("nextStageCode", "source-safety-filter"),
                        Map.entry("audienceFocus", "INSTAGRAM_BROAD_MEI_AUTONOMO"),
                        Map.entry("distributionChannel", "INSTAGRAM"),
                        Map.entry("discoveryStrategy", "BROAD_CREATIVE_FIRST"),
                        Map.entry("hypothesisPipelineInputRole", "AUDIENCE_ROUTINE_LANGUAGE_INPUT"),
                        Map.entry("commercialBoundary", "NAO_GERAR_DOR_RESULTADO_OFERTA"),
                        Map.entry("instagramAudiencePrinciple",
                                "público amplo por desejo reconhecível; o criativo filtra a pessoa certa")),
                List.of());
    }

    /** Escolhe recortes amplos para Instagram, mantendo o CNAE como contexto e não como público estreito. */
    private List<Map<String, Object>> candidateSetFor(String cnaeReference) {
        return List.of(
                instagramCandidate("C1", "RENDA_COM_TRABALHO_PROPRIO", "Pessoa que quer ganhar dinheiro trabalhando por conta própria usando habilidade, veículo, ferramenta ou atendimento local", cnaeReference),
                instagramCandidate("C2", "CLIENTES_PELO_WHATSAPP", "Autônomo que depende de WhatsApp, Instagram, indicação e resposta rápida para conseguir clientes", cnaeReference),
                instagramCandidate("C3", "AGENDA_VAZIA_OU_OSCILANTE", "Prestador local que sofre com dias sem cliente, agenda irregular, cancelamentos e falta de recorrência", cnaeReference),
                instagramCandidate("C4", "PRECO_COBRANCA_LUCRO", "Autônomo que sente insegurança para cobrar, montar preço, negociar pacote e proteger lucro", cnaeReference),
                instagramCandidate("C5", "PROFISSIONALIZAR_ATENDIMENTO", "Pessoa que executa o serviço e quer parecer mais profissional no atendimento, orçamento e pós-venda", cnaeReference),
                instagramCandidate("C6", "SAIR_DA_DEPENDENCIA_DE_PLATAFORMA", "Trabalhador que quer depender menos de aplicativo, plataforma, intermediário ou indicação ocasional", cnaeReference),
                instagramCandidate("C7", "ORGANIZAR_ROTINA_AUTONOMA", "MEI/autônomo sobrecarregado que precisa organizar rotina, mensagens, agenda, cobrança e retorno", cnaeReference),
                instagramCandidate("C8", "TRANSFORMAR_RECURSO_EM_RENDA", "Pessoa que tem carro, ferramenta, conhecimento ou tempo disponível e quer transformar isso em renda local", cnaeReference),
                instagramCandidate("C9", "MEDO_DE_NAO_TER_CLIENTE", "Autônomo com medo de não ter cliente suficiente no mês e precisar achar demanda de forma previsível", cnaeReference),
                instagramCandidate("C10", "COMECOU_AGORA_SOZINHO", "Quem começou ou quer começar sozinho como MEI/autônomo e precisa de primeiros clientes e rotina simples", cnaeReference));
    }

    /** Usa a descrição do CNAE quando disponível para evitar contexto operacional genérico por código numérico. */
    private String cnaeReference(Map<String, Object> input, String cnaeCode) {
        Object description = input.get("cnaeDescription");
        if (description instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return "CNAE " + cnaeCode;
    }

    /** Monta um candidato amplo para Instagram sem transformar o CNAE em segmentação estreita. */
    private Map<String, Object> instagramCandidate(
            String candidateId, String job, String broadAudienceContext, String cnaeReference) {
        return Map.ofEntries(
                Map.entry("candidateId", candidateId),
                Map.entry("operator", "BROAD_MEI_AUTONOMO_INSTAGRAM"),
                Map.entry("job", job),
                Map.entry("buyerTypes", List.of("B2C")),
                Map.entry("audienceFocus", "INSTAGRAM_BROAD_MEI_AUTONOMO"),
                Map.entry("workerMode", "CRIATIVO_FILTRA_PUBLICO_AMPLO"),
                Map.entry("distributionChannel", "INSTAGRAM"),
                Map.entry("targetingMode", "BROAD_AUDIENCE_CREATIVE_FILTER"),
                Map.entry("hypothesisPipelineInputRole", "AUDIENCE_ROUTINE_LANGUAGE_INPUT"),
                Map.entry("commercialBoundary", "NAO_GERAR_DOR_RESULTADO_OFERTA"),
                Map.entry("cnaeReference", cnaeReference),
                Map.entry("evidenceFocus", List.of(
                        "desejo reconhecível no feed",
                        "sinal amplo de aquisição de clientes",
                        "rotina autônoma",
                        "cobrança e preço",
                        "linguagem simples de Instagram")),
                Map.entry("operationalContext", broadAudienceContext),
                Map.entry("priorConfidence", "LOW"));
    }
}
