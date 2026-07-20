package com.marketinghub.planning.service;

import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanWeekObjective;
import com.marketinghub.planning.dto.CommercialPlanFunnelStageDto;
import com.marketinghub.planning.dto.CommercialPlanWeekDto;
import com.marketinghub.planning.dto.CommercialPlanWeekExperimentDto;
import com.marketinghub.planning.dto.CommercialPlanWeekObjectiveDto;
import com.marketinghub.planning.dto.UpdateCommercialPlanWeekObjectivesRequest;
import com.marketinghub.repository.jpa.planning.CommercialPlanWeekObjectiveRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: montar a leitura semanal de experimentos do planejamento comercial. */
@Service
public class CommercialPlanWeeklyExperimentService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final List<String> JULY_FIRST_WEEK_DEFAULT_OBJECTIVES = List.of(
            "Manter venda real como objetivo principal; checkout_click e lead servem apenas como diagnostico intermediario.",
            "Garantir tracking confiavel antes de gastar: campanha sem metrica atualizada nao pode continuar rodando como aprendizado valido.",
            "Colocar prova visual forte antes do preco para reduzir duvida e aumentar disposicao de pagamento.",
            "Separar criterio por funil: venda direta mede compra, previa paga mede checkout iniciado e captura mede lead que avanca para checkout.",
            "Aplicar trava de gasto: experimento sem resultado primario ou aprendizado acionavel deve ser parado ao atingir o limite operacional.");

    private final CommercialPlanService planService;
    private final JdbcTemplate jdbcTemplate;
    private final CommercialPlanWeekObjectiveRepository objectiveRepository;
    private final CurrencyConversionService currencyConversionService;
    private final Clock clock;

    /** Inicializa o serviço com a fonte de plano e acesso SQL de leitura operacional. */
    @Autowired
    public CommercialPlanWeeklyExperimentService(
            CommercialPlanService planService,
            JdbcTemplate jdbcTemplate,
            CommercialPlanWeekObjectiveRepository objectiveRepository,
            CurrencyConversionService currencyConversionService) {
        this(planService, jdbcTemplate, objectiveRepository, currencyConversionService, Clock.systemUTC());
    }

    /** Inicializa o serviço permitindo controlar o relogio em testes de janela semanal. */
    CommercialPlanWeeklyExperimentService(
            CommercialPlanService planService,
            JdbcTemplate jdbcTemplate,
            CommercialPlanWeekObjectiveRepository objectiveRepository,
            CurrencyConversionService currencyConversionService,
            Clock clock) {
        this.planService = planService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectiveRepository = objectiveRepository;
        this.currencyConversionService = currencyConversionService;
        this.clock = clock;
    }

    /** Lista as semanas comerciais do mes do plano com experimentos do periodo e objetivos da proxima semana. */
    @Transactional(readOnly = true)
    public List<CommercialPlanWeekDto> listWeeks(Long planId) {
        return listWeeks(planId, null);
    }

    /** Lista as semanas comerciais do mes informado ou do mes de referencia do plano. */
    @Transactional(readOnly = true)
    public List<CommercialPlanWeekDto> listWeeks(Long planId, String referenceMonth) {
        CommercialPlan plan = planService.getPlan(planId);
        YearMonth planMonth = resolvePlanMonth(plan);
        YearMonth selectedMonth = resolveReferenceMonth(referenceMonth, planMonth);
        boolean planReferenceMonth = selectedMonth.equals(planMonth);
        List<WeekPeriod> periods = buildCommercialWeeks(selectedMonth);
        List<CommercialPlanWeekDto> weeks = new ArrayList<>();
        for (int index = 0; index < periods.size(); index++) {
            WeekPeriod period = periods.get(index);
            int weekNumber = index + 1;
            List<CommercialPlanWeekExperimentDto> experiments =
                    listExperiments(period.startDate(), period.endDate().plusDays(1));
            boolean hasNextWeek = index + 1 < periods.size();
            boolean objectivesEditable = planReferenceMonth && hasNextWeek && isObjectiveEditWindowOpen(period.endDate());
            Integer objectiveWeekNumber = weekNumber + 1;
            weeks.add(new CommercialPlanWeekDto(
                    weekNumber,
                    period.startDate(),
                    period.endDate(),
                    experiments.size(),
                    sumCost(experiments),
                    sumRevenue(experiments),
                    objectivesEditable,
                    objectiveEditWindowMessage(period.endDate(), objectivesEditable, planReferenceMonth),
                    buildFunnelStages(experiments),
                    planReferenceMonth && hasNextWeek ? listObjectives(plan, objectiveWeekNumber) : List.of(),
                    experiments));
        }
        return weeks;
    }

    /** Atualiza os objetivos da próxima semana a partir do card semanal informado. */
    @Transactional
    public List<CommercialPlanWeekObjectiveDto> updateObjectives(
            Long planId,
            Integer weekNumber,
            UpdateCommercialPlanWeekObjectivesRequest request) {
        CommercialPlan plan = planService.getPlan(planId);
        WeekPeriod period = resolveWeekPeriod(plan, weekNumber);
        if (!isObjectiveEditWindowOpen(period.endDate())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    objectiveEditWindowMessage(period.endDate(), false, true));
        }
        Integer objectiveWeekNumber = weekNumber + 1;
        resolveWeekPeriod(plan, objectiveWeekNumber);
        objectiveRepository.deleteByPlanIdAndWeekNumber(planId, objectiveWeekNumber);
        List<CommercialPlanWeekObjective> objectives = new ArrayList<>();
        List<UpdateCommercialPlanWeekObjectivesRequest.Item> items =
                request == null || request.objectives() == null ? List.of() : request.objectives();
        int nextOrder = 1;
        for (UpdateCommercialPlanWeekObjectivesRequest.Item item : items) {
            if (item.objectiveText() == null || item.objectiveText().isBlank()) {
                continue;
            }
            objectives.add(CommercialPlanWeekObjective.builder()
                    .plan(plan)
                    .weekNumber(objectiveWeekNumber)
                    .sequenceOrder(nextOrder)
                    .objectiveText(item.objectiveText().trim())
                    .score(normalizeScore(item.score()))
                    .build());
            nextOrder++;
        }
        return objectiveRepository.saveAll(objectives).stream()
                .map(this::toObjectiveDto)
                .toList();
    }

    /** Resolve a semana solicitada dentro do mes de referencia do plano. */
    private WeekPeriod resolveWeekPeriod(CommercialPlan plan, Integer weekNumber) {
        if (weekNumber == null || weekNumber < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semana do planejamento invalida.");
        }
        List<WeekPeriod> periods = buildCommercialWeeks(resolvePlanMonth(plan));
        for (int index = 0; index < periods.size(); index++) {
            if (index + 1 == weekNumber) {
                return periods.get(index);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semana do planejamento fora do mes do plano.");
    }

    /** Verifica se a edicao de objetivos esta dentro de dois dias antes ou depois do fim da semana. */
    private boolean isObjectiveEditWindowOpen(LocalDate weekEndDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowStart = weekEndDate.minusDays(2);
        LocalDate windowEnd = weekEndDate.plusDays(2);
        return !today.isBefore(windowStart) && !today.isAfter(windowEnd);
    }

    /** Monta a mensagem de disponibilidade da janela de objetivos semanais. */
    private String objectiveEditWindowMessage(LocalDate weekEndDate, boolean editable, boolean planReferenceMonth) {
        if (!planReferenceMonth) {
            return "Objetivos editaveis apenas no mes de referencia do plano.";
        }
        LocalDate windowStart = weekEndDate.minusDays(2);
        LocalDate windowEnd = weekEndDate.plusDays(2);
        if (editable) {
            return "Objetivos liberados ate " + windowEnd + ".";
        }
        return "Objetivos disponiveis de " + windowStart + " ate " + windowEnd + ".";
    }

    /** Resolve o mes de referencia do plano a partir do prazo ou da criacao. */
    private YearMonth resolvePlanMonth(CommercialPlan plan) {
        if (plan.getDeadline() != null) {
            return YearMonth.from(plan.getDeadline());
        }
        Instant createdAt = plan.getCreatedAt() == null ? Instant.now() : plan.getCreatedAt();
        LocalDate createdDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
        return YearMonth.from(createdDate);
    }

    /** Resolve o mes solicitado pela tela mantendo o mes do plano como padrao. */
    private YearMonth resolveReferenceMonth(String referenceMonth, YearMonth planMonth) {
        if (referenceMonth == null || referenceMonth.isBlank()) {
            return planMonth;
        }
        try {
            return YearMonth.parse(referenceMonth.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mes de referencia invalido. Use o formato yyyy-MM.",
                    ex);
        }
    }

    /** Monta semanas comerciais pela primeira, segunda, terceira, quarta e quinta segunda-feira do mes. */
    private List<WeekPeriod> buildCommercialWeeks(YearMonth month) {
        List<WeekPeriod> periods = new ArrayList<>();
        LocalDate monday = firstMondayOfMonth(month);
        while (YearMonth.from(monday).equals(month)) {
            periods.add(new WeekPeriod(monday, monday.plusDays(6)));
            monday = monday.plusWeeks(1);
        }
        return periods;
    }

    /** Localiza a primeira segunda-feira do mes comercial. */
    private LocalDate firstMondayOfMonth(YearMonth month) {
        LocalDate date = month.atDay(1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    /** Lista objetivos persistidos ou a sugestao inicial da primeira semana de julho. */
    private List<CommercialPlanWeekObjectiveDto> listObjectives(CommercialPlan plan, Integer weekNumber) {
        List<CommercialPlanWeekObjectiveDto> objectives = objectiveRepository
                .findByPlanIdAndWeekNumberOrderBySequenceOrderAsc(plan.getId(), weekNumber)
                .stream()
                .map(this::toObjectiveDto)
                .toList();
        if (!objectives.isEmpty() || !isJulyFirstWeek(plan, weekNumber)) {
            return objectives;
        }
        List<CommercialPlanWeekObjectiveDto> defaults = new ArrayList<>();
        for (int index = 0; index < JULY_FIRST_WEEK_DEFAULT_OBJECTIVES.size(); index++) {
            defaults.add(new CommercialPlanWeekObjectiveDto(
                    null,
                    index + 1,
                    JULY_FIRST_WEEK_DEFAULT_OBJECTIVES.get(index),
                    null));
        }
        return defaults;
    }

    /** Verifica se a semana deve receber a sugestao inicial de objetivos de julho. */
    private boolean isJulyFirstWeek(CommercialPlan plan, Integer weekNumber) {
        return Integer.valueOf(1).equals(weekNumber)
                && plan.getDeadline() != null
                && plan.getDeadline().getYear() == 2026
                && plan.getDeadline().getMonthValue() == 7;
    }

    /** Converte um objetivo semanal para o contrato da tela. */
    private CommercialPlanWeekObjectiveDto toObjectiveDto(CommercialPlanWeekObjective objective) {
        return new CommercialPlanWeekObjectiveDto(
                objective.getId(),
                objective.getSequenceOrder(),
                objective.getObjectiveText(),
                objective.getScore());
    }

    /** Mantem a nota dentro da escala simples de avaliacao semanal. */
    private Integer normalizeScore(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(10, score));
    }

    /** Busca os experimentos criados no intervalo e agrega custos rastreáveis por experimento. */
    private List<CommercialPlanWeekExperimentDto> listExperiments(LocalDate startInclusive, LocalDate endExclusive) {
        Timestamp start = Timestamp.from(startInclusive.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp end = Timestamp.from(endExclusive.atStartOfDay().toInstant(ZoneOffset.UTC));
        return jdbcTemplate.query("""
                select
                    e.id,
                    e.name,
                    e.niche_id,
                    mn.name as niche_name,
                    lower(concat(
                        substr(hex(h.id), 1, 8), '-',
                        substr(hex(h.id), 9, 4), '-',
                        substr(hex(h.id), 13, 4), '-',
                        substr(hex(h.id), 17, 4), '-',
                        substr(hex(h.id), 21, 12)
                    )) as hypothesis_id,
                    h.title as hypothesis_title,
                    coalesce(e.product_ai_subtype, e.experiment_type) as product_type,
                    case when e.creation_source = 'MANUAL_FLOW' then 1 else 0 end as manual,
                    case when ab_test.experiment_id is null then 0 else 1 end as ab_test,
                    e.status,
                    e.created_at,
                    coalesce(campaign.cost, 0) as campaign_cost,
                    coalesce(financial.ai_cost, 0) as ai_cost,
                    coalesce(video.cost, 0) as video_cost_usd,
                    coalesce(financial.revenue, 0) as revenue,
                    coalesce(nullif(financial.impressions, 0), campaign.impressions, 0) as impressions,
                    coalesce(nullif(financial.clicks, 0), campaign.clicks, 0) as clicks,
                    coalesce(nullif(financial.visitors, 0), analytics.visitors, 0) as visitors,
                    coalesce(nullif(financial.leads, 0), campaign.leads, 0) as leads,
                    coalesce(financial.checkout_clicks, 0) as checkout_clicks,
                    coalesce(financial.purchases, 0) as purchases,
                    analytics.average_product_view_time_ms
                from experiment e
                left join market_niche mn on mn.id = e.niche_id
                left join hypothesis h on h.id = e.hypothesis_id
                left join (
                    select
                        m.experiment_id,
                        sum(coalesce(m.spend, 0)) as cost,
                        sum(coalesce(m.impressions, 0)) as impressions,
                        sum(coalesce(m.clicks, 0)) as clicks,
                        sum(coalesce(m.leads, 0)) as leads
                    from experiment_campaign_metric m
                    where (
                        m.date_start is not null
                        and m.date_start < ?
                        and (m.date_stop is null or m.date_stop >= ?)
                    )
                    or (
                        m.date_start is null
                        and m.updated_at >= ?
                        and m.updated_at < ?
                    )
                    group by m.experiment_id
                ) campaign on campaign.experiment_id = e.id
                left join (
                    select b.external_experiment_id as experiment_id,
                           sum(coalesce(f.ai_cost_cents, 0)) / 100.0 as ai_cost,
                           sum(coalesce(f.revenue_cents, 0)) / 100.0 as revenue,
                           sum(coalesce(f.impressions, 0)) as impressions,
                           sum(coalesce(f.clicks, 0)) as clicks,
                           sum(coalesce(f.visitors, 0)) as visitors,
                           sum(coalesce(f.leads, 0)) as leads,
                           sum(coalesce(f.checkout_clicks, 0)) as checkout_clicks,
                           sum(coalesce(f.purchases, 0)) as purchases
                    from experiment_budget b
                    join experiment_financial_metric f on f.experiment_budget_id = b.id
                    where f.measured_at >= ? and f.measured_at < ?
                    group by b.external_experiment_id
                ) financial on financial.experiment_id = e.id
                left join (
                    select experiment_id, sum(coalesce(cost, 0)) as cost
                    from experiment_video_asset
                    where created_at >= ? and created_at < ?
                    group by experiment_id
                ) video on video.experiment_id = e.id
                left join (
                    select distinct experiment_id
                    from experiment_sales_page_ab_test
                ) ab_test on ab_test.experiment_id = e.id
                left join (
                    select
                        event_times.experiment_id,
                        count(distinct event_times.session_id) as visitors,
                        round(coalesce(sum(event_times.elapsed_ms), 0) / count(distinct event_times.session_id)) as average_product_view_time_ms
                    from (
                        select
                            lae.experiment_id,
                            coalesce(lae.session_id, concat('evento:', lae.id)) as session_id,
                            case
                                when lower(lae.event_type) = 'section_view_time'
                                then cast(nullif(substring_index(substring_index(efe.payload, 'elapsedMs=', -1), ';', 1), '') as unsigned)
                                else null
                            end as elapsed_ms
                        from experiment_landing_analytics_event lae
                        join experiment_funnel_event efe on efe.id = lae.funnel_event_id
                        where lae.occurred_at >= ? and lae.occurred_at < ?
                    ) event_times
                    group by event_times.experiment_id
                ) analytics on analytics.experiment_id = e.id
                where e.created_at >= ? and e.created_at < ?
                order by analytics.average_product_view_time_ms desc, e.created_at asc, e.id asc
                """, this::mapExperiment, java.sql.Date.valueOf(endExclusive), java.sql.Date.valueOf(startInclusive),
                start, end, start, end, start, end, start, end, start, end);
    }

    /** Converte a linha SQL no contrato de experimento semanal. */
    private CommercialPlanWeekExperimentDto mapExperiment(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal campaignCost = money(rs.getBigDecimal("campaign_cost"));
        BigDecimal aiCost = money(rs.getBigDecimal("ai_cost"));
        BigDecimal videoCost = money(currencyConversionService.usdToBrl(rs.getBigDecimal("video_cost_usd")));
        BigDecimal revenue = money(rs.getBigDecimal("revenue"));
        BigDecimal totalCost = money(campaignCost.add(aiCost).add(videoCost));
        return new CommercialPlanWeekExperimentDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("niche_id"),
                rs.getString("niche_name"),
                rs.getString("hypothesis_id"),
                rs.getString("hypothesis_title"),
                rs.getString("product_type"),
                rs.getBoolean("manual"),
                rs.getBoolean("ab_test"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("created_at")),
                campaignCost,
                aiCost,
                videoCost,
                totalCost,
                revenue,
                rs.getLong("impressions"),
                rs.getLong("clicks"),
                rs.getLong("visitors"),
                rs.getLong("leads"),
                rs.getInt("checkout_clicks"),
                rs.getInt("purchases"),
                nullableLong(rs, "average_product_view_time_ms"),
                resultLabel(revenue, totalCost));
    }

    /** Monta o funil semanal completo a partir das metricas persistidas usadas no planejamento. */
    private List<CommercialPlanFunnelStageDto> buildFunnelStages(List<CommercialPlanWeekExperimentDto> experiments) {
        BigDecimal totalCost = sumCost(experiments);
        Instant lastEventAt = experiments.stream()
                .map(CommercialPlanWeekExperimentDto::createdAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        List<FunnelStageMetric> metrics = List.of(
                new FunnelStageMetric(
                        "AD_VIEW",
                        "Visualizacao do anuncio",
                        sumLong(experiments, CommercialPlanWeekExperimentDto::impressions),
                        true,
                        "experiment_campaign_metric.impressions / experiment_financial_metric.impressions"),
                new FunnelStageMetric(
                        "AD_CLICK",
                        "Clique no anuncio",
                        sumLong(experiments, CommercialPlanWeekExperimentDto::clicks),
                        true,
                        "experiment_campaign_metric.clicks / experiment_financial_metric.clicks"),
                new FunnelStageMetric(
                        "PRODUCT_ENTRY",
                        "Entrada no produto",
                        sumLong(experiments, CommercialPlanWeekExperimentDto::visitors),
                        true,
                        "experiment_financial_metric.visitors"),
                new FunnelStageMetric(
                        "LOGIN_OR_SIGNUP",
                        "Login ou criacao de conta",
                        null,
                        false,
                        "Sem fonte canonica persistida para a semana"),
                new FunnelStageMetric(
                        "OFFER_VIEW",
                        "Visualizacao da oferta",
                        null,
                        false,
                        "Sem fonte canonica persistida para a semana"),
                new FunnelStageMetric(
                        "CHECKOUT_CLICK",
                        "Clique no plano ou checkout",
                        sumInteger(experiments, CommercialPlanWeekExperimentDto::checkoutClicks),
                        true,
                        "experiment_financial_metric.checkout_clicks"),
                new FunnelStageMetric(
                        "APPROVED_PURCHASE",
                        "Assinatura ou compra aprovada",
                        sumInteger(experiments, CommercialPlanWeekExperimentDto::purchases),
                        true,
                        "experiment_financial_metric.purchases"),
                new FunnelStageMetric(
                        "ACCESS_GRANTED",
                        "Acesso liberado",
                        null,
                        false,
                        "Sem fonte canonica persistida para a semana"),
                new FunnelStageMetric(
                        "FIRST_USE",
                        "Primeiro uso ou ativacao",
                        null,
                        false,
                        "Sem fonte canonica persistida para a semana"));
        List<CommercialPlanFunnelStageDto> result = new ArrayList<>();
        Long previousActual = null;
        for (FunnelStageMetric metric : metrics) {
            BigDecimal conversion = metric.applicable()
                    ? conversionFromPrevious(metric.actualTotal(), previousActual)
                    : null;
            BigDecimal costPerConversion = metric.applicable()
                    ? costPerConversion(totalCost, metric.actualTotal())
                    : null;
            result.add(new CommercialPlanFunnelStageDto(
                    metric.code(),
                    metric.name(),
                    null,
                    metric.actualTotal(),
                    conversion,
                    costPerConversion,
                    metric.actualTotal(),
                    metric.actualTotal() != null && metric.actualTotal() > 0 ? lastEventAt : null,
                    metric.applicable(),
                    metric.evidenceSource()));
            if (metric.applicable()) {
                previousActual = metric.actualTotal();
            }
        }
        return result;
    }

    /** Soma valores long opcionais dos experimentos da semana. */
    private Long sumLong(
            List<CommercialPlanWeekExperimentDto> experiments,
            java.util.function.Function<CommercialPlanWeekExperimentDto, Long> extractor) {
        return experiments.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(0L, Long::sum);
    }

    /** Soma valores inteiros opcionais dos experimentos da semana. */
    private Long sumInteger(
            List<CommercialPlanWeekExperimentDto> experiments,
            java.util.function.Function<CommercialPlanWeekExperimentDto, Integer> extractor) {
        return experiments.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .map(Integer::longValue)
                .reduce(0L, Long::sum);
    }

    /** Calcula a conversao percentual contra a etapa anterior aplicavel. */
    private BigDecimal conversionFromPrevious(Long actual, Long previousActual) {
        if (actual == null || previousActual == null || previousActual <= 0) {
            return null;
        }
        return BigDecimal.valueOf(actual)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousActual), 2, RoundingMode.HALF_UP);
    }

    /** Calcula custo por conversao quando existe volume executado na etapa. */
    private BigDecimal costPerConversion(BigDecimal totalCost, Long actual) {
        if (actual == null || actual <= 0) {
            return null;
        }
        return money(totalCost.divide(BigDecimal.valueOf(actual), 2, RoundingMode.HALF_UP));
    }

    /** Soma custo total dos experimentos da semana. */
    private BigDecimal sumCost(List<CommercialPlanWeekExperimentDto> experiments) {
        return money(experiments.stream()
                .map(CommercialPlanWeekExperimentDto::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Soma receita dos experimentos da semana. */
    private BigDecimal sumRevenue(List<CommercialPlanWeekExperimentDto> experiments) {
        return money(experiments.stream()
                .map(CommercialPlanWeekExperimentDto::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Classifica o resultado financeiro rastreável do experimento. */
    private String resultLabel(BigDecimal revenue, BigDecimal totalCost) {
        if (revenue.compareTo(BigDecimal.ZERO) > 0 && revenue.compareTo(totalCost) >= 0) {
            return "Receita cobre custo";
        }
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            return "Receita parcial";
        }
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            return "Sem receita rastreada";
        }
        return "Sem custo rastreado";
    }

    /** Normaliza valores monetários para reais. */
    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Converte timestamp nulo ou preenchido para Instant. */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /** Lê um long opcional preservando nulo quando a agregação SQL não retornou valor. */
    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /** Representa o periodo de uma semana dentro do planejamento comercial. */
    private record WeekPeriod(LocalDate startDate, LocalDate endDate) {}

    /** Representa a metrica interna usada para montar uma etapa do funil semanal. */
    private record FunnelStageMetric(
            String code,
            String name,
            Long actualTotal,
            boolean applicable,
            String evidenceSource) {}
}
