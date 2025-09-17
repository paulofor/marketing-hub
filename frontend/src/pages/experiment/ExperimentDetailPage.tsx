import { Fragment, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import CriativosTab from "./CriativosTab";
import PublicosTab from "./PublicosTab";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";
import FunnelPreviewModal from "./FunnelPreviewModal";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const { data, isLoading } = useExperiment(expId);
  const { data: niche } = useNiche(data?.nicheId ?? 0);
  const { data: hyp } = useHypothesis(
    data ? String(data.nicheId) : undefined,
    data ? String(data.hypothesisId) : undefined,
  );
  const { data: presets } = useMetricPresets();
  const [tab, setTab] = useState("overview");
  const [isFunnelPreviewOpen, setFunnelPreviewOpen] = useState(false);
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: niche?.name || "...", to: `/niches/${data?.nicheId}` },
    {
      label: hyp?.title || "...",
      to: `/niches/${data?.nicheId}/hypotheses/${data?.hypothesisId}`,
    },
    { label: data?.name || "..." },
  ]);
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const preset = presets?.find((p) => p.id === data.metricPresetId);
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
      value: data.audienceApproved ? "Sim" : "Não",
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
          <PageTitle>{data.name}</PageTitle>
          <p className="text-muted mb-0">{data.hypothesis}</p>
        </div>
        <div className="d-flex align-items-center">
          <Link to="edit" className="btn btn-outline-secondary me-2">
            Editar
          </Link>
          <span className="badge bg-secondary">{data.status}</span>
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
