package com.marketinghub.nichocnaev2.pipeline.candidategenerator;

import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa plugável que cria candidatos neutros para pesquisa NichoCNAE versão 2. */
public final class CandidateGeneratorProcessor implements StageProcessor {
    /** Produz candidatos neutros e fontes-semente seguras sem escolher vencedor nem declarar dor validada. */
    @Override
    public StageResult process(StageContext context) {
        String cnaeCode = String.valueOf(context.input().getOrDefault("cnaeCode", "CNAE_DESCONHECIDO"));
        String cnaeReference = cnaeReference(context.input(), cnaeCode);
        List<Map<String, Object>> candidates = candidateSetFor(cnaeCode, cnaeReference);
        List<String> candidateUrls = List.of(
                "https://sebrae.com.br/sites/PortalSebrae/ideias/como-montar-uma-loja-de-roupas",
                "https://www.gov.br/empresas-e-negocios/pt-br/empreendedor",
                "https://www.gov.br/receitafederal/pt-br/assuntos/orientacao-tributaria/cadastros/cnpj/classificacao-nacional-de-atividades-economicas-2013-cnae",
                "https://sebrae.com.br/sites/PortalSebrae/ufs/sp/artigos/gestao-de-estoque-no-varejo");
        return new StageResult(
                "BOOTSTRAPPED",
                Map.of(
                        "stage", "candidate-generator",
                        "candidateCount", candidates.size(),
                        "candidates", candidates,
                        "candidateUrls", candidateUrls,
                        "nextStageCode", "source-safety-filter"),
                List.of());
    }

    /** Escolhe recortes neutros específicos quando há conhecimento seguro do CNAE; caso contrário usa recortes genéricos. */
    private List<Map<String, Object>> candidateSetFor(String cnaeCode, String cnaeReference) {
        if ("4781400".equals(cnaeCode)) {
            return List.of(
                    neutralCandidate("C1", "RETAIL_OPERATOR", "VESTUARIO_ATENDIMENTO_LOJA", "Atendimento e venda assistida em loja de vestuário"),
                    neutralCandidate("C2", "RETAIL_OPERATOR", "VESTUARIO_ESTOQUE_GRADE", "Organização de estoque, grade e reposição de peças"),
                    neutralCandidate("C3", "RETAIL_OPERATOR", "VESTUARIO_TROCAS_AJUSTES", "Trocas, ajustes e atendimento pós-venda de vestuário"),
                    neutralCandidate("C4", "RETAIL_OPERATOR", "VESTUARIO_VENDA_DIGITAL_LOCAL", "Venda digital local de artigos de vestuário"));
        }
        return List.of(
                neutralCandidate("C1", "CNAE_OPERATOR", "ATENDIMENTO_OPERACIONAL", "Atendimento operacional de " + cnaeReference),
                neutralCandidate("C2", "CNAE_OPERATOR", "GESTAO_DE_ROTINA", "Gestão de rotina e execução diária de " + cnaeReference),
                neutralCandidate("C3", "CNAE_OPERATOR", "AQUISICAO_DE_CLIENTES", "Aquisição e atendimento de clientes de " + cnaeReference),
                neutralCandidate("C4", "CNAE_OPERATOR", "OPERACAO_DIGITAL_LOCAL", "Operação digital local de " + cnaeReference));
    }

    /** Usa a descrição do CNAE quando disponível para evitar contexto operacional genérico por código numérico. */
    private String cnaeReference(Map<String, Object> input, String cnaeCode) {
        Object description = input.get("cnaeDescription");
        if (description instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return "CNAE " + cnaeCode;
    }

    /** Monta um candidato neutro sem dor, canal ou promessa ainda não comprovados. */
    private Map<String, Object> neutralCandidate(String candidateId, String operator, String job, String operationalContext) {
        return Map.of(
                "candidateId", candidateId,
                "operator", operator,
                "job", job,
                "buyerTypes", List.of("B2C"),
                "operationalContext", operationalContext,
                "painHypotheses", List.of(),
                "priorConfidence", "LOW");
    }
}
