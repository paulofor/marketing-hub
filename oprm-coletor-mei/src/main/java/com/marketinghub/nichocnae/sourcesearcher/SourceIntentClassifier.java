package com.marketinghub.nichocnae.sourcesearcher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Classifica resultados públicos por aderência à rotina real antes da persistência da etapa três. */
@Component
public class SourceIntentClassifier {
    private static final String INTENT_ROUTINE_REPORT = "ROUTINE_REPORT";
    private static final String INTENT_REAL_QUESTION = "REAL_QUESTION";
    private static final String INTENT_PRACTICAL_GUIDE = "PRACTICAL_GUIDE";
    private static final String INTENT_EDUCATIONAL_CONTENT = "EDUCATIONAL_CONTENT";
    private static final String INTENT_COMMERCIAL_PAGE_RISK = "COMMERCIAL_PAGE_RISK";
    private static final String INTENT_GENERIC_PUBLIC_CONTENT = "GENERIC_PUBLIC_CONTENT";
    private static final String TYPE_BRAZILIAN_OFFICIAL_SOURCE = "BRAZILIAN_OFFICIAL_SOURCE";
    private static final String TYPE_RECENT_SECTOR_CONTENT = "RECENT_SECTOR_CONTENT";
    private static final String TYPE_REAL_PROFESSIONAL_REPORT_OR_QUESTION = "REAL_PROFESSIONAL_REPORT_OR_QUESTION";
    private static final String TYPE_SOCIAL_OR_COMMUNITY_CONTENT = "SOCIAL_OR_COMMUNITY_CONTENT";
    private static final String TYPE_RECENT_NEWS = "RECENT_NEWS";
    private static final String TYPE_COMMERCIAL_PAGE = "COMMERCIAL_PAGE";
    private static final String TYPE_OLD_OR_UNDATED_CONTENT = "OLD_OR_UNDATED_CONTENT";
    private static final String TYPE_STRUCTURED_COMPANY_CONTENT = "STRUCTURED_COMPANY_CONTENT";
    private static final int RECENT_MONTHS_LIMIT = 24;
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(20[0-9]{2})\\b");
    private static final List<String> ROUTINE_TERMS = List.of(
            "rotina", "dia a dia", "tarefas", "atividade", "trabalho", "processo", "procedimento", "operação",
            "execução", "executa", "executado", "executada", "atendimento cliente", "atendimento real",
            "tarefas diárias", "tarefas do dia a dia", "rotina de trabalho", "rotina manual",
            "prática profissional", "relato da rotina", "como atende");
    private static final List<String> PRACTICAL_EXECUTION_TERMS = List.of(
            "o que faz", "como faz", "atendimento", "cliente", "higiene", "esterilização", "lavar", "cortar",
            "escovar", "colorir", "preparar", "aplicar", "organizar", "limpar", "cuidar",
            "agenda de atendimento", "confirmar horário", "remarcar", "cobrar", "orçamento", "materiais",
            "antes do atendimento", "durante o atendimento", "depois do atendimento");
    private static final List<String> PROFESSIONAL_SOURCE_TERMS = List.of(
            "cbo", "classificação brasileira de ocupações", "guia profissional", "relato de profissional",
            "relato profissional", "profissional relata", "profissionais relatam", "experiência profissional",
            "ocupação", "descrição da ocupação", "atribuições", "funções");
    private static final List<String> PROBLEM_TERMS = List.of(
            "problema", "dificuldade", "desafio", "erro", "reclamação", "dúvida", "pergunta", "como fazer",
            "cansaço", "ansiedade", "estresse", "sobrecarga", "insegurança", "medo", "frustração",
            "falta", "cancelamento", "atraso", "retrabalho");
    private static final List<String> GUIDE_TERMS = List.of(
            "guia", "passo a passo", "manual", "tutorial", "boas práticas", "checklist", "orientação");
    private static final List<String> EDUCATIONAL_TERMS = List.of(
            "curso", "aula", "apostila", "artigo", "entenda", "o que é", "conceito");
    private static final List<String> COMMERCIAL_TERMS = List.of(
            "comprar", "promoção", "software", "plataforma", "ferramenta", "curso online",
            "consultoria", "agende", "contrate", "venda", "solução", "produto", "agenda online",
            "app", "aplicativo", "automação", "sistema", "template", "landing page", "funil", "campanha",
            "demonstração", "teste grátis", "planos", "assinatura");
    private static final List<String> SOFTWARE_SALES_TERMS = List.of(
            "software", "plataforma", "agenda online", "app", "aplicativo", "automação", "sistema", "gestão de salão",
            "sistema para salão", "sistema para manicure", "marcação online", "reservas online", "crm",
            "template", "curso online", "ferramenta", "ia para", "inteligência artificial");
    private static final List<String> SOLUTION_SOURCE_TERMS = List.of(
            "solução", "software", "ferramenta", "produto", "comprar", "contrate", "agende uma demonstração",
            "teste grátis", "planos", "assinatura", "automação", "plataforma", "app", "aplicativo", "curso online",
            "template", "funil", "campanha", "landing page");
    private static final List<String> COMMERCIAL_DOMAINS = List.of(
            "hotmart.", "kiwify.", "eduzz.", "monetizze.", "shopify.", "mercadolivre.", "amazon.");
    private static final List<String> BRAZIL_MARKERS = List.of(
            ".br", "brasil", "brasileir", "mei", "microempreendedor", "autônomo", "autonomo", "sebrae", "gov.br");
    private static final List<String> AUTONOMOUS_TERMS = List.of(
            "mei", "microempreendedor", "autônomo", "autonomo", "profissional liberal", "por conta própria",
            "trabalhador independente", "dono-operador", "freelancer", "empreendedor individual");
    private static final List<String> STRUCTURED_BUSINESS_TERMS = List.of(
            "empresa de médio porte", "grande empresa", "corporativo", "franquia", "indústria", "rede de lojas",
            "departamento", "equipe comercial", "gestão empresarial", "b2b");
    private static final List<String> OFFICIAL_DOMAINS = List.of("gov.br", "sebrae.com.br", "receita.fazenda.gov.br");
    private static final List<String> COMMUNITY_DOMAINS = List.of(
            "reddit.com", "youtube.com", "youtu.be", "quora.com", "facebook.com", "instagram.com", "tiktok.com");
    private static final List<String> NEWS_DOMAINS = List.of("g1.globo.com", "uol.com.br", "estadao.com.br", "folha.uol.com.br", "valor.globo.com");

    /** Retorna uma cópia do resultado com intenção, escore e riscos calculados por heurística conservadora. */
    public SourceSearchResult classify(SourceSearchResult result) {
        String text = normalized(result.sourceTitle() + " " + result.sourceSnippet() + " " + result.sourceUrl());
        String domain = normalized(result.sourceDomain());
        int routineHits = countHits(text, ROUTINE_TERMS);
        int problemHits = countHits(text, PROBLEM_TERMS);
        int guideHits = countHits(text, GUIDE_TERMS);
        int practicalExecutionHits = countHits(text, PRACTICAL_EXECUTION_TERMS);
        int professionalSourceHits = countHits(text, PROFESSIONAL_SOURCE_TERMS);
        int educationalHits = countHits(text, EDUCATIONAL_TERMS);
        int commercialHits = countHits(text, COMMERCIAL_TERMS) + countHits(domain, COMMERCIAL_DOMAINS);
        int softwareSalesHits = countHits(text, SOFTWARE_SALES_TERMS);
        int realWorkEvidenceHits = countRealWorkEvidenceHits(text);
        int brazilScore = brazilRelevanceScore(text, domain);
        int autonomousScore = autonomousProfessionalEvidenceScore(text);
        boolean structuredBusinessDriftRisk = structuredBusinessDriftRisk(text, autonomousScore);
        boolean commercialRisk = commercialRisk(
                commercialHits, softwareSalesHits, routineHits, problemHits, practicalExecutionHits, professionalSourceHits,
                autonomousScore, realWorkEvidenceHits);
        boolean solutionRisk = solutionLanguageRisk(text, commercialHits, softwareSalesHits, realWorkEvidenceHits);
        Instant publishedAt = extractPublishedAt(result, text);
        int freshnessScore = sourceFreshnessScore(publishedAt);
        boolean outdatedRisk = outdatedSourceRisk(publishedAt, freshnessScore);
        String intent = classifyIntent(
                routineHits, problemHits, guideHits, practicalExecutionHits, professionalSourceHits, educationalHits,
                commercialRisk || solutionRisk);
        String classificationType = classifySourceType(
                domain, text, commercialRisk || solutionRisk, structuredBusinessDriftRisk, publishedAt, freshnessScore, problemHits,
                practicalExecutionHits, professionalSourceHits);
        int score = routineEvidenceScore(
                routineHits, problemHits, guideHits, practicalExecutionHits, professionalSourceHits, educationalHits,
                commercialHits, softwareSalesHits, realWorkEvidenceHits);
        return new SourceSearchResult(
                result.sourceUrl(),
                result.sourceTitle(),
                result.sourceSnippet(),
                result.sourceDomain(),
                result.searchPosition(),
                intent,
                score,
                commercialRisk,
                solutionRisk,
                classificationType,
                freshnessScore,
                outdatedRisk,
                brazilScore,
                autonomousScore,
                structuredBusinessDriftRisk,
                publishedAt);
    }

    /** Decide a intenção operacional priorizando rotina, perguntas reais e guias não vendedores. */
    private String classifyIntent(
            int routineHits,
            int problemHits,
            int guideHits,
            int practicalExecutionHits,
            int professionalSourceHits,
            int educationalHits,
            boolean commercialRisk) {
        if (commercialRisk) {
            return INTENT_COMMERCIAL_PAGE_RISK;
        }
        if (routineHits > 0 || practicalExecutionHits > 1 || professionalSourceHits > 0) {
            return INTENT_ROUTINE_REPORT;
        }
        if (problemHits > 0) {
            return INTENT_REAL_QUESTION;
        }
        if (guideHits > 0 || practicalExecutionHits > 0) {
            return INTENT_PRACTICAL_GUIDE;
        }
        if (educationalHits > 0) {
            return INTENT_EDUCATIONAL_CONTENT;
        }
        return INTENT_GENERIC_PUBLIC_CONTENT;
    }

    /** Define o tipo de fonte usado para priorizar conteúdo brasileiro, recente e aderente ao autônomo. */
    private String classifySourceType(
            String domain,
            String text,
            boolean commercialRisk,
            boolean structuredBusinessDriftRisk,
            Instant publishedAt,
            int freshnessScore,
            int problemHits,
            int practicalExecutionHits,
            int professionalSourceHits) {
        if (commercialRisk) {
            return TYPE_COMMERCIAL_PAGE;
        }
        if (structuredBusinessDriftRisk) {
            return TYPE_STRUCTURED_COMPANY_CONTENT;
        }
        if (countHits(domain, OFFICIAL_DOMAINS) > 0) {
            return TYPE_BRAZILIAN_OFFICIAL_SOURCE;
        }
        boolean communitySource = countHits(domain, COMMUNITY_DOMAINS) > 0;
        if (professionalSourceHits > 0 && (practicalExecutionHits > 0 || problemHits > 0)) {
            return TYPE_REAL_PROFESSIONAL_REPORT_OR_QUESTION;
        }
        if ((communitySource && (problemHits > 0 || practicalExecutionHits > 0 || containsAny(text, AUTONOMOUS_TERMS)))
                || (problemHits > 0 && containsAny(text, AUTONOMOUS_TERMS))) {
            return TYPE_REAL_PROFESSIONAL_REPORT_OR_QUESTION;
        }
        if (countHits(domain, NEWS_DOMAINS) > 0 && freshnessScore >= 70) {
            return TYPE_RECENT_NEWS;
        }
        if (communitySource) {
            return TYPE_SOCIAL_OR_COMMUNITY_CONTENT;
        }
        if (publishedAt == null || freshnessScore < 40) {
            return TYPE_OLD_OR_UNDATED_CONTENT;
        }
        return TYPE_RECENT_SECTOR_CONTENT;
    }

    /** Calcula escore simples para ordenar fontes com maior evidência de rotina antes de páginas comerciais. */
    private int routineEvidenceScore(
            int routineHits,
            int problemHits,
            int guideHits,
            int practicalExecutionHits,
            int professionalSourceHits,
            int educationalHits,
            int commercialHits,
            int softwareSalesHits,
            int realWorkEvidenceHits) {
        int score = 40
                + routineHits * 22
                + practicalExecutionHits * 14
                + professionalSourceHits * 18
                + problemHits * 12
                + guideHits * 10
                + educationalHits * 4
                + realWorkEvidenceHits * 10
                - commercialHits * 16
                - softwareSalesHits * 24;
        return Math.max(0, Math.min(100, score));
    }

    /** Marca risco comercial quando venda de sistema domina a fonte sem tarefas concretas do executor. */
    private boolean commercialRisk(
            int commercialHits,
            int softwareSalesHits,
            int routineHits,
            int problemHits,
            int practicalExecutionHits,
            int professionalSourceHits,
            int autonomousScore,
            int realWorkEvidenceHits) {
        int concreteExecutionSignals = routineHits + problemHits + practicalExecutionHits + professionalSourceHits;
        if (softwareSalesHits > 0 && concreteExecutionSignals + realWorkEvidenceHits < 3) {
            return true;
        }
        return commercialHits > 0 && concreteExecutionSignals + realWorkEvidenceHits + autonomousScore / 25 < 2;
    }

    /** Marca fonte de solução quando a página vende resposta pronta em vez de relatar rotina do executor. */
    private boolean solutionLanguageRisk(
            String text, int commercialHits, int softwareSalesHits, int realWorkEvidenceHits) {
        return containsAny(text, SOLUTION_SOURCE_TERMS)
                && commercialHits + softwareSalesHits > realWorkEvidenceHits;
    }

    /** Conta grupos de evidência humana e operacional que indicam trabalho real em vez de página de solução. */
    private int countRealWorkEvidenceHits(String text) {
        return countHits(text, List.of(
                "rotina manual", "atendimento real", "relato", "cliente faltou", "desmarcou", "fidelização",
                "recorrência", "indicação", "boca a boca", "mensagem de cliente", "como eu faço",
                "minha rotina", "meus clientes", "dor", "medo", "insegurança", "cobrar cliente"));
    }

    /** Calcula aderência Brasil-first por domínio e marcadores explícitos do mercado brasileiro. */
    private int brazilRelevanceScore(String text, String domain) {
        int score = countHits(text + " " + domain, BRAZIL_MARKERS) * 25;
        if (domain.endsWith(".br") || domain.contains("gov.br")) {
            score += 35;
        }
        return Math.max(0, Math.min(100, score));
    }

    /** Calcula evidência de que a fonte fala de MEI, autônomo ou trabalhador por conta própria. */
    private int autonomousProfessionalEvidenceScore(String text) {
        return Math.max(0, Math.min(100, countHits(text, AUTONOMOUS_TERMS) * 30));
    }

    /** Marca risco quando a fonte parece falar de empresa estruturada e não do profissional executor. */
    private boolean structuredBusinessDriftRisk(String text, int autonomousScore) {
        return countHits(text, STRUCTURED_BUSINESS_TERMS) > 0 && autonomousScore < 40;
    }

    /** Calcula atualidade privilegiando datas extraídas dentro dos últimos vinte e quatro meses. */
    private int sourceFreshnessScore(Instant publishedAt) {
        if (publishedAt == null) {
            return 35;
        }
        LocalDate publishedDate = LocalDate.ofInstant(publishedAt, ZoneOffset.UTC);
        LocalDate recentLimit = LocalDate.now(ZoneOffset.UTC).minusMonths(RECENT_MONTHS_LIMIT);
        if (!publishedDate.isBefore(recentLimit)) {
            return 100;
        }
        if (publishedDate.isAfter(LocalDate.now(ZoneOffset.UTC).minusYears(4))) {
            return 60;
        }
        return 20;
    }

    /** Marca risco de fonte antiga ou sem data para impedir que vire verdade principal sem revisão. */
    private boolean outdatedSourceRisk(Instant publishedAt, int freshnessScore) {
        return publishedAt == null || freshnessScore < 70;
    }

    /** Extrai uma data aproximada de publicação quando o provedor expõe ano no resultado público. */
    private Instant extractPublishedAt(SourceSearchResult result, String normalizedText) {
        if (result.publishedAt() != null) {
            return result.publishedAt();
        }
        Matcher matcher = YEAR_PATTERN.matcher(normalizedText);
        int latestYear = 0;
        while (matcher.find()) {
            latestYear = Math.max(latestYear, Integer.parseInt(matcher.group(1)));
        }
        if (latestYear == 0) {
            return null;
        }
        return LocalDate.of(latestYear, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /** Conta quantos grupos de termos aparecem no texto normalizado. */
    private int countHits(String text, List<String> terms) {
        int hits = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                hits++;
            }
        }
        return hits;
    }

    /** Indica se pelo menos um termo aparece no texto normalizado. */
    private boolean containsAny(String text, List<String> terms) {
        return countHits(text, terms) > 0;
    }

    /** Normaliza texto para comparação determinística sem depender de IA. */
    private String normalized(String value) {
        return StringUtils.hasText(value) ? value.toLowerCase(Locale.ROOT) : "";
    }
}
