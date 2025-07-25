import { Fragment, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useNiche } from "../../api/niche/useNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import PageTitle from "../../components/PageTitle";
import CriativosTab from "../Experiment/CriativosTab";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import * as Tabs from "@radix-ui/react-tabs";

export default function ExperimentDetailPage() {
  const { id } = useParams();
  const expId = id as string;
  const { data, isLoading } = useExperiment(expId);
  const { data: niche } = useNiche(data?.nicheId ?? 0);
  const { data: hyp } = useHypothesis(
    data ? String(data.nicheId) : undefined,
    data ? String(data.hypothesisId) : undefined,
  );
  const [tab, setTab] = useState("overview");
  useBreadcrumbs([
    { label: "Nichos", to: "/niches" },
    { label: niche?.name || "...", to: `/niches/${data?.nicheId}` },
    { label: hyp?.title || "...", to: `/niches/${data?.nicheId}/hypotheses/${data?.hypothesisId}` },
    { label: data?.name || "..." },
  ]);
  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;
  const rows = [
    { label: "Nome", value: data.name },
    {
      label: "Nicho",
      value: <Link to={`/niches/${data.nicheId}/edit`}>{niche?.name}</Link>,
    },
    { label: "Hipótese", value: data.hypothesis },
    { label: "KPI", value: data.kpiTarget },
    { label: "Início", value: data.startDate },
    { label: "Término", value: data.endDate },
  ];
  return (
    <div>
      <div className="d-flex justify-content-between align-items-center">
        <PageTitle>{`Teste ${data.id}`}</PageTitle>
        <span className="badge bg-secondary">{data.status}</span>
      </div>
      <Tabs.Root value={tab} onValueChange={setTab} className="mt-3">
        <Tabs.List className="nav nav-tabs">
          <Tabs.Trigger value="overview" className="nav-link">
            Overview
          </Tabs.Trigger>
          <Tabs.Trigger value="landings" className="nav-link">
            Landings
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
        <Tabs.Content value="landings">
          <p>Landings aqui</p>
        </Tabs.Content>
        <Tabs.Content value="audiences">
          <p>Públicos aqui</p>
        </Tabs.Content>
        <Tabs.Content value="creatives" asChild>
          <CriativosTab experimentId={expId} />
        </Tabs.Content>
      </Tabs.Root>
    </div>
  );
}
