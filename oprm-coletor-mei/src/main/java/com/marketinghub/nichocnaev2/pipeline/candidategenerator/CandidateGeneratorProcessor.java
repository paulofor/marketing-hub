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
                Map.of(
                        "stage", "candidate-generator",
                        "candidateCount", candidates.size(),
                        "candidates", candidates,
                        "candidateUrls", candidateUrls,
                        "nextStageCode", "source-safety-filter",
                        "audienceFocus", "MEI_AUTONOMO_DONO_OPERADOR"),
                List.of());
    }

    /** Escolhe recortes MEI/autônomo aplicáveis a qualquer CNAE sem especializar por atividade. */
    private List<Map<String, Object>> candidateSetFor(String cnaeReference) {
        return List.of(
                meiCandidate("C1", "MEI_OWNER_OPERATOR", "MEI_ATENDIMENTO_DIRETO", "MEI/autônomo que atende clientes diretamente em " + cnaeReference),
                meiCandidate("C2", "MEI_OWNER_OPERATOR", "MEI_ROTINA_EXECUCAO", "Dono-operador que executa pessoalmente a rotina diária de " + cnaeReference),
                meiCandidate("C3", "MEI_OWNER_OPERATOR", "MEI_AQUISICAO_CLIENTES", "MEI/autônomo que precisa conseguir clientes, responder mensagens e manter agenda em " + cnaeReference),
                meiCandidate("C4", "MEI_OWNER_OPERATOR", "MEI_OPERACAO_DIGITAL_LOCAL", "MEI/autônomo que usa WhatsApp, Instagram ou indicação local para operar " + cnaeReference),
                meiCandidate("C5", "MEI_OWNER_OPERATOR", "MEI_PRECO_COBRANCA", "Dono-operador que define preço, cobra, negocia e lida com inadimplência em " + cnaeReference),
                meiCandidate("C6", "MEI_OWNER_OPERATOR", "MEI_AGENDA_RECORRENCIA", "MEI/autônomo que depende de agenda, retorno, recorrência e fidelização em " + cnaeReference),
                meiCandidate("C7", "MEI_OWNER_OPERATOR", "MEI_INSUMOS_FORNECEDORES", "MEI/autônomo que compra insumos, prepara entrega e negocia fornecedores para " + cnaeReference),
                meiCandidate("C8", "MEI_OWNER_OPERATOR", "MEI_POS_VENDA_RETRABALHO", "Dono-operador que resolve pós-venda, retrabalho e dúvidas de clientes em " + cnaeReference),
                meiCandidate("C9", "MEI_OWNER_OPERATOR", "MEI_SAZONALIDADE_DEMANDA", "MEI/autônomo que enfrenta sazonalidade, picos de demanda e semanas sem cliente em " + cnaeReference),
                meiCandidate("C10", "MEI_OWNER_OPERATOR", "MEI_LINGUAGEM_CLIENTES", "MEI/autônomo que usa linguagem simples com clientes e responde dúvidas recorrentes em " + cnaeReference));
    }

    /** Usa a descrição do CNAE quando disponível para evitar contexto operacional genérico por código numérico. */
    private String cnaeReference(Map<String, Object> input, String cnaeCode) {
        Object description = input.get("cnaeDescription");
        if (description instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return "CNAE " + cnaeCode;
    }

    /** Monta um candidato focado em MEI/autônomo sem dor ou promessa ainda não comprovadas. */
    private Map<String, Object> meiCandidate(String candidateId, String operator, String job, String operationalContext) {
        return Map.of(
                "candidateId", candidateId,
                "operator", operator,
                "job", job,
                "buyerTypes", List.of("B2C"),
                "audienceFocus", "MEI_AUTONOMO_DONO_OPERADOR",
                "workerMode", "EXECUTA_PESSOALMENTE_O_TRABALHO",
                "evidenceFocus", List.of("rotina", "aquisição de clientes", "atendimento", "cobrança", "linguagem real"),
                "operationalContext", operationalContext,
                "painHypotheses", List.of(),
                "priorConfidence", "LOW");
    }
}
