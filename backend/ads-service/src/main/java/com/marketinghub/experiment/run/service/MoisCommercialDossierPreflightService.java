package com.marketinghub.experiment.run.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Avalia se o experimento possui evidência comercial MOIS suficiente antes de liberar mídia. */
@Service
public class MoisCommercialDossierPreflightService {
    private static final List<String> COMMERCIAL_DOSSIER_PIPELINES = List.of(
            "salespagepatterns.v1",
            "warmupecosystem.v1");
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "que", "por", "dos", "das", "sem", "sobre", "como", "mais",
            "menos", "seu", "sua", "seus", "suas", "de", "do", "da", "em", "no", "na", "os", "as",
            "um", "ao", "aos", "experimento", "hipotese", "hipótese", "nicho", "teste");

    private final PipelineDossieProdutoRepository pipelineDossieProdutoRepository;

    /** Cria o serviço com o repositório de auditoria dos dossiês MOIS. */
    public MoisCommercialDossierPreflightService(PipelineDossieProdutoRepository pipelineDossieProdutoRepository) {
        this.pipelineDossieProdutoRepository = pipelineDossieProdutoRepository;
    }

    /** Compara o contexto do experimento com os dossiês comerciais concluídos mais recentes. */
    public CommercialDossierPreflightResult evaluate(Experiment experiment) {
        Set<String> experimentTerms = commercialTerms(experiment);
        if (experimentTerms.isEmpty()) {
            return CommercialDossierPreflightResult.blocked(
                    "Experimento sem termos comerciais suficientes para buscar dossiês MOIS aderentes.",
                    null);
        }
        List<PipelineDossieProduto> dossiers = pipelineDossieProdutoRepository.findCompletedCommercialDossiers(
                COMMERCIAL_DOSSIER_PIPELINES, PageRequest.of(0, 50));
        List<DossierMatch> matches = dossiers.stream()
                .map(dossier -> match(dossier, experimentTerms))
                .filter(DossierMatch::isRelevant)
                .limit(3)
                .toList();
        if (matches.isEmpty()) {
            return CommercialDossierPreflightResult.blocked(
                    "Nenhum dossiê MOIS concluído e aderente ao nicho/hipótese foi encontrado para pré-validar a oferta.",
                    "termos=" + String.join(",", experimentTerms.stream().limit(12).toList()));
        }
        DossierMatch bestMatch = matches.get(0);
        return CommercialDossierPreflightResult.approved(
                "Dossiê MOIS aderente encontrado antes da mídia: " + bestMatch.summary(),
                evidenceReference(matches));
    }

    /** Extrai termos comerciais úteis do experimento e da hipótese associada. */
    private Set<String> commercialTerms(Experiment experiment) {
        Set<String> terms = new LinkedHashSet<>();
        if (experiment == null) {
            return terms;
        }
        addTerms(terms, experiment.getName());
        addTerms(terms, experiment.getHypothesis());
        addTerms(terms, experiment.getSinglePain());
        addTerms(terms, experiment.getFreeReward());
        addTerms(terms, experiment.getFunnelPromise());
        if (experiment.getNiche() != null) {
            addTerms(terms, experiment.getNiche().getName());
        }
        Hypothesis hypothesis = experiment.getHypothesisRef();
        if (hypothesis != null) {
            addTerms(terms, hypothesis.getTitle());
            addTerms(terms, hypothesis.getPersona());
            addTerms(terms, hypothesis.getProblem());
            addTerms(terms, hypothesis.getPromise());
            addTerms(terms, hypothesis.getMechanism());
            addTerms(terms, hypothesis.getUniqueMechanism());
            addTerms(terms, hypothesis.getEntrega());
        }
        return terms;
    }

    /** Adiciona tokens relevantes ao conjunto de comparação comercial. */
    private void addTerms(Set<String> terms, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : TOKEN_SPLITTER.split(value.toLowerCase(Locale.ROOT))) {
            if (token.length() >= 4 && !STOP_WORDS.contains(token)) {
                terms.add(token);
            }
        }
    }

    /** Calcula aderência simples por interseção entre termos do experimento e texto do dossiê. */
    private DossierMatch match(PipelineDossieProduto dossier, Set<String> experimentTerms) {
        String text = (safe(dossier.getRespostaFinal()) + " " + safe(dossier.getResponse()) + " " + safe(dossier.getRequest()))
                .toLowerCase(Locale.ROOT);
        List<String> matchedTerms = new ArrayList<>();
        for (String term : experimentTerms) {
            if (text.contains(term)) {
                matchedTerms.add(term);
            }
        }
        return new DossierMatch(dossier.getId(), dossier.getIdExterno(), dossier.getPipelineCode(), matchedTerms);
    }

    /** Monta referência auditável compacta para o gate de preflight. */
    private String evidenceReference(List<DossierMatch> matches) {
        return "mois-dossiers:" + matches.stream()
                .map(match -> "%s/%s/terms=%s".formatted(
                        match.pipelineCode(),
                        match.dossierId(),
                        String.join("|", match.matchedTerms().stream().limit(5).toList())))
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    /** Normaliza texto nulo para comparação segura. */
    private String safe(String value) {
        return value != null ? value : "";
    }

    /** Resultado objetivo usado pelo preflight do run para aprovar ou bloquear mídia. */
    public record CommercialDossierPreflightResult(boolean approved, String summary, String evidenceReference) {
        /** Cria resultado aprovado com resumo e referência de evidência. */
        public static CommercialDossierPreflightResult approved(String summary, String evidenceReference) {
            return new CommercialDossierPreflightResult(true, summary, evidenceReference);
        }

        /** Cria resultado bloqueado com resumo e referência diagnóstica. */
        public static CommercialDossierPreflightResult blocked(String summary, String evidenceReference) {
            return new CommercialDossierPreflightResult(false, summary, evidenceReference);
        }
    }

    /** Representa a aderência de um dossiê aos termos comerciais do experimento. */
    private record DossierMatch(Long dossierId, String productKey, String pipelineCode, List<String> matchedTerms) {
        /** Indica se o dossiê possui sinais mínimos para orientar o experimento. */
        private boolean isRelevant() {
            return matchedTerms.size() >= 2;
        }

        /** Resume a referência comercial encontrada para texto do gate. */
        private String summary() {
            return "%s #%d com termos %s".formatted(pipelineCode, dossierId, String.join(", ", matchedTerms.stream().limit(5).toList()));
        }
    }
}
