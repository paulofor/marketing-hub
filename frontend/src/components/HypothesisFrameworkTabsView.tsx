import { Fragment, useState } from "react";
import * as Tabs from "@radix-ui/react-tabs";
import { Loader2 } from "lucide-react";
import type { HypothesisFramework } from "../api/hypothesis/types";
import { normalizeFramework } from "../api/hypothesis/types";
import type { HypothesisFrameworkSection } from "../api/hypothesis/types";
import { useGenerateFrameworkSection } from "../api/hypothesis/useGenerateFrameworkSection";

const SECTIONS: { id: HypothesisFrameworkSection; label: string }[] = [
  { id: "pain", label: "Dor" },
  { id: "result", label: "Resultado" },
  { id: "mechanism", label: "Mecanismo" },
  { id: "proof", label: "Prova" },
  { id: "offer", label: "Oferta" },
];

interface Props {
  hypothesisId: string;
  nicheId?: string;
  framework?: HypothesisFramework | null;
  onRefresh?: () => void;
}

export function HypothesisFrameworkTabsView({
  hypothesisId,
  nicheId,
  framework,
  onRefresh,
}: Props) {
  const data = normalizeFramework(framework);
  const [tab, setTab] = useState<HypothesisFrameworkSection>("pain");
  const [instructions, setInstructions] = useState<Record<HypothesisFrameworkSection, string>>({
    pain: "",
    result: "",
    mechanism: "",
    proof: "",
    offer: "",
  });
  const [pendingSection, setPendingSection] = useState<
    HypothesisFrameworkSection | undefined
  >();
  const generate = useGenerateFrameworkSection(hypothesisId, nicheId);

  const handleGenerate = async (section: HypothesisFrameworkSection) => {
    try {
      setPendingSection(section);
      await generate.mutateAsync({
        section,
        customInstructions: instructions[section],
      });
      onRefresh?.();
    } finally {
      setPendingSection(undefined);
    }
  };

  const renderRows = (rows: Array<{ label: string; value?: string | null }>) => (
    <dl className="row">
      {rows.map((row, index) => (
        <Fragment key={`${row.label}-${index}`}>
          <dt className="col-sm-4 text-muted">{row.label}</dt>
          <dd className="col-sm-8">{row.value?.trim() || "-"}</dd>
        </Fragment>
      ))}
    </dl>
  );

  const renderSection = (section: HypothesisFrameworkSection) => {
    switch (section) {
      case "pain":
        return renderRows([
          { label: "Superfície", value: data.pain.surface },
          { label: "Raiz", value: data.pain.root },
          { label: "Dor emocional", value: data.pain.emotional },
          { label: "Dor social", value: data.pain.social },
          { label: "Custo", value: data.pain.cost },
        ]);
      case "result":
        return renderRows([
          { label: "Resultado desejado", value: data.result.desiredResult },
          { label: "Identidade", value: data.result.desiredIdentity },
          { label: "Impacto de negócio", value: data.result.businessOutcome },
          { label: "Sinal de sucesso", value: data.result.successSignal },
        ]);
      case "mechanism":
        return renderRows([
          { label: "Mecanismo central", value: data.mechanism.core },
          { label: "Mecanismo único", value: data.mechanism.unique },
          { label: "O que é visível", value: data.mechanism.visible },
          { label: "Por que acreditar", value: data.mechanism.believability },
        ]);
      case "proof":
        return renderRows([
          { label: "Tipo", value: data.proof.type },
          { label: "Ativo", value: data.proof.asset },
          { label: "Mensagem", value: data.proof.message },
          { label: "Estágio", value: data.proof.deliveryStage },
        ]);
      case "offer":
      default:
        return renderRows([
          { label: "Nome", value: data.offer.name },
          { label: "Promessa", value: data.offer.corePromise },
          { label: "Entregáveis", value: data.offer.deliverables },
          { label: "Risco", value: data.offer.riskReversal },
          { label: "Narrativa de preço", value: data.offer.priceLogic },
          {
            label: "Preço",
            value:
              typeof data.offer.priceAmount === "number"
                ? `R$ ${data.offer.priceAmount.toFixed(2)}`
                : undefined,
          },
          { label: "CTA", value: data.offer.cta },
        ]);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <h3 className="h6 mb-0">Framework Dor → Resultado → Oferta</h3>
        <small className="text-muted">
          Revise ou gere novamente cada seção antes de aprovar a hipótese.
        </small>
      </div>
      <div className="card-body">
        <Tabs.Root value={tab} onValueChange={(value) => setTab(value as HypothesisFrameworkSection)}>
          <Tabs.List className="nav nav-tabs mb-3">
            {SECTIONS.map((section) => (
              <Tabs.Trigger
                key={section.id}
                value={section.id}
                className={`nav-link ${tab === section.id ? "active" : ""}`}
              >
                {section.label}
              </Tabs.Trigger>
            ))}
          </Tabs.List>
          {SECTIONS.map((section) => (
            <Tabs.Content key={section.id} value={section.id}>
              {renderSection(section.id)}
              <div className="d-flex flex-column flex-md-row gap-2 mt-3">
                <textarea
                  className="form-control"
                  rows={2}
                  placeholder="Instruções extras (opcional)"
                  value={instructions[section.id]}
                  onChange={(event) =>
                    setInstructions((prev) => ({
                      ...prev,
                      [section.id]: event.target.value,
                    }))
                  }
                />
                <button
                  type="button"
                  className="btn btn-outline-primary align-self-start"
                  onClick={() => handleGenerate(section.id)}
                  disabled={generate.isPending}
                >
                  {pendingSection === section.id && generate.isPending ? (
                    <span className="d-inline-flex align-items-center gap-1">
                      <Loader2 className="icon icon-sm spin" /> Gerando...
                    </span>
                  ) : (
                    "Gerar com IA"
                  )}
                </button>
              </div>
            </Tabs.Content>
          ))}
        </Tabs.Root>

        <hr className="my-4" />
        <h4 className="h6">Checklist de aprovação</h4>
        <dl className="row mb-0">
          <dt className="col-sm-4">Dor validada</dt>
          <dd className="col-sm-8">{data.checklist.painReady ? "Sim" : "Não"}</dd>
          <dt className="col-sm-4">Resultado claro</dt>
          <dd className="col-sm-8">{data.checklist.resultReady ? "Sim" : "Não"}</dd>
          <dt className="col-sm-4">Mecanismo pronto</dt>
          <dd className="col-sm-8">{data.checklist.mechanismReady ? "Sim" : "Não"}</dd>
          <dt className="col-sm-4">Prova definida</dt>
          <dd className="col-sm-8">{data.checklist.proofReady ? "Sim" : "Não"}</dd>
          <dt className="col-sm-4">Oferta empacotada</dt>
          <dd className="col-sm-8">{data.checklist.offerReady ? "Sim" : "Não"}</dd>
          <dt className="col-sm-4">Liberado para experimento</dt>
          <dd className="col-sm-8">
            {data.checklist.approvedForExperiment ? "Sim" : "Não"}
          </dd>
          {data.checklist.notes && (
            <>
              <dt className="col-sm-4">Notas</dt>
              <dd className="col-sm-8">{data.checklist.notes}</dd>
            </>
          )}
        </dl>
      </div>
    </div>
  );
}
