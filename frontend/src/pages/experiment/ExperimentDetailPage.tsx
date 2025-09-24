import { Fragment, useState } from "react";
import { useParams, Link } from "react-router-dom";
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
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";
import FunnelPreviewModal from "./FunnelPreviewModal";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
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
  const [isFunnelPreviewOpen, setFunnelPreviewOpen] = useState(false);
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
  const readinessChecks = [
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
    ...(data.salesFunnelName
      ? [
          {
            label: "Funil de Vendas",
            value: data.salesFunnelId ? (
              <button
                type="button"
                className="btn btn-link p-0 align-baseline"
                onClick={() => setFunnelPreviewOpen(true)}
              >
                {data.salesFunnelName}
              </button>
            ) : (
              data.salesFunnelName
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
  return (
    <div>
      <div className="d-flex justify-content-between align-items-start">
        <div>
          <PageTitle icon={experimentIcon}>{data.name}</PageTitle>
          <p className="text-muted mb-0">{data.hypothesis}</p>
        </div>
        <div className="d-flex align-items-center">
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
      </Tabs.Root>
      {isFunnelPreviewOpen && data.salesFunnelId && (
        <FunnelPreviewModal
          funnelId={data.salesFunnelId}
          fallbackName={data.salesFunnelName}
          onClose={() => setFunnelPreviewOpen(false)}
        />
      )}
    </div>
  );
}
