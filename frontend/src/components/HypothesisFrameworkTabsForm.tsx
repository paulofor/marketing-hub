import { useState } from "react";
import * as Tabs from "@radix-ui/react-tabs";
import { useFormContext } from "react-hook-form";
import { Loader2 } from "lucide-react";
import type { HypothesisFrameworkSection } from "../api/hypothesis/types";
import { normalizeFramework } from "../api/hypothesis/types";
import { useGenerateFrameworkSection } from "../api/hypothesis/useGenerateFrameworkSection";
import type { HypothesisFormValues } from "../pages/hypothesis/formTypes";
import { HypothesisProofLibrary } from "./HypothesisProofLibrary";
import { HypothesisOfferPackageSelector } from "./HypothesisOfferPackageSelector";

const SECTIONS: { id: HypothesisFrameworkSection; label: string }[] = [
  { id: "pain", label: "Dor" },
  { id: "result", label: "Resultado" },
  { id: "mechanism", label: "Mecanismo" },
  { id: "proof", label: "Prova" },
  { id: "offer", label: "Oferta" },
];

const CHECKLIST_FIELDS = [
  { name: "painReady", label: "Dor validada" },
  { name: "resultReady", label: "Resultado claro" },
  { name: "mechanismReady", label: "Mecanismo explicável" },
  { name: "proofReady", label: "Prova definida" },
  { name: "offerReady", label: "Oferta empacotada" },
  { name: "approvedForExperiment", label: "Aprovado para experimento" },
] as const;

interface Props {
  hypothesisId?: string;
  nicheId?: string;
  readOnly?: boolean;
}

export function HypothesisFrameworkTabsForm({
  hypothesisId,
  nicheId,
  readOnly = false,
}: Props) {
  const { register, setValue, watch } = useFormContext<HypothesisFormValues>();
  const [tab, setTab] = useState<HypothesisFrameworkSection>("pain");
  const [instructions, setInstructions] = useState<Record<HypothesisFrameworkSection, string>>({
    pain: "",
    result: "",
    mechanism: "",
    proof: "",
    offer: "",
  });
  const offerPackageId = watch("offerPackageId");
  const [pendingSection, setPendingSection] = useState<
    HypothesisFrameworkSection | undefined
  >();
  const generate = useGenerateFrameworkSection(hypothesisId, nicheId);

  const handleGenerate = async (section: HypothesisFrameworkSection) => {
    if (!hypothesisId || readOnly) return;
    try {
      setPendingSection(section);
      const updated = await generate.mutateAsync({
        section,
        customInstructions: instructions[section],
      });
      if (updated?.framework) {
        setValue("framework", normalizeFramework(updated.framework), {
          shouldDirty: true,
        });
      }
      setValue("problem", updated?.problem ?? "", { shouldDirty: true });
      setValue("promise", updated?.promise ?? "", { shouldDirty: true });
      setValue("mechanism", updated?.mechanism ?? "", { shouldDirty: true });
      setValue("uniqueMechanism", updated?.uniqueMechanism ?? "", {
        shouldDirty: true,
      });
      setValue("entrega", updated?.entrega ?? "", { shouldDirty: true });
      setValue("title", updated?.title ?? "", { shouldDirty: true });
      setValue("price", updated?.price ?? undefined, { shouldDirty: true });
      if (updated?.offerType) {
        setValue("offerType", updated.offerType as "LEAD" | "TRIPWIRE", {
          shouldDirty: true,
        });
      }
    } finally {
      setPendingSection(undefined);
    }
  };

  const disabled = readOnly || !hypothesisId;

  const renderAiActions = (section: HypothesisFrameworkSection) => (
    <div className="d-flex flex-column flex-md-row gap-2 mt-3">
      <textarea
        className="form-control"
        placeholder="Instruções extras para o modelo (opcional)"
        rows={2}
        value={instructions[section]}
        disabled={readOnly}
        onChange={(event) =>
          setInstructions((prev) => ({
            ...prev,
            [section]: event.target.value,
          }))
        }
      />
      <button
        type="button"
        className="btn btn-outline-primary align-self-start"
        disabled={disabled}
        onClick={() => handleGenerate(section)}
      >
        {pendingSection === section && generate.isPending ? (
          <span className="d-inline-flex align-items-center gap-1">
            <Loader2 className="icon icon-sm spin" /> Gerando...
          </span>
        ) : (
          "Gerar com IA"
        )}
      </button>
    </div>
  );

  return (
    <div className="card">
      <div className="card-header d-flex justify-content-between align-items-center">
        <div>
          <h3 className="h5 mb-0">Framework Dor → Resultado → Oferta</h3>
          <small className="text-muted">
            Capture todas as hipóteses de mensagem antes de enviar para experimento.
          </small>
        </div>
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
          <Tabs.Content value="pain">
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Dor de superfície</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.pain.surface")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Dor raiz</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.pain.root")}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label">Dor emocional</label>
                <textarea
                  className="form-control"
                  rows={2}
                  disabled={readOnly}
                  {...register("framework.pain.emotional")}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label">Dor social</label>
                <textarea
                  className="form-control"
                  rows={2}
                  disabled={readOnly}
                  {...register("framework.pain.social")}
                />
              </div>
              <div className="col-md-4">
                <label className="form-label">Custo da inação</label>
                <textarea
                  className="form-control"
                  rows={2}
                  disabled={readOnly}
                  {...register("framework.pain.cost")}
                />
              </div>
            </div>
            {!readOnly && renderAiActions("pain")}
          </Tabs.Content>
          <Tabs.Content value="result">
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Resultado desejado</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.result.desiredResult")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Identidade desejada</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.result.desiredIdentity")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Impacto no negócio</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.result.businessOutcome")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Sinal de sucesso</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.result.successSignal")}
                />
              </div>
            </div>
            {!readOnly && renderAiActions("result")}
          </Tabs.Content>
          <Tabs.Content value="mechanism">
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Mecanismo central</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.mechanism.core")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Mecanismo único/diferencial</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.mechanism.unique")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">O que o lead consegue ver/testar</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.mechanism.visible")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Por que acreditar</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.mechanism.believability")}
                />
              </div>
            </div>
            {!readOnly && renderAiActions("mechanism")}
          </Tabs.Content>
          <Tabs.Content value="proof">
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Tipo de prova</label>
                <input
                  className="form-control"
                  disabled={readOnly}
                  {...register("framework.proof.type")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Ativo/formato</label>
                <input
                  className="form-control"
                  disabled={readOnly}
                  {...register("framework.proof.asset")}
                />
              </div>
              <div className="col-md-12">
                <label className="form-label">Mensagem da prova</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.proof.message")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Estágio de entrega</label>
                <input
                  className="form-control"
                  disabled={readOnly}
                  {...register("framework.proof.deliveryStage")}
                />
              </div>
            </div>
            <HypothesisProofLibrary
              hypothesisId={hypothesisId}
              readOnly={readOnly}
              onApply={(proof) => {
                setValue("framework.proof.type", proof.typeLabel ?? "", { shouldDirty: true });
                setValue(
                  "framework.proof.asset",
                  proof.assetPlan ?? proof.assetUrl ?? "",
                  { shouldDirty: true },
                );
                setValue("framework.proof.message", proof.message ?? "", { shouldDirty: true });
                setValue(
                  "framework.proof.deliveryStage",
                  proof.stage ?? "",
                  { shouldDirty: true },
                );
              }}
            />
            {!readOnly && renderAiActions("proof")}
          </Tabs.Content>
          <Tabs.Content value="offer">
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Nome da oferta</label>
                <input
                  className="form-control"
                  disabled={readOnly}
                  {...register("framework.offer.name")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Promessa central</label>
                <input
                  className="form-control"
                  disabled={readOnly}
                  {...register("framework.offer.corePromise")}
                />
              </div>
              <div className="col-md-12">
                <label className="form-label">Entregáveis principais</label>
                <textarea
                  className="form-control"
                  rows={3}
                  disabled={readOnly}
                  {...register("framework.offer.deliverables")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Mitigação de risco</label>
                <textarea
                  className="form-control"
                  rows={2}
                  disabled={readOnly}
                  {...register("framework.offer.riskReversal")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Narrativa de preço/valor</label>
                <textarea
                  className="form-control"
                  rows={2}
                  disabled={readOnly}
                  {...register("framework.offer.priceLogic")}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">CTA final</label>
                <input
                  className="form-control"
                  disabled={readOnly}
                  {...register("framework.offer.cta")}
                />
              </div>
            </div>
            <HypothesisOfferPackageSelector
              hypothesisId={hypothesisId}
              nicheId={nicheId ? Number(nicheId) : undefined}
              value={offerPackageId ?? null}
              onChange={(id) => setValue("offerPackageId", id ?? null, { shouldDirty: true })}
              readOnly={readOnly}
            />
            {!readOnly && renderAiActions("offer")}
          </Tabs.Content>
        </Tabs.Root>

        <hr className="my-4" />
        <div className="row g-3">
          {CHECKLIST_FIELDS.map((field) => (
            <div className="col-md-4" key={field.name}>
              <div className="form-check">
                <input
                  id={`framework-check-${field.name}`}
                  type="checkbox"
                  className="form-check-input"
                  disabled={readOnly}
                  {...register(
                    `framework.checklist.${field.name}` as const,
                  )}
                />
                <label
                  className="form-check-label"
                  htmlFor={`framework-check-${field.name}`}
                >
                  {field.label}
                </label>
              </div>
            </div>
          ))}
          <div className="col-12">
            <label className="form-label">Notas da revisão</label>
            <textarea
              className="form-control"
              rows={2}
              disabled={readOnly}
              {...register("framework.checklist.notes")}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
