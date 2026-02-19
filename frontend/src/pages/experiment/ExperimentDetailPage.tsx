import { Fragment, useEffect, useMemo, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useExperimentDiagnostics } from "../../api/experiment/useExperimentDiagnostics";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import CriativosTab from "./CriativosTab";
import InstantFormsTab from "./InstantFormsTab";
import EmailsTab from "./EmailsTab";
import SampleEmailsTab from "./SampleEmailsTab";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import { useJourneyTemplate } from "../../api/journey/useJourneyTemplate";
import { useExperimentJourneyAssignments } from "../../api/experiment/useExperimentJourneyAssignments";
import { useRebuildExperimentJourney } from "../../api/experiment/useRebuildExperimentJourney";
import { useUpdateExperimentStatus } from "../../api/experiment/useUpdateExperimentStatus";
import { useExperimentFacebookCampaigns } from "../../api/experiment/useExperimentFacebookCampaigns";
import {
  useExperimentCampaignReset,
  useExperimentCampaignResetPreview,
  type ExperimentCampaignResetSummary,
} from "../../api/experiment/useExperimentCampaignReset";
import type { JourneyAssignment, JourneyStep } from "../../api/journey/types";
import DeliverablesTab from "./DeliverablesTab";
import LeadPortalFlowTab from "./LeadPortalFlowTab";
import TargetingTab from "./TargetingTab";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import { useExperimentAdSetWorkflow } from "../../api/experiment/useExperimentAdSetWorkflow";
import type { TargetingElementType } from "../../api/targeting/types";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const {
    data: diagnostics,
    isLoading: isLoadingDiagnostics,
    dataUpdatedAt: diagnosticsUpdatedAt,
  } = useExperimentDiagnostics(expId);
  const { data: niche } = useNiche(data?.nicheId ?? 0);
  const { data: hyp } = useHypothesis(
    data ? String(data.nicheId) : undefined,
    data ? String(data.hypothesisId) : undefined,
  );
  const hypothesisId = data?.hypothesisId;
  const nicheIdParam = data?.nicheId != null ? String(data.nicheId) : undefined;
  const { data: presets } = useMetricPresets();
  const [tab, setTab] = useState("overview");
  const [journeyError, setJourneyError] = useState<string | null>(null);
  const { data: facebookConfig, isLoading: isLoadingFacebookConfig } =
    useFacebookConfigurationStatus();
  const { data: journeyAssignments, isLoading: isLoadingJourneyAssignments } =
    useExperimentJourneyAssignments(expId);
  const { data: template } = useJourneyTemplate(
    data?.journeyTemplateId ?? undefined,
  );
  const rebuildJourney = useRebuildExperimentJourney(expId);
  const updateExperimentStatus = useUpdateExperimentStatus(expId);
  const isUpdatingStatus = updateExperimentStatus.isPending;
  const { data: targetingElements, isLoading: isLoadingTargeting } =
    useTargetingElementsByNiche(nicheIdParam);
  const { data: adSetWorkflow, isLoading: isLoadingAdSetWorkflow } =
    useExperimentAdSetWorkflow(expId);
  const {
    data: facebookCampaigns,
    isLoading: isLoadingFacebookCampaigns,
  } = useExperimentFacebookCampaigns(expId);
  const [isResetModalOpen, setIsResetModalOpen] = useState(false);
  const {
    data: resetPreviewData,
    isFetching: isFetchingResetPreview,
    isError: isResetPreviewError,
    error: resetPreviewError,
    refetch: refetchResetPreview,
  } = useExperimentCampaignResetPreview(expId);
  const resetCampaigns = useExperimentCampaignReset(expId);
  const hypothesisIdAsString = hypothesisId ? String(hypothesisId) : undefined;
  const hasCompleteTargeting = useMemo(() => {
    if (!Array.isArray(targetingElements)) return false;
    const requiredTypes: TargetingElementType[] = [
      "INTEREST",
      "JOB_TITLE",
      "BEHAVIOR",
    ];
    return requiredTypes.every((type) =>
      targetingElements.some(
        (element) =>
          element.type === type &&
          element.status === "APPROVED" &&
          (!element.hypothesisId ||
            element.hypothesisId === hypothesisIdAsString),
      ),
    );
  }, [targetingElements, hypothesisIdAsString]);
  useBreadcrumbs([
    {
      label: niche?.name || "...",
      to: `/niches/${data?.nicheId}`,
      icon: nicheIcon,
    },
    {
      label: hyp?.title || "...",
      to: `/niches/${data?.nicheId}/hypotheses/${data?.hypothesisId}`,
      icon: hypothesisIcon,
    },
    { label: data?.name || "...", icon: experimentIcon },
  ]);
  const templateSteps = template?.steps ?? [];
  const hasInstantFormSteps = templateSteps.some(
    (step) => step.stimulusType === "INSTANT_FORM",
  );
  const hasEmailSteps = templateSteps.some(
    (step) => step.stimulusType === "EMAIL",
  );

  useEffect(() => {
    if (tab === "instant-form" && !hasInstantFormSteps) {
      setTab("overview");
    }
    if (tab === "emails" && !hasEmailSteps) {
      setTab("overview");
    }
  }, [tab, hasInstantFormSteps, hasEmailSteps]);
  const assignmentsWithSteps = useMemo(() => {
    const assignments = journeyAssignments?.assignments ?? [];
    if (assignments.length === 0) {
      return [] as { assignment: JourneyAssignment; step?: JourneyStep }[];
    }
    const stepIndex = new Map<number, JourneyStep>(
      templateSteps.map((step) => [step.id, step]),
    );
    const pairs = assignments.map((assignment) => ({
      assignment,
      step: assignment.nextStepId
        ? stepIndex.get(assignment.nextStepId)
        : undefined,
    }));
    pairs.sort((a, b) => {
      const posA = a.step?.position ?? Number.MAX_SAFE_INTEGER;
      const posB = b.step?.position ?? Number.MAX_SAFE_INTEGER;
      if (posA !== posB) return posA - posB;
      return a.assignment.id - b.assignment.id;
    });
    return pairs;
  }, [journeyAssignments?.assignments, templateSteps]);

  const openResetModal = () => setIsResetModalOpen(true);

  useEffect(() => {
    if (!isResetModalOpen) return;
    void refetchResetPreview();
  }, [isResetModalOpen, refetchResetPreview]);

  const closeResetModal = () => {
    if (resetCampaigns.isPending) return;
    setIsResetModalOpen(false);
  };

  const handleConfirmReset = async () => {
    try {
      const summary = await resetCampaigns.mutateAsync();
      if (summary.campaigns > 0) {
        toast.success(
          `Reset concluído: ${summary.campaigns} campanha(s), ${summary.adSets} conjunto(s), ${summary.ads} anúncio(s) e ${summary.creatives} criativo(s) foram removidos.`,
        );
      } else {
        toast.info("Não havia campanhas pendentes para remover.");
      }
      setIsResetModalOpen(false);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message ?? error.response?.data?.detail ??
          "Não foi possível resetar as campanhas pendentes."
        : "Não foi possível resetar as campanhas pendentes.";
      toast.error(message);
    }
  };

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const preset = presets?.find((p) => p.id === data.metricPresetId);
  const resetPreviewSummary: ExperimentCampaignResetSummary =
    resetPreviewData ?? { campaigns: 0, adSets: 0, ads: 0, creatives: 0 };
  const totalItemsToReset =
    resetPreviewSummary.campaigns +
    resetPreviewSummary.adSets +
    resetPreviewSummary.ads +
    resetPreviewSummary.creatives;
  const hasItemsToReset = totalItemsToReset > 0;
  const previewErrorMessage = isResetPreviewError
    ? resetPreviewError instanceof Error
      ? resetPreviewError.message
      : "Não foi possível carregar a prévia do reset."
    : null;
  const confirmResetDisabled =
    isFetchingResetPreview || !hasItemsToReset || Boolean(previewErrorMessage) || resetCampaigns.isPending;
  const formatCurrency = (n?: number | null) =>
    n != null
      ? new Intl.NumberFormat("pt-BR", {
          style: "currency",
          currency: "BRL",
        }).format(n)
      : "—";
  const formatPercent = (n?: number | null) => (n != null ? `${n}%` : "—");
  const formatDateTimeValue = (value?: string | null) => {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    });
  };
  const diagnosticsTimestamp =
    diagnosticsUpdatedAt > 0
      ? formatDateTimeValue(new Date(diagnosticsUpdatedAt).toISOString())
      : null;
  const baseKpi = data.kpiTarget ?? data.kpiTargetCpl;
  const stopLossFactor = preset?.stopLossFactor;
  const stopLossCpl =
    data.stopLossCpl ??
    (baseKpi != null && stopLossFactor != null
      ? baseKpi * stopLossFactor
      : null);
  const hasConfiguredFacebookPage = facebookConfig?.hasConfiguredPages ?? false;
  const experimentPage = data.facebookPage;
  const hasExperimentPage = Boolean(experimentPage?.pageId);
  const experimentInstantForm = data.facebookInstantForm;
  const instagramAccount = data.instagramAccount;
  const hasInstagramAccount = Boolean(instagramAccount);
  const readinessChecks = [
    {
      id: "facebook-page",
      title: "Página do Facebook configurada",
      isMet: hasConfiguredFacebookPage,
      hint: hasConfiguredFacebookPage
        ? "Já existe ao menos uma página configurada para publicar campanhas."
        : isLoadingFacebookConfig
          ? "Verificando páginas configuradas..."
          : "Cadastre e relacione uma página do Facebook na tela Contas do Facebook.",
      action:
        !isLoadingFacebookConfig && !hasConfiguredFacebookPage
          ? () => navigate("/accounts/facebook")
          : undefined,
      actionLabel:
        !isLoadingFacebookConfig && !hasConfiguredFacebookPage
          ? "Abrir Contas do Facebook"
          : undefined,
    },
    ...(hasInstantFormSteps
      ? [
          {
            id: "instant-form",
            title: "Instant form vinculado",
            isMet: Boolean(experimentInstantForm),
            hint: experimentInstantForm
              ? `O formulário ${experimentInstantForm.name}${experimentInstantForm.facebookFormId ? ` (${experimentInstantForm.facebookFormId})` : ""} será usado na captura.`
              : "Associe um instant form compatível na aba Instant Forms para destravar a etapa de captura.",
            action: experimentInstantForm
              ? undefined
              : () => setTab("instant-form"),
            actionLabel: experimentInstantForm
              ? undefined
              : "Ir para Instant Forms",
          },
        ]
      : []),
    {
      id: "instagram-account",
      title: "Conta de Instagram vinculada",
      isMet: hasInstagramAccount,
      hint: hasInstagramAccount
        ? `Este experimento usa a conta ${instagramAccount?.handle}.`
        : "Associe uma conta do Instagram ao experimento para liberar as campanhas.",
      action: hasInstagramAccount
        ? undefined
        : () => navigate(`/experiments/${expId}/edit`),
      actionLabel: hasInstagramAccount ? undefined : "Editar experimento",
    },
    {
      id: "experiment-page",
      title: "Página definida no experimento",
      isMet: hasExperimentPage,
      hint: hasExperimentPage
        ? `Este experimento usa a página ${experimentPage?.name ?? experimentPage?.pageId}.`
        : "Defina a página na edição do experimento para garantir que os anúncios publiquem no local correto. A edição deve ser feita no experimento.",
      action: hasExperimentPage
        ? undefined
        : () => navigate(`/experiments/${expId}/edit`),
      actionLabel: hasExperimentPage ? undefined : "Editar experimento",
    },
    {
      id: "approved-targeting",
      title: "Segmentação Meta aprovada",
      isMet: hasCompleteTargeting,
      hint: isLoadingTargeting
        ? "Verificando elementos aprovados..."
        : hasCompleteTargeting
          ? "Já existem interesses, cargos e comportamentos aprovados para este nicho/hipótese."
          : "Aprove ao menos um interesse, cargo e comportamento na aba Segmentação para liberar a campanha.",
      action: hasCompleteTargeting ? undefined : () => setTab("targeting"),
      actionLabel: hasCompleteTargeting ? undefined : "Ir para Segmentação",
    },
    {
      id: "platform",
      title: "Plataforma configurada para Facebook Ads",
      isMet: data.platform === "FACEBOOK",
      hint:
        data.platform === "FACEBOOK"
          ? "Este experimento já usa a plataforma do Facebook."
          : `Plataforma atual: ${data.platform}. Ajuste para Facebook Ads para liberar a campanha.`,
      actionLabel: undefined,
    },
    {
      id: "status",
      title: "Status marcado como Planejado",
      isMet: data.status === "PLANNED",
      hint:
        data.status === "PLANNED"
          ? "O worker poderá buscar este experimento quando os demais itens estiverem prontos."
          : "Altere o status para Planejado na lista de experimentos para liberar o worker de Facebook.",
      action:
        data.status === "PLANNED"
          ? undefined
          : () => updateExperimentStatus.mutate("PLANNED"),
      actionLabel:
        data.status === "PLANNED" ? undefined : "Marcar como Planejado",
      actionDisabled: isUpdatingStatus,
      actionLoading: isUpdatingStatus,
    },
    {
      id: "creatives",
      title: "Criativos aprovados",
      isMet: data.creativeApproved,
      hint: data.creativeApproved
        ? "Os criativos já estão aprovados."
        : "Revise e aprove pelo menos um criativo na aba Criativos.",
      action: data.creativeApproved ? undefined : () => setTab("creatives"),
      actionLabel: "Ir para Criativos",
    },
  ];
  const isReadyForFacebook = readinessChecks.every((c) => c.isMet);
  const diagnosticsVariant: Record<string, string> = {
    ERROR: "danger",
    WARNING: "warning",
    INFO: "secondary",
  };

  const workflowStatusVariant: Record<string, string> = {
    NOT_STARTED: "secondary",
    RUNNING: "info",
    COMPLETED: "success",
    FAILED: "danger",
  };
  const workflowStatusLabel: Record<string, string> = {
    NOT_STARTED: "Não iniciado",
    RUNNING: "Em andamento",
    COMPLETED: "Concluído",
    FAILED: "Com erro",
  };

  const selectedEmailOverview = data.selectedSampleEmailSubject ? (
    <div className="d-flex flex-column">
      <span>{data.selectedSampleEmailSubject}</span>
      {data.selectedSampleEmailUpdatedAt ? (
        <span className="text-muted small">
          Atualizado em {formatDateTimeValue(data.selectedSampleEmailUpdatedAt)}
        </span>
      ) : null}
      <button
        type="button"
        className="btn btn-link btn-sm p-0 mt-1 align-self-start"
        onClick={() => setTab("sample-emails")}
      >
        Ver e-mails
      </button>
    </div>
  ) : (
    <button
      type="button"
      className="btn btn-link btn-sm p-0"
      onClick={() => setTab("sample-emails")}
    >
      Escolher e-mail
    </button>
  );

  const rows = [
    {
      label: "Nicho",
      value: <Link to={`/niches/${data.nicheId}/edit`}>{niche?.name}</Link>,
    },
    {
      label: "Hipótese",
      value: (
        <Link to={`/niches/${data.nicheId}/hypotheses/${data.hypothesisId}`}>
          {hyp?.title || data.hypothesis}
        </Link>
      ),
    },
    {
      label: "Página do Facebook",
      value: experimentPage
        ? `${experimentPage.name} (${experimentPage.pageId})`
        : "—",
    },
    {
      label: "Conta do Instagram",
      value: instagramAccount
        ? `${instagramAccount.name} (${instagramAccount.handle})`
        : "—",
    },
    ...(data.journeyTemplateName
      ? [
          {
            label: "Template de Jornada",
            value: data.journeyTemplateId ? (
              <Link
                to={`/journey-templates/${data.journeyTemplateId}`}
                className="btn btn-link p-0 align-baseline"
              >
                {data.journeyTemplateName}
              </Link>
            ) : (
              data.journeyTemplateName
            ),
          },
        ]
      : []),
    { label: "Preset de Métricas", value: preset?.name || "—" },
    {
      label: "Sample size",
      value: data.sampleSize ?? preset?.sampleSize ?? "—",
    },
    { label: "Criativos a gerar", value: data.creativesToGenerate ?? "—" },
    { label: "E-mails a gerar", value: data.emailsToGenerate ?? "—" },
    {
      label: "E-mails de amostra a gerar",
      value: data.sampleEmailsToGenerate ?? "—",
    },
    {
      label: "E-mail de amostra selecionado",
      value: selectedEmailOverview,
    },
    {
      label: "Fluxo de portal do lead",
      value: data.leadPortalFlowName ? (
        <div className="d-flex flex-column">
          <span>{data.leadPortalFlowName}</span>
          {data.leadPortalFlowSlug ? (
            <span className="text-muted small">
              Slug: {data.leadPortalFlowSlug}
            </span>
          ) : null}
        </div>
      ) : (
        "—"
      ),
    },
    {
      label: "Modelo de imagem",
      value: data.imageModelName
        ? `${data.imageModelName}${data.imageModelQualityName ? " · " + data.imageModelQualityName : ""}`
        : "—",
    },
    {
      label: "MDE (p.p.)",
      value: data.mdePercent ?? preset?.defaultMdePp ?? "—",
    },
    {
      label: "Stop-loss factor",
      value: preset?.stopLossFactor ? `${preset.stopLossFactor}×` : "—",
    },
    {
      label: "CPL-meta",
      value: formatCurrency(data.kpiTarget ?? data.kpiTargetCpl),
    },
    { label: "Custo", value: formatCurrency(data.cost) },
    { label: "Despesa", value: formatCurrency(data.expense) },
    { label: "Stop-loss CPL", value: formatCurrency(stopLossCpl) },
    { label: "Baseline CVR", value: formatPercent(data.baselineCvr) },
    { label: "Target CVR", value: formatPercent(data.targetCvr) },
    { label: "Plataforma", value: data.platform },
    { label: "Início", value: data.startDate },
    { label: "Término", value: data.endDate },
  ];
  const handleCreateJourney = async () => {
    setJourneyError(null);
    try {
      await rebuildJourney.mutateAsync();
    } catch (error) {
      console.error("Failed to rebuild journey assignments", error);
      setJourneyError("Não foi possível criar a jornada. Tente novamente.");
    }
  };

  const isJourneyActionDisabled =
    rebuildJourney.isPending || !data?.journeyTemplateId || isLoading;
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <PageTitle icon={experimentIcon}>{data.name}</PageTitle>
          <p className="text-muted mb-0">{data.hypothesis}</p>
        </div>
        <div className="d-flex align-items-center">
          <button
            type="button"
            className="btn btn-primary me-2"
            onClick={handleCreateJourney}
            disabled={isJourneyActionDisabled}
          >
            {rebuildJourney.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                />
                Criando jornada...
              </>
            ) : (
              "Criar jornada"
            )}
          </button>
          <Link to="edit" className="btn btn-outline-secondary me-2">
            Editar
          </Link>
          <Link to="adset-workflow" className="btn btn-outline-primary me-2">
            Playbook de Ad Sets
          </Link>
          <Link to="facebook-api-logs" className="btn btn-outline-info me-2">
            Chamadas Meta
          </Link>
          <button
            type="button"
            className="btn btn-outline-danger me-2"
            onClick={openResetModal}
            disabled={resetCampaigns.isPending}
          >
            {resetCampaigns.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
                Resetando...
              </>
            ) : (
              "Resetar pendências"
            )}
          </button>
          <span className="badge bg-secondary">{data.status}</span>
        </div>
      </div>
      {isLoadingDiagnostics ? (
        <div
          className="alert alert-light d-flex align-items-center gap-2 mt-3"
          role="status"
        >
          <span
            className="spinner-border spinner-border-sm"
            role="status"
            aria-hidden="true"
          />
          <span>Carregando diagnóstico da publicação...</span>
        </div>
      ) : diagnostics ? (
        <div
          className={`alert alert-${diagnosticsVariant[diagnostics.severity] ?? "secondary"} mt-3`}
          role="alert"
        >
          <div className="d-flex justify-content-between gap-3">
            <div>
              <h6 className="alert-heading mb-1">{diagnostics.headline}</h6>
              {diagnosticsTimestamp ? (
                <p className="mb-2 small text-body-secondary">
                  <strong>Data e hora da mensagem:</strong>{" "}
                  {diagnosticsTimestamp}
                </p>
              ) : null}
              <p className="mb-2">{diagnostics.description}</p>
              {diagnostics.resolution ? (
                <p className="mb-2">
                  <strong>O que deve ser corrigido:</strong>{" "}
                  {diagnostics.resolution}
                </p>
              ) : null}
            </div>
            <span
              className={`badge text-bg-${diagnosticsVariant[diagnostics.severity] ?? "secondary"} align-self-start`}
            >
              {diagnostics.severity}
            </span>
          </div>
          {diagnostics.artifacts.length > 0 ? (
            <>
              <p className="mb-2 mt-2 small">
                <strong>Itens com pendência para corrigir:</strong>
              </p>
              <ul className="mb-0 small ps-3">
                {diagnostics.artifacts.map((artifact) => (
                  <li key={`${artifact.type}-${artifact.id}`}>
                    <strong>{artifact.type}</strong> ·{" "}
                    {artifact.name || artifact.id} — ID interno {artifact.id},
                    ID Meta: {artifact.externalId ?? "—"}
                  </li>
                ))}
              </ul>
            </>
          ) : null}
        </div>
      ) : null}
      <div className="card border-0 shadow-sm rounded-3 mt-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <h5 className="card-title mb-0">Campanha de Facebook Ads</h5>
            <span
              className={`badge ${
                isReadyForFacebook ? "text-bg-success" : "text-bg-warning"
              }`}
            >
              {isReadyForFacebook ? "Pronto" : "Pendente"}
            </span>
          </div>
          <p className="card-text mt-2">
            {isReadyForFacebook
              ? "Este experimento já atende aos requisitos mínimos para virar uma campanha no Facebook Ads quando o worker executar."
              : "Para liberar este experimento para campanha no Facebook Ads, resolva os itens abaixo."}
          </p>
          <ul className="list-unstyled mb-0 d-flex flex-column gap-2">
            {readinessChecks.map((check) => (
              <li
                key={check.id}
                className="d-flex align-items-start gap-3 p-3 bg-body-tertiary rounded-3"
              >
                <span
                  className={`badge flex-shrink-0 ${
                    check.isMet ? "text-bg-success" : "text-bg-warning"
                  }`}
                >
                  {check.isMet ? "Pronto" : "Pendente"}
                </span>
                <div className="flex-grow-1">
                  <div className="fw-semibold text-body">{check.title}</div>
                  {check.hint ? (
                    <div className="text-muted small mt-1">{check.hint}</div>
                  ) : null}
                  {!check.isMet && check.action ? (
                    <button
                      type="button"
                      className="btn btn-link btn-sm p-0 align-baseline mt-2"
                      onClick={check.action}
                      disabled={check.actionDisabled}
                    >
                      {check.actionLoading ? (
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          role="status"
                        />
                      ) : null}
                      {check.actionLabel}
                    </button>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
          <div className="mt-4">
            <h6 className="text-uppercase small text-muted">Execução registrada</h6>
            {isLoadingFacebookCampaigns ? (
              <p className="text-muted small mb-0">Carregando campanhas publicadas...</p>
            ) : !facebookCampaigns?.length ? (
              <p className="text-muted small mb-0">Nenhuma publicação registrada para este experimento.</p>
            ) : (
              <div className="d-flex flex-column gap-3">
                {facebookCampaigns.map((campaign) => (
                  <div key={campaign.id} className="border rounded-3 p-3">
                    <div className="d-flex justify-content-between flex-wrap gap-2">
                      <div>
                        <div className="fw-semibold">{campaign.name}</div>
                        <div className="text-muted small">
                          Objetivo: {campaign.objective ?? "—"} · Criada em {formatDateTimeValue(campaign.createdAt)}
                        </div>
                      </div>
                      <span className={`badge text-bg-${campaign.status === "PAUSED" ? "secondary" : "success"}`}>{campaign.status}</span>
                    </div>
                    {campaign.issues?.length ? (
                      <div className="alert alert-warning mt-3 mb-2" role="alert">
                        <ul className="mb-0 ps-3">
                          {campaign.issues.map((issue) => (
                            <li key={`${campaign.id}-${issue}`}>{issue}</li>
                          ))}
                        </ul>
                      </div>
                    ) : null}
                    {campaign.adSets.length ? (
                      <div className="d-flex flex-column gap-3 mt-3">
                        {campaign.adSets.map((adSet) => (
                          <div key={adSet.id} className="border rounded-3 p-3 bg-body-tertiary">
                            <div className="d-flex justify-content-between flex-wrap gap-2">
                              <div>
                                <div className="fw-semibold">{adSet.name}</div>
                                <div className="text-muted small">
                                  {adSet.experimentAdSetId
                                    ? `Segmento aprovado #${adSet.experimentAdSetId}`
                                    : "Sem vínculo com segmento aprovado"}
                                  · {adSet.ads.length} anúncio{adSet.ads.length === 1 ? "" : "s"}
                                </div>
                              </div>
                              <span className={`badge text-bg-${adSet.experimentAdSetId ? "success" : "warning"}`}>{adSet.status}</span>
                            </div>
                            {adSet.issues?.length ? (
                              <ul className="text-warning small mb-0 mt-2 ps-3">
                                {adSet.issues.map((issue) => (
                                  <li key={`${adSet.id}-${issue}`}>{issue}</li>
                                ))}
                              </ul>
                            ) : null}
                            <div className="small text-muted mt-2">
                              {adSet.ads.length
                                ? `Anúncios: ${adSet.ads.map((ad) => ad.name).join(", ")}`
                                : "Nenhum anúncio publicado neste conjunto."}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-muted small mb-0 mt-3">Nenhum conjunto de anúncios registrado.</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      <div className="card border-0 shadow-sm rounded-3 mt-3">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h5 className="card-title mb-1">Pipeline de Públicos</h5>
              <p className="text-muted small mb-0">
                Geração automatizada de segmentações validadas com a Meta Ads
              </p>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <Link to="adset-workflow" className="btn btn-outline-primary btn-sm">
                Ver playbook
              </Link>
              <Link to="facebook-api-logs" className="btn btn-outline-secondary btn-sm">
                Logs da Graph API
              </Link>
            </div>
          </div>
          {isLoadingAdSetWorkflow ? (
            <p className="text-muted small mt-3 mb-0">Carregando pipeline...</p>
          ) : adSetWorkflow ? (
            <>
              <div className="d-flex flex-wrap gap-3 align-items-center mt-3">
                <span className={`badge text-bg-${workflowStatusVariant[adSetWorkflow.status ?? "NOT_STARTED"] ?? "secondary"}`}>
                  {workflowStatusLabel[adSetWorkflow.status ?? "NOT_STARTED"]}
                </span>
                <div className="small text-muted">
                  Specs prontas: {adSetWorkflow.specs?.filter((spec) => spec.reachStatus?.toUpperCase() === "READY").length ?? 0} / {adSetWorkflow.specs?.length ?? 0}
                </div>
                <div className="small text-muted">
                  Última atualização: {formatDateTimeValue(
                    adSetWorkflow.updatedAt ?? adSetWorkflow.completedAt ?? adSetWorkflow.createdAt,
                  )}
                </div>
              </div>
              {adSetWorkflow.lastError ? (
                <div className="alert alert-warning mt-3 mb-0" role="alert">
                  <strong>Último erro:</strong> {adSetWorkflow.lastError}
                </div>
              ) : null}
            </>
          ) : (
            <p className="text-muted small mt-3 mb-0">
              Este experimento ainda não iniciou o playbook de públicos. Use o botão acima para configurar.
            </p>
          )}
        </div>
      </div>

      </div>
      {data.journeyTemplateId ? (
        <div className="card border-0 shadow-sm rounded-3 mt-3">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start">
              <div>
                <h5 className="card-title mb-0">Jornada</h5>
                <p className="text-muted mb-0">
                  Template associado: {data.journeyTemplateName ?? "—"}
                </p>
              </div>
              {journeyAssignments?.journeyId ? (
                <span className="badge text-bg-secondary">
                  Jornada #{journeyAssignments.journeyId}
                </span>
              ) : null}
            </div>
            {journeyError ? (
              <div className="alert alert-danger mt-3" role="alert">
                {journeyError}
              </div>
            ) : null}
            {isLoadingJourneyAssignments ? (
              <div className="text-muted small mt-3">Carregando jornada...</div>
            ) : assignmentsWithSteps.length > 0 ? (
              <ul className="list-group list-group-flush mt-3">
                {assignmentsWithSteps.map(({ assignment, step }) => (
                  <li key={assignment.id} className="list-group-item px-0">
                    <div className="d-flex justify-content-between align-items-start">
                      <div>
                        <div className="fw-semibold">
                          {step?.name ??
                            step?.phase ??
                            `Passo ${assignment.nextStepId ?? "—"}`}
                        </div>
                        <div className="text-muted small">
                          {step?.phase ?? "—"} · {step?.stimulusType ?? "—"}
                        </div>
                      </div>
                      <span className="badge text-bg-light text-dark">
                        {assignment.status}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="text-muted small mt-3">
                Nenhuma jornada criada ainda. Clique em "Criar jornada" para
                gerar os passos do template.
              </div>
            )}
          </div>
        </div>
      ) : null}
      <Tabs.Root value={tab} onValueChange={setTab} className="mt-3">
        <Tabs.List className="nav nav-tabs">
          <Tabs.Trigger value="overview" className="nav-link">
            Overview
          </Tabs.Trigger>
          <Tabs.Trigger value="targeting" className="nav-link">
            Segmentação
          </Tabs.Trigger>
          <Tabs.Trigger value="creatives" className="nav-link">
            Criativos
          </Tabs.Trigger>
          {hasInstantFormSteps ? (
            <Tabs.Trigger value="instant-form" className="nav-link">
              Instant Forms
            </Tabs.Trigger>
          ) : null}
          {hasEmailSteps ? (
            <Tabs.Trigger value="emails" className="nav-link">
              E-mails
            </Tabs.Trigger>
          ) : null}
          <Tabs.Trigger value="sample-emails" className="nav-link">
            E-mails de amostra
          </Tabs.Trigger>
          <Tabs.Trigger value="lead-portal" className="nav-link">
            Portal do Lead
          </Tabs.Trigger>
          <Tabs.Trigger value="deliverables" className="nav-link">
            Entregáveis
          </Tabs.Trigger>
        </Tabs.List>
        <Tabs.Content value="overview" asChild>
          <div className="d-flex flex-column gap-3">
            <div className="card">
              <div className="card-body p-0">
                <dl className="row mb-0">
                  {rows.map((r, idx) => (
                    <Fragment key={r.label}>
                      <dt
                        className={`col-sm-3 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                      >
                        {r.label}
                      </dt>
                      <dd
                        className={`col-sm-9 py-2${idx % 2 === 0 ? " bg-light" : ""}`}
                      >
                        {r.value}
                      </dd>
                    </Fragment>
                  ))}
                </dl>
              </div>
            </div>
            <div className="card">
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start mb-2">
                  <div>
                    <h5 className="card-title mb-1">Pixel do Facebook</h5>
                    <p className="text-muted mb-0">
                      Geramos um pixel por experimento para rastrear conversões
                      do checkout.
                    </p>
                  </div>
                  {data.facebookPixelCreatedAt ? (
                    <span className="text-muted small">
                      Criado em{" "}
                      {formatDateTimeValue(data.facebookPixelCreatedAt)}
                    </span>
                  ) : null}
                </div>
                {data.facebookPixelId ? (
                  <>
                    <div className="mb-2">
                      <strong>ID:</strong> {data.facebookPixelId}
                    </div>
                    {data.facebookPixelCode ? (
                      <textarea
                        readOnly
                        className="form-control font-monospace"
                        value={data.facebookPixelCode}
                        rows={6}
                      />
                    ) : (
                      <div className="alert alert-info mb-0">
                        Pixel criado, aguardando retorno do código completo pelo
                        Facebook.
                      </div>
                    )}
                  </>
                ) : (
                  <div className="alert alert-warning mb-0">
                    Pixel ainda não criado. Ele será criado automaticamente
                    quando o experimento estiver pronto e aprovado.
                  </div>
                )}
              </div>
            </div>
          </div>
        </Tabs.Content>
        <Tabs.Content value="targeting" asChild>
          <TargetingTab
            nicheId={data.nicheId}
            hypothesisId={data.hypothesisId}
            nicheName={niche?.name}
            hypothesisTitle={hyp?.title}
          />
        </Tabs.Content>
        <Tabs.Content value="creatives" asChild>
          <CriativosTab experimentId={expId} />
        </Tabs.Content>
        {hasInstantFormSteps ? (
          <Tabs.Content value="instant-form" asChild>
            <InstantFormsTab experiment={data} steps={templateSteps} />
          </Tabs.Content>
        ) : null}
        {hasEmailSteps ? (
          <Tabs.Content value="emails" asChild>
            <EmailsTab
              experimentId={expId}
              requestedEmails={data.emailsToGenerate}
              journeyId={journeyAssignments?.journeyId ?? undefined}
              steps={templateSteps}
              experimentName={data.name}
            />
          </Tabs.Content>
        ) : null}
        <Tabs.Content value="sample-emails" asChild>
          <SampleEmailsTab
            experimentId={expId}
            requestedSampleEmails={data.sampleEmailsToGenerate}
            selectedSampleEmailId={data.selectedSampleEmailId}
            selectedSampleEmailSubject={data.selectedSampleEmailSubject}
            selectedSampleEmailUpdatedAt={data.selectedSampleEmailUpdatedAt}
          />
        </Tabs.Content>
        <Tabs.Content value="lead-portal" asChild>
          <LeadPortalFlowTab experiment={data} />
        </Tabs.Content>
        <Tabs.Content value="deliverables" asChild>
          <DeliverablesTab experiment={data} nicheName={niche?.name} />
        </Tabs.Content>
      </Tabs.Root>
      {isResetModalOpen ? (
        <div className="modal d-block" tabIndex={-1} role="dialog" aria-modal="true">
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Resetar campanhas pendentes</h5>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Fechar"
                  onClick={closeResetModal}
                  disabled={resetCampaigns.isPending}
                />
              </div>
              <div className="modal-body">
                <p>
                  Este reset apaga campanhas, conjuntos e anúncios que foram salvos apenas localmente e ainda não
                  possuem ID do Meta. Utilize a ação quando quiser recomeçar a publicação do experimento do zero.
                </p>
                {previewErrorMessage ? (
                  <div className="alert alert-danger" role="alert">
                    {previewErrorMessage}
                  </div>
                ) : isFetchingResetPreview ? (
                  <div className="d-flex align-items-center gap-2">
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                    <span>Calculando itens que serão removidos...</span>
                  </div>
                ) : hasItemsToReset ? (
                  <table className="table table-sm mb-3">
                    <tbody>
                      <tr>
                        <td>Campanhas locais sem ID do Meta</td>
                        <td className="text-end fw-semibold">{resetPreviewSummary.campaigns}</td>
                      </tr>
                      <tr>
                        <td>Conjuntos de anúncios</td>
                        <td className="text-end fw-semibold">{resetPreviewSummary.adSets}</td>
                      </tr>
                      <tr>
                        <td>Anúncios</td>
                        <td className="text-end fw-semibold">{resetPreviewSummary.ads}</td>
                      </tr>
                      <tr>
                        <td>Criativos vinculados</td>
                        <td className="text-end fw-semibold">{resetPreviewSummary.creatives}</td>
                      </tr>
                    </tbody>
                  </table>
                ) : (
                  <p className="text-muted mb-0">
                    Nenhuma campanha sem ID do Meta foi encontrada para este experimento.
                  </p>
                )}
                <p className="text-muted small mt-3 mb-0">
                  Somente ativos sem ID do Meta serão excluídos. Tudo que já foi publicado permanece intacto.
                </p>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={closeResetModal}
                  disabled={resetCampaigns.isPending}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={handleConfirmReset}
                  disabled={confirmResetDisabled}
                >
                  {resetCampaigns.isPending ? (
                    <>
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        role="status"
                        aria-hidden="true"
                      />
                      Resetando...
                    </>
                  ) : (
                    "Confirmar reset"
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
