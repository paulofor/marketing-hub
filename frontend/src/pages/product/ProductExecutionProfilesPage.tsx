import { useState, type FormEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
import { useProduct } from "../../api/product/useProduct";
import {
  useExecutionProfiles,
  useProfileCatalog,
  useChangeExecutionProfile,
  type ExecutionProfile,
} from "../../api/product/useExecutionProfiles";

const brl = (value: number) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    value,
  );
const scenarioNames: Record<string, string> = {
  FAVORABLE: "Favorável",
  BASE: "Base",
  CONSERVATIVE: "Conservador",
};
const moneyFields = [
  ["priceBrl", "Preço por pacote"],
  ["acquisitionBrl", "Aquisição (CAC)"],
  ["feesBrl", "Taxas e tributos"],
  ["supportBrl", "Suporte"],
  ["storageDeliveryBrl", "Armazenamento e entrega"],
  ["otherCostsBrl", "Outros custos, reembolsos e produção amortizada"],
] as const;

export default function ProductExecutionProfilesPage() {
  const { productId = "" } = useParams();
  const [search] = useSearchParams();
  const product = useProduct(productId);
  const profiles = useExecutionProfiles(productId);
  const catalog = useProfileCatalog(productId);
  const change = useChangeExecutionProfile(productId);
  const [selectedId, setSelectedId] = useState<number>(
    Number(search.get("profileId")),
  );
  const [editing, setEditing] = useState(false);
  const [copy, setCopy] = useState<ExecutionProfile>();
  const [noVariableAiCost, setNoVariableAiCost] = useState(false);
  const [notice, setNotice] = useState("");
  const [bindingId, setBindingId] = useState("");
  const selected =
    selectedId > 0
      ? profiles.data?.find((p) => p.id === selectedId)
      : (profiles.data?.find((p) =>
          p.bindings.some(
            (b) =>
              b.sourceReference === search.get("sourceReference") ||
              (search.has("learningCycleId") &&
                String(b.learningCycleId) === search.get("learningCycleId")),
          ),
        ) ?? profiles.data?.[0]);
  const binding =
    selected?.bindings.find((b) => String(b.id) === bindingId) ??
    selected?.bindings.find(
      (b) =>
        b.sourceReference === search.get("sourceReference") ||
        (search.has("learningCycleId") &&
          String(b.learningCycleId) === search.get("learningCycleId")),
    ) ??
    (selected?.bindings.length === 1 ? selected.bindings[0] : undefined);
  const processQuery = new URLSearchParams({
    chainId: String(selected?.chainId ?? search.get("chainId") ?? ""),
  });
  if (binding) processQuery.set("sourceReference", binding.sourceReference);
  if (binding?.learningCycleId)
    processQuery.set("learningCycleId", String(binding.learningCycleId));
  const error = axios.isAxiosError(change.error)
    ? (change.error.response?.data?.detail ??
      change.error.response?.data?.message ??
      change.error.message)
    : change.error?.message;

  async function submit(
    body: unknown,
    action?:
      | "bindings"
      | "financial-analysis"
      | "financial-reviews"
      | "consumption-reconciliations",
  ) {
    setNotice("");
    try {
      const result = await change.mutateAsync({
        id: action ? selected?.id : undefined,
        action,
        body,
      });
      setSelectedId(result.id);
      if (!action) setEditing(false);
      setNotice(
        "Registro salvo. Consulte o percurso e as pendências informadas pelo backend.",
      );
    } catch {
      /* O erro da requisição é apresentado pelo estado da mutation. */
    }
  }

  function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const text = (name: string) => String(values.get(name) ?? "").trim();
    const number = (name: string) => Number(text(name));
    const lines = (name: string) =>
      text(name)
        .split("\n")
        .map((v) => v.trim())
        .filter(Boolean);
    void submit({
      chainId: number("chainId"),
      commercialPlanId: number("commercialPlanId"),
      createdBy: text("actor"),
      contract: {
        productVersion: text("productVersion"),
        capability: text("capability"),
        purchasedOutcome: text("purchasedOutcome"),
        inputs: lines("inputs"),
        deliverables: lines("deliverables"),
        qualityCriteria: lines("qualityCriteria"),
        deliveryMode: text("deliveryMode"),
        revenueModel: text("revenueModel"),
        audiovisualRequired: values.has("audiovisualRequired"),
        includedUnits: number("includedUnits"),
        maximumAttempts: number("maximumAttempts"),
        costModel: noVariableAiCost ? "NO_VARIABLE_AI_COST" : text("costModel"),
        pricingRevision: text("pricingRevision"),
        usdBrl: number("usdBrl"),
        maximumAttemptCostBrl: noVariableAiCost
          ? 0
          : number("maximumAttemptCostBrl"),
        maximumDeliveryCostBrl: noVariableAiCost
          ? 0
          : number("maximumDeliveryCostBrl"),
        minimumContributionBrl: number("minimumContributionBrl"),
        productionBudget: {
          costModel: text("production-costModel"),
          maximumAttempts: number("production-maximumAttempts"),
          maximumAttemptCostBrl: number("production-maximumAttemptCostBrl"),
          maximumTotalCostBrl: number("production-maximumTotalCostBrl"),
        },
        scenarios: Object.keys(scenarioNames).map((code) =>
          Object.fromEntries([
            ["code", code],
            ...moneyFields.map(([key]) => [key, number(`${code}-${key}`)]),
          ]),
        ),
      },
    });
  }

  if (profiles.isLoading || catalog.isLoading)
    return <p role="status">Carregando fichas de execução...</p>;
  if (profiles.isError || catalog.isError)
    return (
      <div className="alert alert-danger" role="alert">
        Não foi possível consultar as fichas de execução.{" "}
        <button
          className="btn btn-link"
          disabled={profiles.isFetching || catalog.isFetching}
          onClick={() => {
            void profiles.refetch();
            void catalog.refetch();
          }}
        >
          {(profiles.isFetching || catalog.isFetching) && (
            <span className="spinner-border spinner-border-sm me-2" />
          )}
          Tentar novamente
        </button>
      </div>
    );

  return (
    <div style={{ maxWidth: 1120, overflowWrap: "anywhere" }}>
      <Link
        to={`/products/${productId}/value-chain-history?${processQuery}`}
        className="btn btn-outline-secondary mb-3"
      >
        Voltar à cadeia do produto
      </Link>
      <PageTitle>
        Ficha de execução ·{" "}
        {product.data?.internalName ||
          product.data?.name ||
          `Produto ${productId}`}
      </PageTitle>
      <p>
        Uma cadeia comum, com atividades adequadas ao resultado comprado. O
        formato da entrega orienta o percurso; o tipo do produto é preservado.
      </p>
      {notice && (
        <p className="alert alert-success" role="status">
          {notice}
        </p>
      )}
      {selectedId > 0 && !selected && (
        <p role="alert" className="alert alert-warning">
          A revisão solicitada não pertence a este produto ou não foi
          encontrada. Selecione uma revisão existente.
        </p>
      )}
      {error && (
        <p className="alert alert-danger" role="alert">
          {String(error)}
        </p>
      )}
      <div className="d-flex flex-wrap gap-2 mb-3">
        <label>
          Revisão{" "}
          <select
            className="form-select"
            value={selected?.id ?? ""}
            onChange={(e) => setSelectedId(Number(e.target.value))}
          >
            {!profiles.data?.length && (
              <option value="">Nenhuma ficha registrada</option>
            )}
            {profiles.data?.map((p) => (
              <option key={p.id} value={p.id}>
                v{p.revision} · {p.contract.productVersion} · {p.routeName}
              </option>
            ))}
          </select>
        </label>
        <button
          className="btn btn-primary align-self-end"
          onClick={() => {
            setCopy(undefined);
            setNoVariableAiCost(false);
            setEditing(true);
            change.reset();
          }}
        >
          Nova ficha
        </button>
        {selected && (
          <button
            className="btn btn-outline-primary align-self-end"
            onClick={() => {
              setCopy(selected);
              setNoVariableAiCost(
                selected.contract.costModel === "NO_VARIABLE_AI_COST",
              );
              setEditing(true);
              change.reset();
            }}
          >
            Criar nova revisão
          </button>
        )}
      </div>
      {editing && (
        <form
          onSubmit={create}
          className="card card-body mb-4"
          key={copy?.id ?? "new"}
        >
          <h2 className="h5">Contrato da entrega</h2>
          <p>
            Preencha os custos completos. Use zero apenas quando o custo
            realmente não existir. A revisão anterior e seus vínculos serão
            preservados.
          </p>
          <div className="row g-3">
            <Select
              label="Cadeia"
              name="chainId"
              options={catalog.data?.chains ?? []}
              value={String(copy?.chainId ?? search.get("chainId") ?? "")}
            />
            <Select
              label="Plano comercial"
              name="commercialPlanId"
              options={catalog.data?.commercialPlans ?? []}
              value={String(copy?.commercialPlanId ?? "")}
            />
            <Select
              label="Capacidade principal"
              name="capability"
              options={catalog.data?.capabilities ?? []}
              value={copy?.contract.capability ?? ""}
            />
            <Input
              label="Versão do produto"
              name="productVersion"
              value={copy?.contract.productVersion}
            />
            <Input label="Responsável pelo registro" name="actor" />
            <Input
              label="Modo de entrega"
              name="deliveryMode"
              value={copy?.contract.deliveryMode}
            />
            <Input
              label="Modelo de receita"
              name="revenueModel"
              value={copy?.contract.revenueModel}
            />
            <Area
              label="Resultado comprado"
              name="purchasedOutcome"
              value={copy?.contract.purchasedOutcome}
            />
            <Area
              label="Entradas necessárias (uma por linha)"
              name="inputs"
              value={copy?.contract.inputs.join("\n")}
            />
            <Area
              label="Entregáveis (um por linha)"
              name="deliverables"
              value={copy?.contract.deliverables.join("\n")}
            />
            <Area
              label="Critérios de qualidade (um por linha)"
              name="qualityCriteria"
              value={copy?.contract.qualityCriteria.join("\n")}
            />
          </div>
          <label className="form-check mt-3">
            <input
              className="form-check-input"
              type="checkbox"
              name="audiovisualRequired"
              defaultChecked={copy?.contract.audiovisualRequired}
            />
            A entrega inclui audiovisual
          </label>
          <h2 className="h5 mt-4">Limites por pacote completo</h2>
          <label className="form-check mb-3">
            <input
              className="form-check-input"
              type="checkbox"
              name="noVariableAiCost"
              checked={noVariableAiCost}
              onChange={(e) => setNoVariableAiCost(e.target.checked)}
            />
            Entrega sem consumo variável de IA
          </label>
          <p>
            Declare esta opção somente quando cada entrega vendida não fizer
            chamadas de IA. Custos de produção, aquisição, suporte e entrega
            continuam na análise.
          </p>
          <div className="row g-3">
            <Input
              label="Quantidade incluída"
              name="includedUnits"
              type="number"
              min="1"
              step="1"
              value={copy?.contract.includedUnits}
            />
            <Input
              label="Tentativas máximas, incluindo regenerações"
              name="maximumAttempts"
              type="number"
              min="1"
              step="1"
              value={copy?.contract.maximumAttempts}
            />
            <Input
              label="Modelo usado no cálculo"
              name="costModel"
              value={
                copy?.contract.costModel === "NO_VARIABLE_AI_COST"
                  ? ""
                  : copy?.contract.costModel
              }
              disabled={noVariableAiCost}
              placeholder={catalog.data?.imageModel}
            />
            <Input
              label="Referência e revisão da tarifa"
              name="pricingRevision"
              value={copy?.contract.pricingRevision}
            />
            <Input
              label="Câmbio BRL por USD"
              name="usdBrl"
              type="number"
              min="0.000001"
              value={copy?.contract.usdBrl}
            />
            <Input
              label="Teto por tentativa (R$)"
              name="maximumAttemptCostBrl"
              disabled={noVariableAiCost}
              type="number"
              min="0.000001"
              value={copy?.contract.maximumAttemptCostBrl}
            />
            <Input
              label="Teto total de geração e uso por pacote (R$)"
              name="maximumDeliveryCostBrl"
              disabled={noVariableAiCost}
              type="number"
              min="0.000001"
              value={copy?.contract.maximumDeliveryCostBrl}
            />
            <Input
              label="Contribuição mínima por pacote (R$)"
              name="minimumContributionBrl"
              type="number"
              min="0.01"
              value={copy?.contract.minimumContributionBrl}
            />
          </div>
          <h2 className="h5 mt-4">Orçamento privado de produção e teste</h2>
          <p>
            Limite total por execução, separado das entregas vendidas. Informe
            zero nas tentativas e nos dois valores para não autorizar consumo
            privado. A fonte da tarifa também deve cobrir este modelo.
          </p>
          <div className="row g-3">
            <Input
              label="Modelo da produção privada"
              name="production-costModel"
              value={copy?.contract.productionBudget.costModel}
              placeholder={catalog.data?.imageModel}
            />
            <Input
              label="Tentativas privadas máximas"
              name="production-maximumAttempts"
              type="number"
              min="0"
              step="1"
              value={copy?.contract.productionBudget.maximumAttempts}
            />
            <Input
              label="Teto por tentativa privada (R$)"
              name="production-maximumAttemptCostBrl"
              type="number"
              min="0"
              value={copy?.contract.productionBudget.maximumAttemptCostBrl}
            />
            <Input
              label="Teto total privado por execução (R$)"
              name="production-maximumTotalCostBrl"
              type="number"
              min="0"
              value={copy?.contract.productionBudget.maximumTotalCostBrl}
            />
          </div>
          {Object.entries(scenarioNames).map(([code, label]) => (
            <fieldset className="mt-4" key={code}>
              <legend className="h6">
                Cenário {label.toLowerCase()} · valores por pacote em reais
              </legend>
              <div className="row g-3">
                {moneyFields.map(([key, title]) => (
                  <Input
                    key={key}
                    label={title}
                    name={`${code}-${key}`}
                    type="number"
                    min={key === "priceBrl" ? "0.01" : "0"}
                    value={
                      copy?.contract.scenarios.find((s) => s.code === code)?.[
                        key
                      ]
                    }
                  />
                ))}
              </div>
            </fieldset>
          ))}
          <div className="d-flex gap-2 mt-4">
            <button className="btn btn-primary" disabled={change.isPending}>
              {change.isPending && (
                <span className="spinner-border spinner-border-sm me-2" />
              )}
              Salvar nova revisão
            </button>
            <button
              className="btn btn-outline-secondary"
              type="button"
              onClick={() => setEditing(false)}
            >
              Fechar formulário
            </button>
          </div>
        </form>
      )}
      {selected && (
        <>
          <section className="card card-body mb-3">
            <h2 className="h5">{selected.routeName}</h2>
            {selected.bindings.length > 1 && (
              <label>
                Contexto para navegar pelos processos
                <select
                  className="form-select"
                  value={binding?.id ?? ""}
                  onChange={(e) => setBindingId(e.target.value)}
                >
                  <option value="">Selecione a execução</option>
                  {selected.bindings.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.sourceReference} · ciclo{" "}
                      {b.learningCycleId ?? "não informado"}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <p>
              <strong>Resultado:</strong> {selected.contract.purchasedOutcome}
            </p>
            <p>
              {selected.productType || "Tipo não informado"} ·{" "}
              {selected.contract.deliveryMode} ·{" "}
              {selected.contract.revenueModel}
            </p>
            <p>
              Ficha v{selected.revision} · produto{" "}
              {selected.contract.productVersion} · plano v
              {selected.commercialPlanVersion} · registrada por{" "}
              {selected.createdBy}
            </p>
            {!binding && (
              <p className="alert alert-info">
                Vincule a ficha a uma execução e selecione o contexto para abrir
                seus processos.
              </p>
            )}
            {selected.blockers.length > 0 && (
              <div className="alert alert-warning">
                <strong>Pendências da ficha</strong>
                <ul className="mb-0">
                  {selected.blockers.map((b) => (
                    <li key={b}>{b}</li>
                  ))}
                </ul>
              </div>
            )}
            <ol className="ps-3">
              {selected.work.map((w) => {
                const process = selected.processes.find(
                  (p) => p.code === w.processCode,
                );
                return (
                  <li className="mb-3" key={`${w.processCode}/${w.activityId}`}>
                    <strong>{w.name}</strong> · {w.owner}
                    <p className="mb-1">
                      {w.applicable ? "Aplicável" : "Dispensada pelo contrato"}:{" "}
                      {w.applicabilityReason}
                    </p>
                    <ul>
                      {w.requirements.map((r, i) => (
                        <li key={i}>{r}</li>
                      ))}
                    </ul>
                    {process && binding && (
                      <Link
                        to={`/products/${productId}/value-chain-history/processes/${process.id}/activities?${processQuery}`}
                      >
                        Abrir processo · v{process.version}
                      </Link>
                    )}
                  </li>
                );
              })}
            </ol>
            <details>
              <summary>Subprocessos e versões fixadas</summary>
              <ul>
                {selected.processes.map((p) => (
                  <li key={p.id}>
                    {binding ? (
                      <Link
                        to={`/products/${productId}/value-chain-history/processes/${p.id}/activities?${processQuery}`}
                      >
                        {p.name} · v{p.version}
                      </Link>
                    ) : (
                      <span>
                        {p.name} · v{p.version}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </details>
          </section>
          <section className="card card-body mb-3">
            <h2 className="h5">Economia e checkpoints de Plutus</h2>
            <p>
              Projeções por pacote; não representam vendas medidas. Os
              checkpoints financeiros preservam as autorizações comerciais
              próprias.
            </p>
            <p>
              Entrega:{" "}
              {selected.contract.costModel === "NO_VARIABLE_AI_COST"
                ? "sem consumo variável de IA"
                : selected.contract.costModel}
              {" · "}teto de geração por pacote{" "}
              {brl(selected.contract.maximumDeliveryCostBrl)}. Produção privada:{" "}
              {selected.contract.productionBudget.costModel}, até{" "}
              {selected.contract.productionBudget.maximumAttempts} tentativas e{" "}
              {brl(selected.contract.productionBudget.maximumTotalCostBrl)} por
              execução.
            </p>
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>Cenário</th>
                    <th>Receita prevista</th>
                    <th>Custo completo</th>
                    <th>Contribuição</th>
                    <th>Viabilidade</th>
                  </tr>
                </thead>
                <tbody>
                  {selected.economics.map((s) => (
                    <tr key={s.code}>
                      <td>{scenarioNames[s.code] ?? s.code}</td>
                      <td>{brl(s.revenueBrl)}</td>
                      <td>{brl(s.fullCostBrl)}</td>
                      <td>{brl(s.contributionBrl)}</td>
                      <td>
                        {s.viable ? "Atende o mínimo" : "Abaixo do mínimo"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <button
              className="btn btn-outline-primary align-self-start"
              disabled={change.isPending}
              onClick={() => void submit(undefined, "financial-analysis")}
            >
              {change.isPending && (
                <span className="spinner-border spinner-border-sm me-2" />
              )}
              Solicitar ou consultar parecer de Plutus
            </button>
            <small className="mt-2">
              A primeira solicitação usa IA e tem custo. O parecer da mesma
              revisão é reaproveitado.
            </small>
            <ul className="mt-3">
              {selected.reviews.map((r, i) => (
                <li key={i}>
                  {r.checkpoint} · {r.executionStatus} · {r.decision} ·{" "}
                  {r.reviewedBy}
                  <p>
                    {r.rationale} ·{" "}
                    <Link to={`/planning/${selected.commercialPlanId}`}>
                      Parecer financeiro #{r.financialExecutionId}
                    </Link>
                  </p>
                </li>
              ))}
            </ul>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                void submit(
                  {
                    checkpoint: f.get("checkpoint"),
                    financialExecutionId: Number(f.get("financialExecutionId")),
                    reviewedBy: f.get("reviewedBy"),
                    rationale: f.get("rationale"),
                    approved: f.get("decision") === "APPROVE",
                  },
                  "financial-reviews",
                );
              }}
            >
              <h3 className="h6">Registrar decisão após ler o parecer</h3>
              <div className="row g-3">
                <Select
                  label="Checkpoint"
                  name="checkpoint"
                  options={catalog.data?.checkpoints ?? []}
                  value=""
                />
                <Input
                  label="Número do parecer"
                  name="financialExecutionId"
                  type="number"
                  min="1"
                  step="1"
                />
                <Input label="Responsável pela decisão" name="reviewedBy" />
                <Area label="Justificativa com evidências" name="rationale" />
                <Select
                  label="Decisão"
                  name="decision"
                  options={[
                    { code: "REJECT", name: "Reprovar" },
                    { code: "APPROVE", name: "Aprovar checkpoint financeiro" },
                  ]}
                  value=""
                />
              </div>
              <button
                className="btn btn-outline-primary mt-3"
                disabled={change.isPending}
              >
                {change.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                Registrar decisão financeira
              </button>
            </form>
          </section>
          <section className="card card-body mb-3">
            <h2 className="h5">Vincular a uma nova execução</h2>
            <p>
              O vínculo fixa esta ficha no contexto informado. Referências que
              já iniciaram tarefas mantêm o histórico e não podem ser alteradas.
            </p>
            {selected.bindings.map((b) => (
              <p key={b.id}>
                {b.sourceReference} ·{" "}
                {b.learningCycleId ? `Ciclo ${b.learningCycleId}` : "Sem ciclo"}{" "}
                · {b.createdBy}
              </p>
            ))}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                void submit(
                  {
                    sourceReference: f.get("sourceReference"),
                    learningCycleId: f.get("learningCycleId")
                      ? Number(f.get("learningCycleId"))
                      : null,
                    actor: f.get("actor"),
                  },
                  "bindings",
                );
              }}
            >
              <div className="row g-3">
                <Input
                  label="Referência oficial exibida no processo"
                  name="sourceReference"
                  value={search.get("sourceReference") ?? ""}
                  placeholder="experiment:123"
                />
                <Input
                  label="Ciclo, quando houver"
                  name="learningCycleId"
                  type="number"
                  min="1"
                  step="1"
                  required={false}
                  value={search.get("learningCycleId") ?? ""}
                />
                <Input label="Responsável pelo vínculo" name="actor" />
              </div>
              <button
                className="btn btn-outline-primary mt-3"
                disabled={change.isPending}
              >
                {change.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                Vincular ficha à execução
              </button>
            </form>
          </section>
          <section className="card card-body">
            <h2 className="h5">Consumo auditado</h2>
            <p>
              Falhas cobradas e custos desconhecidos permanecem registrados.
              Dados de teste não compõem vendas ou receita.
            </p>
            {!selected.consumption.length && (
              <p>Nenhuma reserva registrada nesta ficha.</p>
            )}
            {selected.consumption.map((c) => (
              <p key={c.id}>
                Consumo #{c.id} · {c.operationKey} · até {c.units} tentativas
                reservadas · reserva {brl(c.reservedBrl)} · realizado{" "}
                {c.actualBrl == null ? "não informado" : brl(c.actualBrl)} ·{" "}
                {c.status} · {c.testData ? "Teste segregado" : "Operação"}
                <br />
                {c.evidence}
              </p>
            ))}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                void submit(
                  {
                    consumptionId: Number(f.get("consumptionId")),
                    actualBrl: Number(f.get("actualBrl")),
                    providerReceipt: f.get("providerReceipt"),
                    reviewedBy: f.get("reviewedBy"),
                    rationale: f.get("rationale"),
                    failed: f.has("failed"),
                  },
                  "consumption-reconciliations",
                );
              }}
            >
              <h3 className="h6">Conciliar custo pendente</h3>
              <p>
                Use o comprovante oficial do provedor, nunca uma estimativa. A
                conciliação preserva as tentativas reservadas e bloqueia novas
                chamadas se houve sobrecusto.
              </p>
              <div className="row g-3">
                <Input
                  label="Número do consumo"
                  name="consumptionId"
                  type="number"
                  min="1"
                  step="1"
                />
                <Input
                  label="Custo efetivamente cobrado em reais"
                  name="actualBrl"
                  type="number"
                  min="0"
                />
                <Input
                  label="Link do comprovante oficial"
                  name="providerReceipt"
                />
                <Input label="Responsável pela conciliação" name="reviewedBy" />
                <Area
                  label="Conferência do valor e câmbio aplicado"
                  name="rationale"
                />
              </div>
              <label className="form-check mt-2">
                <input
                  className="form-check-input"
                  type="checkbox"
                  name="failed"
                />
                A tentativa terminou em falha
              </label>
              <button
                className="btn btn-outline-primary mt-3"
                disabled={change.isPending}
              >
                {change.isPending && (
                  <span className="spinner-border spinner-border-sm me-2" />
                )}
                Registrar conciliação do consumo
              </button>
            </form>
          </section>
        </>
      )}
    </div>
  );
}

function Input({
  label,
  name,
  value,
  type = "text",
  min,
  step = "0.000001",
  placeholder,
  required = true,
  disabled = false,
}: {
  label: string;
  name: string;
  value?: string | number;
  type?: string;
  min?: string;
  step?: string;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
}) {
  return (
    <label className="col-12 col-md-6">
      {label}
      {required && " *"}
      <input
        className="form-control"
        name={name}
        type={type}
        required={required}
        disabled={disabled}
        defaultValue={value}
        min={min}
        step={type === "number" ? step : undefined}
        placeholder={placeholder}
      />
    </label>
  );
}
function Area({
  label,
  name,
  value,
}: {
  label: string;
  name: string;
  value?: string;
}) {
  return (
    <label className="col-12">
      {label} *
      <textarea
        className="form-control"
        name={name}
        defaultValue={value}
        rows={3}
        required
      />
    </label>
  );
}
function Select({
  label,
  name,
  options,
  value,
}: {
  label: string;
  name: string;
  options: Array<{ code: string; name: string }>;
  value: string;
}) {
  return (
    <label className="col-12 col-md-6">
      {label} *
      <select className="form-select" name={name} defaultValue={value} required>
        <option value="">Selecione</option>
        {options.map((o) => (
          <option key={o.code} value={o.code}>
            {o.name}
          </option>
        ))}
      </select>
    </label>
  );
}
