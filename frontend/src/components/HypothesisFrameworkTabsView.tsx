import { Fragment, useState } from "react";
import * as Tabs from "@radix-ui/react-tabs";
import { Loader2 } from "lucide-react";
import axios from "axios";
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

type RequestUiStatus = "IDLE" | "PROCESSING" | "COMPLETED" | "FAILED";

interface SectionRequestState {
  status: RequestUiStatus;
  requestedAt?: string;
  completedAt?: string;
  customInstructions?: string;
  errorMessage?: string;
}

const STATUS_LABELS: Record<RequestUiStatus, string> = {
  IDLE: "Sem solicitação",
  PROCESSING: "Em processamento",
  COMPLETED: "Concluída",
  FAILED: "Com erro",
};

const STATUS_BADGES: Record<RequestUiStatus, string> = {
  IDLE: "secondary",
  PROCESSING: "warning",
  COMPLETED: "success",
  FAILED: "danger",
};

const SECTION_REQUEST_INITIAL_STATE: Record<
  HypothesisFrameworkSection,
  SectionRequestState
> = {
  pain: { status: "IDLE" },
  result: { status: "IDLE" },
  mechanism: { status: "IDLE" },
  proof: { status: "IDLE" },
  offer: { status: "IDLE" },
};

function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const responseMessage =
      typeof error.response?.data?.message === "string"
        ? error.response.data.message
        : undefined;
    return responseMessage ?? error.message ?? "Falha ao processar solicitação.";
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "Falha ao processar solicitação.";
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
  const [requestsBySection, setRequestsBySection] = useState<
    Record<HypothesisFrameworkSection, SectionRequestState>
  >(SECTION_REQUEST_INITIAL_STATE);
  const generate = useGenerateFrameworkSection(hypothesisId, nicheId);

  const handleGenerate = async (section: HypothesisFrameworkSection) => {
    const requestedAt = new Date().toISOString();
    const customInstructions = instructions[section]?.trim() ?? "";
    setRequestsBySection((prev) => ({
      ...prev,
      [section]: {
        status: "PROCESSING",
        requestedAt,
        completedAt: undefined,
        customInstructions,
        errorMessage: undefined,
      },
    }));

    try {
      setPendingSection(section);
      await generate.mutateAsync({
        section,
        customInstructions,
      });
      setRequestsBySection((prev) => ({
        ...prev,
        [section]: {
          ...prev[section],
          status: "COMPLETED",
          completedAt: new Date().toISOString(),
          errorMessage: undefined,
        },
      }));
      onRefresh?.();
    } catch (error) {
      setRequestsBySection((prev) => ({
        ...prev,
        [section]: {
          ...prev[section],
          status: "FAILED",
          completedAt: undefined,
          errorMessage: getErrorMessage(error),
        },
      }));
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
                  placeholder="Instruções extras para a IA (opcional)"
                  title="Escreva contexto adicional para guiar a IA nesta seção."
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

        <div className="border rounded-3 p-3 mt-4">
          <h4 className="h6 mb-2">Acompanhamento das solicitações IA</h4>
          <p className="small text-muted mb-3">
            Veja o que já foi solicitado, o que está em processamento e o que falhou em
            cada etapa do framework.
          </p>
          <div className="d-flex flex-column gap-2">
            {SECTIONS.map((section) => {
              const request = requestsBySection[section.id];
              return (
                <div
                  key={`request-status-${section.id}`}
                  className="border rounded-2 p-2 d-flex flex-column gap-1"
                >
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <strong>{section.label}</strong>
                    <span className={`badge text-bg-${STATUS_BADGES[request.status]}`}>
                      {STATUS_LABELS[request.status]}
                    </span>
                  </div>
                  <small className="text-muted">
                    Solicitado em: {formatDateTime(request.requestedAt)} · Concluído em:{" "}
                    {formatDateTime(request.completedAt)}
                  </small>
                  {request.customInstructions ? (
                    <small className="text-body-secondary">
                      Instruções enviadas: {request.customInstructions}
                    </small>
                  ) : null}
                  {request.errorMessage ? (
                    <small className="text-danger">Erro: {request.errorMessage}</small>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>

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
