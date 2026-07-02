package com.marketinghub.planning.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanMilestone;
import com.marketinghub.planning.CommercialPlanMilestoneStatus;
import com.marketinghub.planning.CommercialPlanRecommendation;
import com.marketinghub.planning.CommercialPlanSimulation;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.CommercialPlanType;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.dto.CreateCommercialPlanSimulationRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanMilestoneRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanMilestoneRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanSimulationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: coordenar cadastro, marcos e simulacoes de planos comerciais. */
@Service
public class CommercialPlanService {
    private static final List<DefaultMilestone> DEFAULT_MILESTONES = List.of(
            new DefaultMilestone("NICHE_APPROVED", "Nicho aprovado"),
            new DefaultMilestone("HYPOTHESIS_APPROVED", "Hipotese aprovada"),
            new DefaultMilestone("LOW_TICKET_OFFER_DEFINED", "Oferta low-ticket definida"),
            new DefaultMilestone("EXPERIMENT_CREATED", "Experimento criado"),
            new DefaultMilestone("CAMPAIGN_CREATED", "Campanha criada"),
            new DefaultMilestone("LANDING_APPROVED", "Landing aprovada"),
            new DefaultMilestone("PUBLICATION_VALIDATED", "Publicacao validada"),
            new DefaultMilestone("RESULT_ANALYZED", "Resultado analisado"),
            new DefaultMilestone("NEXT_DECISION_TAKEN", "Proxima decisao tomada"));

    private final CommercialPlanRepository planRepository;
    private final CommercialPlanMilestoneRepository milestoneRepository;
    private final CommercialPlanSimulationRepository simulationRepository;
    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final ExperimentRepository experimentRepository;
    private final CommercialPlanExecutionSyncService executionSyncService;

    public CommercialPlanService(
            CommercialPlanRepository planRepository,
            CommercialPlanMilestoneRepository milestoneRepository,
            CommercialPlanSimulationRepository simulationRepository,
            MarketNicheRepository nicheRepository,
            HypothesisRepository hypothesisRepository,
            ExperimentRepository experimentRepository,
            CommercialPlanExecutionSyncService executionSyncService) {
        this.planRepository = planRepository;
        this.milestoneRepository = milestoneRepository;
        this.simulationRepository = simulationRepository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.experimentRepository = experimentRepository;
        this.executionSyncService = executionSyncService;
    }

    /** Cria um plano de primeira venda e seus marcos comerciais padrao. */
    @Transactional
    public CommercialPlan create(CreateCommercialPlanRequest request) {
        validateRequired(request.name(), "name");
        CommercialPlan plan = CommercialPlan.builder()
                .name(request.name())
                .planType(CommercialPlanType.FIRST_SALE)
                .status(initialStatus(request))
                .niche(resolveNiche(request.nicheId()))
                .hypothesis(resolveHypothesis(request.hypothesisId()))
                .experiment(resolveExperiment(request.experimentId()))
                .commercialObjective(request.commercialObjective())
                .targetAudience(request.targetAudience())
                .mainPain(request.mainPain())
                .mainOffer(request.mainOffer())
                .mainLeadMagnet(request.mainLeadMagnet())
                .mainChannel(request.mainChannel())
                .mainMetric(request.mainMetric())
                .successCriteria(request.successCriteria())
                .stopCriteria(request.stopCriteria())
                .deadline(request.deadline())
                .maxBudget(request.maxBudget())
                .targetRevenue(request.targetRevenue())
                .operationalRevenueTarget(request.operationalRevenueTarget())
                .experimentsToCreate(request.experimentsToCreate())
                .experimentsToPublish(request.experimentsToPublish())
                .nextAction(request.nextAction())
                .currentBlocker(request.currentBlocker())
                .rootCause(request.rootCause())
                .build();
        CommercialPlan saved = planRepository.save(plan);
        createDefaultMilestones(saved);
        return syncExecution(saved);
    }

    /** Atualiza os campos comerciais e vinculos principais de um plano. */
    @Transactional
    public CommercialPlan update(Long id, UpdateCommercialPlanRequest request) {
        CommercialPlan plan = getPlan(id);
        validateRequired(request.name(), "name");
        plan.setName(request.name());
        plan.setStatus(request.status() != null ? request.status() : plan.getStatus());
        plan.setNiche(resolveNiche(request.nicheId()));
        plan.setHypothesis(resolveHypothesis(request.hypothesisId()));
        plan.setExperiment(resolveExperiment(request.experimentId()));
        plan.setCommercialObjective(request.commercialObjective());
        plan.setTargetAudience(request.targetAudience());
        plan.setMainPain(request.mainPain());
        plan.setMainOffer(request.mainOffer());
        plan.setMainLeadMagnet(request.mainLeadMagnet());
        plan.setMainChannel(request.mainChannel());
        plan.setMainMetric(request.mainMetric());
        plan.setSuccessCriteria(request.successCriteria());
        plan.setStopCriteria(request.stopCriteria());
        plan.setDeadline(request.deadline());
        plan.setMaxBudget(request.maxBudget());
        plan.setTargetRevenue(request.targetRevenue());
        plan.setOperationalRevenueTarget(request.operationalRevenueTarget());
        plan.setExperimentsToCreate(request.experimentsToCreate());
        plan.setExperimentsToPublish(request.experimentsToPublish());
        plan.setNextAction(request.nextAction());
        plan.setCurrentBlocker(request.currentBlocker());
        plan.setRootCause(request.rootCause());
        return syncExecution(planRepository.save(plan));
    }

    /** Lista planos comerciais, com filtro opcional por status. */
    @Transactional
    public List<CommercialPlan> list(CommercialPlanStatus status) {
        List<CommercialPlan> plans = status != null
                ? planRepository.findByStatusOrderByUpdatedAtDesc(status)
                : planRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        return plans.stream().map(this::syncExecution).toList();
    }

    /** Busca um plano comercial pelo identificador. */
    @Transactional
    public CommercialPlan getPlan(Long id) {
        CommercialPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano comercial nao encontrado: " + id));
        return syncExecution(plan);
    }

    /** Lista marcos de um plano na ordem comercial. */
    @Transactional(readOnly = true)
    public List<CommercialPlanMilestone> listMilestones(Long planId) {
        ensurePlanExists(planId);
        return milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(planId);
    }

    /** Lista simulacoes de um plano priorizando as mais recentes. */
    @Transactional(readOnly = true)
    public List<CommercialPlanSimulation> listSimulations(Long planId) {
        ensurePlanExists(planId);
        return simulationRepository.findByPlanIdOrderByCreatedAtDesc(planId);
    }

    /** Atualiza status, evidencia e proxima acao de um marco comercial. */
    @Transactional
    public CommercialPlanMilestone updateMilestone(
            Long planId,
            Long milestoneId,
            UpdateCommercialPlanMilestoneRequest request) {
        CommercialPlanMilestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Marco comercial nao encontrado: " + milestoneId));
        if (!milestone.getPlan().getId().equals(planId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marco nao pertence ao plano informado.");
        }
        milestone.setStatus(request.status() != null ? request.status() : milestone.getStatus());
        milestone.setDueDate(request.dueDate());
        milestone.setTargetCost(request.targetCost());
        milestone.setTargetRevenue(request.targetRevenue());
        milestone.setExperimentsToCreate(request.experimentsToCreate());
        milestone.setExperimentsToPublish(request.experimentsToPublish());
        milestone.setEvidenceSource(request.evidenceSource());
        milestone.setBlocker(request.blocker());
        milestone.setRecommendedNextAction(request.recommendedNextAction());
        syncPlanBlockerFromMilestones(milestone.getPlan());
        milestoneRepository.save(milestone);
        syncExecution(milestone.getPlan());
        return milestone;
    }

    /** Gera e persiste uma simulacao manual assistida com base no estado atual do plano. */
    @Transactional
    public CommercialPlanSimulation simulate(Long planId, CreateCommercialPlanSimulationRequest request) {
        CommercialPlan plan = getPlan(planId);
        List<CommercialPlanMilestone> milestones = milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(planId);
        CommercialPlanSimulation simulation = buildSimulation(plan, milestones, request.decisionNotes());
        CommercialPlanSimulation saved = simulationRepository.save(simulation);
        plan.setMostLikelyScenario(saved.getMostLikelyScenario());
        plan.setMainFutureRisk(saved.getMainRisk());
        plan.setActionToAvoid(saved.getActionToAvoid());
        plan.setNextAction(saved.getBestNextAction());
        planRepository.save(plan);
        return saved;
    }

    /** Cria os marcos padrao recomendados pelo planejamento comercial. */
    private void createDefaultMilestones(CommercialPlan plan) {
        for (int index = 0; index < DEFAULT_MILESTONES.size(); index++) {
            DefaultMilestone defaultMilestone = DEFAULT_MILESTONES.get(index);
            milestoneRepository.save(CommercialPlanMilestone.builder()
                    .plan(plan)
                    .sequenceOrder(index + 1)
                    .code(defaultMilestone.code())
                    .name(defaultMilestone.name())
                    .status(initialMilestoneStatus(index, plan))
                    .recommendedNextAction(defaultMilestone.name())
                    .build());
        }
    }

    /** Sincroniza os valores executados do plano e dos marcos antes de expor o estado comercial. */
    private CommercialPlan syncExecution(CommercialPlan plan) {
        List<CommercialPlanMilestone> milestones = milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(plan.getId());
        if (milestones == null) {
            milestones = List.of();
        }
        executionSyncService.sync(plan, milestones);
        milestoneRepository.saveAll(milestones);
        planRepository.save(plan);
        return plan;
    }

    /** Define o status inicial do plano conforme seus campos obrigatorios de foco comercial. */
    private CommercialPlanStatus initialStatus(CreateCommercialPlanRequest request) {
        if (hasCriticalPlanningGap(
                request.commercialObjective(),
                request.targetAudience(),
                request.mainPain(),
                request.mainOffer(),
                request.mainMetric(),
                request.successCriteria(),
                request.stopCriteria(),
                request.deadline())) {
            return CommercialPlanStatus.BLOCKED;
        }
        return CommercialPlanStatus.DRAFT;
    }

    /** Define o status inicial de cada marco padrao. */
    private CommercialPlanMilestoneStatus initialMilestoneStatus(int index, CommercialPlan plan) {
        if (index == 0 && plan.getNiche() != null) {
            return CommercialPlanMilestoneStatus.DONE;
        }
        if (index == 1 && plan.getHypothesis() != null) {
            return CommercialPlanMilestoneStatus.DONE;
        }
        if (index == 3 && plan.getExperiment() != null) {
            return CommercialPlanMilestoneStatus.DONE;
        }
        return CommercialPlanMilestoneStatus.PENDING;
    }

    /** Sincroniza o bloqueio do plano quando algum marco fica bloqueado. */
    private void syncPlanBlockerFromMilestones(CommercialPlan plan) {
        milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(plan.getId()).stream()
                .filter(milestone -> milestone.getStatus() == CommercialPlanMilestoneStatus.BLOCKED)
                .findFirst()
                .ifPresent(blocked -> {
                    plan.setStatus(CommercialPlanStatus.BLOCKED);
                    plan.setCurrentBlocker(blocked.getBlocker());
                    plan.setNextAction(blocked.getRecommendedNextAction());
                    planRepository.save(plan);
                });
    }

    /** Monta uma simulacao objetiva para reduzir esforco sem evidencia comercial. */
    private CommercialPlanSimulation buildSimulation(
            CommercialPlan plan,
            List<CommercialPlanMilestone> milestones,
            String decisionNotes) {
        boolean missingCommercialGate = hasCriticalPlanningGap(
                plan.getCommercialObjective(),
                plan.getTargetAudience(),
                plan.getMainPain(),
                plan.getMainOffer(),
                plan.getMainMetric(),
                plan.getSuccessCriteria(),
                plan.getStopCriteria(),
                plan.getDeadline());
        boolean hasBlockedMilestone = milestones.stream()
                .anyMatch(milestone -> milestone.getStatus() == CommercialPlanMilestoneStatus.BLOCKED);
        CommercialPlanRecommendation recommendation =
                missingCommercialGate || hasBlockedMilestone ? CommercialPlanRecommendation.CORRECT : CommercialPlanRecommendation.CONTINUE;
        String blocker = firstText(plan.getCurrentBlocker(), "criterio comercial incompleto ou gargalo ainda nao isolado");
        return CommercialPlanSimulation.builder()
                .plan(plan)
                .recommendation(recommendation)
                .mostLikelyScenario(missingCommercialGate
                        ? "O plano tende a gerar atividade operacional sem evidencia clara de venda."
                        : "O plano deve gerar evidencia comercial limitada se a proxima acao for pequena e mensuravel.")
                .bestRealisticScenario("A proxima acao valida uma dor, oferta ou friccao do funil sem aumentar complexidade.")
                .worstLikelyScenario("Mais artefatos ou midia sao criados sem corrigir o gargalo real.")
                .mainRisk(hasBlockedMilestone ? blocker : "Interpretar baixo resultado como problema de mercado antes de validar oferta, formulario e mensuracao.")
                .bestNextAction(bestNextAction(plan, missingCommercialGate, blocker))
                .actionToAvoid("Criar nova campanha, nova landing ou novo criativo antes de fechar o criterio de decisao e o gargalo atual.")
                .continueCondition(firstText(plan.getSuccessCriteria(), "existir sinal comercial forte dentro do prazo definido"))
                .stopCondition(firstText(plan.getStopCriteria(), "atingir limite de acessos, gasto ou prazo sem nova evidencia comercial"))
                .evidence7Days("Validar se a proxima acao removeu o principal bloqueio comercial.")
                .evidence14Days("Comparar metricas do plano com o criterio de sucesso e parada.")
                .evidence30Days("Decidir continuidade, correcao, pausa ou encerramento com base em evidencia persistida.")
                .decisionNotes(decisionNotes)
                .build();
    }

    /** Escolhe a proxima acao com maior impacto comercial imediato. */
    private String bestNextAction(CommercialPlan plan, boolean missingCommercialGate, String blocker) {
        if (missingCommercialGate) {
            return "Completar objetivo, publico, dor, oferta, metrica, prazo e criterios antes de executar pipeline.";
        }
        if (plan.getCurrentBlocker() != null && !plan.getCurrentBlocker().isBlank()) {
            return "Corrigir causa-raiz do bloqueio atual: " + blocker;
        }
        return firstText(plan.getNextAction(), "executar o menor teste capaz de gerar evidencia de compra ou intencao clara");
    }

    /** Verifica se o plano perdeu algum gate minimo de foco em venda. */
    private boolean hasCriticalPlanningGap(
            String objective,
            String audience,
            String pain,
            String offer,
            String metric,
            String successCriteria,
            String stopCriteria,
            LocalDate deadline) {
        return isBlank(objective)
                || isBlank(audience)
                || isBlank(pain)
                || isBlank(offer)
                || isBlank(metric)
                || isBlank(successCriteria)
                || isBlank(stopCriteria)
                || deadline == null;
    }

    /** Busca o nicho vinculado quando informado. */
    private MarketNiche resolveNiche(Long nicheId) {
        if (nicheId == null) {
            return null;
        }
        return nicheRepository.findById(nicheId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho nao encontrado: " + nicheId));
    }

    /** Busca a hipotese vinculada quando informada. */
    private Hypothesis resolveHypothesis(UUID hypothesisId) {
        if (hypothesisId == null) {
            return null;
        }
        return hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hipotese nao encontrada: " + hypothesisId));
    }

    /** Busca o experimento vinculado quando informado. */
    private Experiment resolveExperiment(Long experimentId) {
        if (experimentId == null) {
            return null;
        }
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento nao encontrado: " + experimentId));
    }

    /** Garante que o plano existe antes de consultar dados filhos. */
    private void ensurePlanExists(Long planId) {
        if (!planRepository.existsById(planId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano comercial nao encontrado: " + planId);
        }
    }

    /** Valida texto obrigatorio em contratos de criacao e alteracao. */
    private void validateRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " e obrigatorio");
        }
    }

    /** Retorna verdadeiro quando texto esta vazio. */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Retorna o primeiro texto preenchido ou um texto padrao. */
    private String firstText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    /** Responsabilidade: carregar a definicao fixa de um marco comercial padrao. */
    private record DefaultMilestone(String code, String name) {}
}
