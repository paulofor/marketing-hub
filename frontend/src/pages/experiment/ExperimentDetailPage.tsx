import { Fragment, useEffect, useMemo, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import CriativosTab from "./CriativosTab";
import PublicosTab from "./PublicosTab";
import InstantFormsTab from "./InstantFormsTab";
import EmailsTab from "./EmailsTab";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import { useJourneyTemplate } from "../../api/journey/useJourneyTemplate";
import { useExperimentJourneyAssignments } from "../../api/experiment/useExperimentJourneyAssignments";
import { useRebuildExperimentJourney } from "../../api/experiment/useRebuildExperimentJourney";
import type { JourneyAssignment, JourneyStep } from "../../api/journey/types";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const { data: niche } = useNiche(data?.nicheId ?? 0);
  const { data: hyp } = useHypothesis(
    data ? String(data.nicheId) : undefined,
    data ? String(data.hypothesisId) : undefined,
  );
  const nicheIdParam = data?.nicheId != null ? String(data.nicheId) : undefined;
  const { data: audiences } = useAudiencesByNiche(nicheIdParam);
  const { data: presets } = useMetricPresets();
  const [tab, setTab] = useState("overview");
  const [journeyError, setJourneyError] = useState<string | null>(null);
  const { data: facebookConfig, isLoading: isLoadingFacebookConfig } =
    useFacebookConfigurationStatus();
  const { data: journeyAssignments, isLoading: isLoadingJourneyAssignments } =
    useExperimentJourneyAssignments(expId);
  const { data: template } = useJourneyTemplate(data?.journeyTemplateId ?? undefined);
  const rebuildJourney = useRebuildExperimentJourney(expId);
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
  const hasEmailSteps = templateSteps.some((step) => step.stimulusType === "EMAIL");

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
      step: assignment.nextStepId ? stepIndex.get(assignment.nextStepId) : undefined,
    }));
    pairs.sort((a, b) => {
      const posA = a.step?.position ?? Number.MAX_SAFE_INTEGER;
      const posB = b.step?.position ?? Number.MAX_SAFE_INTEGER;
      if (posA !== posB) return posA - posB;
      return a.assignment.id - b.assignment.id;
    });
    return pairs;
  }, [journeyAssignments?.assignments, templateSteps]);

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const preset = presets?.find((p) => p.id === data.metricPresetId);
  const audienceList = Array.isArray(audiences) ? audiences : undefined;
  const relevantAudiences = audienceList
    ? audienceList.filter(
        (a) => !a.hypothesisId || a.hypothesisId === data.hypothesisId,
      )
    : undefined;
  const totalRelevantAudiences = relevantAudiences?.length ?? 0;
  const approvedAudiencesCount =
    relevantAudiences?.filter((a) => a.approved).length ?? 0;
  const approvedAudienceSummary = relevantAudiences
    ? `${approvedAudiencesCount} de ${relevantAudiences.length}`
    : "—";
  const formatCurrency = (n?: number | null) =>
    n != null
      ? new Intl.NumberFormat("pt-BR", {
          style: "currency",
          currency: "BRL",
        }).format(n)
      : "—";
  const formatPercent = (n?: number | null) => (n != null ? `${n}%` : "—");
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
            action: experimentInstantForm ? undefined : () => setTab("instant-form"),
            actionLabel: experimentInstantForm ? undefined : "Ir para Instant Forms",
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
      action: hasInstagramAccount ? undefined : () => navigate(`/experiments/${expId}/edit`),
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
      actionLabel: undefined,
    },
    {
      id: "creatives",
      title: "Criativos aprovados",
      isMet: data.creativeApproved,
      hint: data.creativeApproved
        ? "Os criativos já estão aprovados."
        : "Revise e aprove pelo menos um criativo na aba Criativos.",
      action: data.creativeApproved
        ? undefined
        : () => setTab("creatives"),
      actionLabel: "Ir para Criativos",
    },
    {
      id: "audiences",
      title: "Pelo menos um público aprovado",
      isMet: approvedAudiencesCount > 0,
      hint:
        approvedAudiencesCount > 0
          ? `${approvedAudiencesCount} público(s) aprovado(s) para este experimento.`
          : totalRelevantAudiences > 0
            ? "Aprove pelo menos um público na aba Públicos."
            : "Cadastre públicos para este nicho e aprove pelo menos um deles.",
      action:
        approvedAudiencesCount > 0 ? undefined : () => setTab("audiences"),
      actionLabel: "Ir para Públicos",
    },
  ];
  const isReadyForFacebook = readinessChecks.every((c) => c.isMet);
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
      label: "Instant Form",
      value: experimentInstantForm ? (
        <div>
          <div>{experimentInstantForm.name}</div>
          <div className="text-muted small">
            ID Meta: {experimentInstantForm.facebookFormId ?? "—"}
          </div>
          {experimentInstantForm.status && (
            <div className="text-muted small">
              Status: {experimentInstantForm.status}
            </div>
          )}
          {experimentInstantForm.locale && (
            <div className="text-muted small">
              Idioma: {experimentInstantForm.locale}
            </div>
          )}
          <div className="d-flex flex-column gap-1 mt-1">
            {experimentInstantForm.followUpActionUrl ? (
              <a
                href={experimentInstantForm.followUpActionUrl}
                target="_blank"
                rel="noreferrer"
                className="small"
              >
                Página de agradecimento
              </a>
            ) : null}
            {experimentInstantForm.privacyPolicyUrl ? (
              <a
                href={experimentInstantForm.privacyPolicyUrl}
                target="_blank"
                rel="noreferrer"
                className="small"
              >
                Política de privacidade
              </a>
            ) : null}
          </div>
        </div>
      ) : (
        "—"
      ),
    },
    {
      label: "Página de agradecimento padrão",
      value: data.followUpActionUrl ? (
        <a
          href={data.followUpActionUrl}
          target="_blank"
          rel="noreferrer"
        >
          Abrir página
        </a>
      ) : (
        "—"
      ),
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
    { label: "Instant forms a gerar", value: data.instantFormsToGenerate ?? "—" },
    { label: "E-mails a gerar", value: data.emailsToGenerate ?? "—" },
    {
      label: "Públicos aprovados",
      value: approvedAudienceSummary,
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
                <span className="spinner-border spinner-border-sm me-2" role="status" />
                Criando jornada...
              </>
            ) : (
              "Criar jornada"
            )}
          </button>
          <Link to="edit" className="btn btn-outline-secondary me-2">
            Editar
          </Link>
          <span className="badge bg-secondary">{data.status}</span>
        </div>
      </div>
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
                    >
                      {check.actionLabel}
                    </button>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
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
                          {step?.name ?? step?.phase ?? `Passo ${assignment.nextStepId ?? "—"}`}
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
                Nenhuma jornada criada ainda. Clique em "Criar jornada" para gerar os
                passos do template.
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
          <Tabs.Trigger value="audiences" className="nav-link">
            Públicos
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
        </Tabs.List>
        <Tabs.Content value="overview" asChild>
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
        </Tabs.Content>
        <Tabs.Content value="audiences" asChild>
          <PublicosTab
            nicheId={data.nicheId}
            hypothesisId={data.hypothesisId}
            nicheName={niche?.name}
            hypothesisTitle={hyp?.title ?? data.hypothesis}
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
      </Tabs.Root>
    </div>
  );
}
