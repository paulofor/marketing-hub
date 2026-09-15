import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import axios from "axios";
import ReactMarkdown from "react-markdown";
import PageTitle from "../../components/PageTitle";
import FinancialPlanPlutusDetails from "./FinancialPlanPlutusDetails";
import {
  costLabels,
  useFinancialPlanCatalog,
  useFinancialPlans,
  useSaveFinancialPlan,
  useAnalyzeFinancialPlan,
  type CostKey,
  type FinancialPlan,
  type PlanCatalog,
  type PlanEnvironment,
  type PlanScope,
  type SaveFinancialPlan,
  type ScenarioCode,
  type ScenarioResult,
} from "../../api/financial/useFinancialPlans";

const money = (value: number | null | undefined, currency = "BRL") =>
  value == null
    ? "Não informado"
    : new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency,
        maximumFractionDigits: 6,
      }).format(value);
const percent = (value: number | null) =>
  value == null
    ? "Indisponível"
    : `${value.toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;
const scenarioLabels: Record<ScenarioCode, string> = {
  CONSERVATIVE: "Conservador",
  BASE: "Base",
  OPTIMISTIC: "Otimista",
};
const errorMessage = (error: unknown) =>
  axios.isAxiosError(error)
    ? (error.response?.data?.detail ??
      error.response?.data?.message ??
      "Não foi possível concluir. Tente novamente após verificar a conexão.")
    : "Não foi possível concluir a operação.";

export default function FinancialPlansPage() {
  const [search, setSearch] = useSearchParams();
  const productId = Number(search.get("productId")) || undefined;
  const typeId = Number(search.get("typeId")) || undefined;
  const scope: PlanScope = productId ? "products" : "product-types";
  const ownerId = productId ?? typeId;
  const environment: PlanEnvironment =
    search.get("environment") === "TEST" ? "TEST" : "LIVE";
  const catalog = useFinancialPlanCatalog(productId);
  return (
    <main className="d-grid gap-3" style={{ minWidth: 0 }}>
      <div>
        <PageTitle>Plano financeiro</PageTitle>
        <p className="text-muted">
          Planeje a margem de cada produto e versão. Reutilize premissas por
          tipo e peça a revisão de Plutus.
        </p>
      </div>
      {catalog.isError && (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar os produtos e tipos.
        </div>
      )}
      <section className="card">
        <div className="card-body row g-3">
          <label className="col-md-5">
            Produto
            <select
              className="form-select"
              aria-label="Produto"
              value={productId ?? ""}
              disabled={catalog.isLoading}
              onChange={(e) =>
                setSearch({
                  ...(e.target.value ? { productId: e.target.value } : {}),
                  environment,
                })
              }
            >
              <option value="">Selecione o produto</option>
              {catalog.data?.products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </label>
          <label className="col-md-5">
            Modelo por tipo
            <select
              className="form-select"
              aria-label="Modelo por tipo"
              value={productId ? "" : (typeId ?? "")}
              disabled={catalog.isLoading}
              onChange={(e) =>
                setSearch({
                  ...(e.target.value ? { typeId: e.target.value } : {}),
                  environment,
                })
              }
            >
              <option value="">Selecione o tipo</option>
              {catalog.data?.productTypes.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
          <label className="col-md-2">
            Dados
            <select
              className="form-select"
              aria-label="Dados"
              value={environment}
              onChange={(e) => {
                const next = new URLSearchParams(search);
                next.set("environment", e.target.value);
                next.delete("revisionId");
                setSearch(next);
              }}
            >
              <option value="LIVE">Operação</option>
              <option value="TEST">Homologação</option>
            </select>
          </label>
        </div>
      </section>
      {catalog.isLoading && <p role="status">Carregando referências...</p>}
      {ownerId && catalog.data ? (
        <PlanWorkspace
          key={`${scope}:${ownerId}:${environment}`}
          scope={scope}
          ownerId={ownerId}
          environment={environment}
          catalog={catalog.data}
          initialRevisionId={Number(search.get("revisionId")) || undefined}
        />
      ) : (
        <div className="alert alert-info">
          Selecione um produto para planejar sua viabilidade ou um tipo para
          criar um modelo reutilizável.
        </div>
      )}
    </main>
  );
}

function PlanWorkspace({
  scope,
  ownerId,
  environment,
  catalog,
  initialRevisionId,
}: {
  scope: PlanScope;
  ownerId: number;
  environment: PlanEnvironment;
  catalog: PlanCatalog;
  initialRevisionId?: number;
}) {
  const history = useFinancialPlans(scope, ownerId, environment);
  const typeId =
    scope === "products"
      ? catalog.products.find((p) => p.id === ownerId)?.productTypeId
      : undefined;
  const templates = useFinancialPlans(
    "product-types",
    typeId ?? undefined,
    environment,
  );
  const save = useSaveFinancialPlan(scope, ownerId, environment);
  const analyze = useAnalyzeFinancialPlan(ownerId, environment);
  const [selectedId, setSelectedId] = useState(initialRevisionId);
  const [editing, setEditing] = useState(false);
  const [copy, setCopy] = useState<FinancialPlan>();
  const [notice, setNotice] = useState("");
  const selected = selectedId
    ? history.data?.find((p) => p.id === selectedId)
    : history.data?.[0];
  const busy = save.isPending || analyze.isPending;
  const startEdit = (source?: FinancialPlan) => {
    setCopy(source);
    setEditing(true);
    setNotice("");
    save.reset();
  };
  async function submit(request: SaveFinancialPlan) {
    setNotice("");
    try {
      const result = await save.mutateAsync(request);
      setSelectedId(result.id);
      setEditing(false);
      setNotice(
        "Revisão salva. Os cálculos abaixo são projeções; o plano preserva as aprovações comerciais.",
      );
    } catch {
      /* A mutation apresenta o erro sem descartar o formulário. */
    }
  }
  if (history.isLoading)
    return <p role="status">Carregando plano financeiro...</p>;
  if (history.isError)
    return (
      <div className="alert alert-danger" role="alert">
        Não foi possível carregar o plano financeiro.
      </div>
    );
  return (
    <>
      <div className="d-flex flex-wrap gap-2 align-items-center">
        <button
          className="btn btn-primary"
          disabled={busy || editing}
          onClick={() => startEdit(selected)}
        >
          {selected
            ? "Criar nova revisão"
            : scope === "products"
              ? "Criar plano do produto"
              : "Criar modelo do tipo"}
        </button>
        {scope === "products" && (
          <>
            <Link
              className="btn btn-outline-secondary"
              to={`/products/${ownerId}/execution-profiles`}
            >
              Ficha e checkpoints da cadeia
            </Link>
            <Link
              className="btn btn-outline-secondary"
              to={`/products/${ownerId}/financial`}
            >
              Consultar realizado
            </Link>
          </>
        )}
      </div>
      {scope === "products" && templates.isError && (
        <div className="alert alert-warning">
          Não foi possível carregar os modelos deste tipo.
        </div>
      )}
      {scope === "products" && !!templates.data?.length && (
        <section className="card">
          <div className="card-body">
            <h2 className="h5">Modelos do tipo deste produto</h2>
            <p className="text-muted">
              A adoção copia as premissas. Confirme preço, versão, consumo e
              fontes para este produto.
            </p>
            <div className="d-flex gap-2 flex-wrap">
              {templates.data.map((t) => (
                <button
                  key={t.id}
                  className="btn btn-outline-primary"
                  disabled={busy || editing || t.stale}
                  onClick={() => startEdit(t)}
                >
                  Usar modelo: {t.name} · revisão {t.revision}
                </button>
              ))}
            </div>
          </div>
        </section>
      )}
      {!!history.data?.length && (
        <label>
          Histórico de revisões
          <select
            className="form-select"
            aria-label="Histórico de revisões"
            value={selected?.id ?? selectedId ?? ""}
            disabled={busy || editing}
            onChange={(e) => {
              setSelectedId(Number(e.target.value));
              setNotice("");
              analyze.reset();
            }}
          >
            {history.data.map((p) => (
              <option key={p.id} value={p.id}>
                Revisão {p.revision} · {p.name} ·{" "}
                {p.assumptions.productVersion || "Modelo do tipo"}
              </option>
            ))}
          </select>
        </label>
      )}
      {selectedId && !selected && !editing && (
        <div className="alert alert-warning">
          A revisão solicitada não pertence a este histórico.
        </div>
      )}
      {notice && (
        <div className="alert alert-success" role="status">
          {notice}
        </div>
      )}
      {(save.isError || analyze.isError) && (
        <div className="alert alert-danger" role="alert">
          {errorMessage(save.error ?? analyze.error)}
        </div>
      )}
      {editing && (
        <PlanEditor
          key={copy?.id ?? "new"}
          source={copy}
          scope={scope}
          plans={catalog.commercialPlans}
          expectedRevision={history.data?.[0]?.revision ?? 0}
          busy={busy}
          onSubmit={submit}
          onCancel={() => setEditing(false)}
        />
      )}
      {selected && !editing && (
        <>
          <section className="card">
            <div className="card-body">
              <h2 className="h4">{selected.name}</h2>
              <p>
                Revisão {selected.revision} ·{" "}
                {selected.assumptions.productVersion || "Modelo por tipo"} ·{" "}
                {selected.environment === "TEST"
                  ? "Dados de homologação"
                  : "Premissas de operação"}
              </p>
              <strong>{selected.evaluation.label}</strong>
              {selected.stale && (
                <p className="text-danger mt-2">
                  Revisão precisa ser atualizada
                </p>
              )}
              <p className="small text-muted mt-2">
                Registrado por {selected.createdBy} ·{" "}
                {new Date(selected.createdAt).toLocaleString("pt-BR")} · válido
                até {selected.assumptions.validUntil}.
              </p>
              <p>
                Período: {selected.assumptions.periodDays ?? "não informado"}{" "}
                dias · preço: {money(selected.assumptions.priceBrl)} · margem
                mínima proposta:{" "}
                {percent(selected.assumptions.minimumMarginPercent)}.
              </p>
              <p>
                Incluído:{" "}
                {selected.assumptions.ai.includedUnits ?? "não informado"}{" "}
                resultados úteis · até{" "}
                {selected.assumptions.ai.maximumAttempts ?? "não informado"}{" "}
                tentativas · teto de IA por cliente:{" "}
                {money(selected.assumptions.ai.maximumCostPerCustomerBrl)}.
              </p>
              {selected.commercialPlanId && (
                <p>
                  Plano comercial #{selected.commercialPlanId} · versão{" "}
                  {selected.commercialPlanVersion}.
                </p>
              )}
              {selected.templateId && (
                <p>
                  Modelo de origem: #{selected.templateId}. A revisão foi
                  preservada.
                </p>
              )}
              <details>
                <summary>Premissas e fontes registradas</summary>
                <p style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>
                  {selected.assumptions.evidence}
                </p>
                <p>
                  IA: {selected.assumptions.ai.providerModel || "Não informado"}{" "}
                  ·{" "}
                  {money(
                    selected.assumptions.ai.perAttempt,
                    selected.assumptions.ai.currency,
                  )}{" "}
                  por tentativa.
                </p>
                <p style={{ overflowWrap: "anywhere" }}>
                  Tarifa:{" "}
                  {selected.assumptions.ai.pricingSource || "Não informada"} ·
                  conferência:{" "}
                  {selected.assumptions.ai.pricingCheckedOn || "Não informada"}.
                </p>
                {selected.assumptions.ai.currency === "USD" && (
                  <p>
                    Câmbio informado:{" "}
                    {selected.assumptions.ai.usdBrl ?? "ausente"} BRL/USD ·{" "}
                    {selected.assumptions.ai.exchangeSource || "fonte ausente"}.
                  </p>
                )}
                <dl className="row">
                  {Object.entries(costLabels).map(([key, label]) => (
                    <div className="col-md-6 mb-2" key={key}>
                      <dt>{label}</dt>
                      <dd>
                        {key.endsWith("Percent")
                          ? percent(selected.assumptions.costs[key as CostKey])
                          : money(selected.assumptions.costs[key as CostKey])}
                      </dd>
                    </div>
                  ))}
                </dl>
              </details>
            </div>
          </section>
          {(selected.evaluation.blockers.length > 0 ||
            selected.pendingActions.length > 0) && (
            <div className="alert alert-warning" role="status">
              <h2 className="h6">Pendências e responsáveis</h2>
              <ul className="mb-0">
                {[...selected.evaluation.blockers, ...selected.pendingActions]
                  .filter((v, i, all) => all.indexOf(v) === i)
                  .map((v) => (
                    <li key={v}>{v}</li>
                  ))}
              </ul>
            </div>
          )}
          {!!selected.evaluation.scenarios.length && (
            <ScenarioTable rows={selected.evaluation.scenarios} />
          )}
          <section className="card">
            <div className="card-body">
              <h2 className="h5">Parecer de Plutus</h2>
              <p>
                Revê custo, margem, limites e premissas desta revisão. O parecer
                não autoriza mídia, publicação ou cobrança.
              </p>
              {scope === "products" && (
                <button
                  className="btn btn-outline-primary"
                  disabled={busy || !selected.canRequestAnalysis}
                  onClick={() => analyze.mutate(selected.id)}
                >
                  {analyze.isPending ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" />
                      Solicitando...
                    </>
                  ) : (
                    "Solicitar parecer de Plutus"
                  )}
                </button>
              )}
              {selected.analysis ? (
                <div className="mt-3">
                  <p>
                    <strong>
                      Parecer #{selected.analysis.executionId} ·{" "}
                      {selected.analysis.status}
                    </strong>
                  </p>
                  <p>
                    Custo estimado da avaliação:{" "}
                    {money(selected.analysis.costUsd, "USD")} ·{" "}
                    {selected.analysis.model || "Modelo ainda não informado"}.
                    Este valor não foi somado automaticamente ao investimento
                    inicial.
                  </p>
                  <ReactMarkdown>
                    {selected.analysis.report ??
                      selected.analysis.result?.executiveSummary ??
                      (selected.analysis.error
                        ? "A análise registrou uma falha. Consulte o erro abaixo."
                        : "Aguardando parecer. Esta mesma solicitação será reutilizada.")}
                  </ReactMarkdown>
                  {selected.analysis.result && (
                    <FinancialPlanPlutusDetails
                      result={selected.analysis.result}
                      money={money}
                      percent={percent}
                    />
                  )}
                  {selected.analysis.error && (
                    <p role="alert" className="text-danger">
                      {selected.analysis.error}
                    </p>
                  )}
                </div>
              ) : (
                <p className="text-muted mt-2 mb-0">
                  Parecer ainda não solicitado.
                </p>
              )}
            </div>
          </section>
          <section className="alert alert-info mb-0">
            <strong>Participação na cadeia:</strong> Plutus revisa oferta,
            desenho da entrega, homologação e operação. Confira os limites na
            ficha de execução antes de adotar o plano. Criar este plano não
            conclui atividades nem altera contratos vendidos.
          </section>
        </>
      )}
    </>
  );
}

function ScenarioTable({ rows }: { rows: ScenarioResult[] }) {
  const metrics: Array<[string, (r: ScenarioResult) => string | number]> = [
    ["Clientes projetados", (r) => r.customers],
    ["Tentativas por cliente", (r) => r.attemptsPerCustomer],
    ["Receita bruta no período", (r) => money(r.grossRevenueBrl)],
    ["Receita líquida no período", (r) => money(r.netRevenueBrl)],
    ["IA por cliente/pacote", (r) => money(r.aiCostPerCustomerBrl)],
    ["IA por resultado útil", (r) => money(r.aiCostPerUsefulUnitBrl)],
    [
      "Custo variável de entrega por cliente",
      (r) => money(r.variableDeliveryPerCustomerBrl),
    ],
    [
      "Contribuição por cliente antes do CAC",
      (r) => money(r.contributionBeforeCacBrl),
    ],
    [
      "Contribuição por cliente após CAC",
      (r) => money(r.contributionAfterCacBrl),
    ],
    ["Margem após aquisição", (r) => percent(r.marginPercent)],
    [
      "CAC máximo para preservar a margem",
      (r) => money(r.maximumAffordableCacBrl),
    ],
    ["Resultado operacional do período", (r) => money(r.operatingResultBrl)],
    [
      "Resultado após investimento inicial",
      (r) => money(r.resultAfterInvestmentBrl),
    ],
    [
      "Clientes para recuperar fixos + investimento",
      (r) => r.breakEvenCustomers ?? "Indisponível",
    ],
    [
      "IA de uso / receita líquida projetada",
      (r) => percent(r.aiToNetRevenuePercent),
    ],
  ];
  return (
    <section className="card">
      <div className="card-body">
        <h2 className="h5">Cenários de viabilidade</h2>
        <p>
          Projeções por período contratado. O uso intenso aplica as premissas
          conservadoras ao limite completo de tentativas. Investimento inicial é
          descontado uma única vez.
        </p>
        <div
          className="table-responsive"
          tabIndex={0}
          role="region"
          aria-label="Comparação financeira dos cenários"
        >
          <table className="table table-striped align-middle">
            <thead>
              <tr>
                <th scope="col">Métrica projetada</th>
                {rows.map((r) => (
                  <th key={r.code} scope="col">
                    {r.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {metrics.map(([label, render]) => (
                <tr key={label}>
                  <th scope="row">{label}</th>
                  {rows.map((r) => (
                    <td key={r.code}>{render(r)}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="text-muted small mb-0">
          Resultado positivo projetado depende das premissas. Receita real,
          satisfação e entrega precisam de conciliação própria.
        </p>
      </div>
    </section>
  );
}

function PlanEditor({
  source,
  scope,
  plans,
  expectedRevision,
  busy,
  onSubmit,
  onCancel,
}: {
  source?: FinancialPlan;
  scope: PlanScope;
  plans: PlanCatalog["commercialPlans"];
  expectedRevision: number;
  busy: boolean;
  onSubmit: (request: SaveFinancialPlan) => Promise<void>;
  onCancel: () => void;
}) {
  const a = source?.assumptions;
  const copyingType = scope === "products" && source?.scope === "TYPE";
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const text = (name: string) => String(data.get(name) ?? "").trim();
    const number = (name: string) =>
      text(name) === "" ? null : Number(text(name));
    void onSubmit({
      name: text("name"),
      createdBy: text("createdBy"),
      expectedRevision,
      commercialPlanId:
        scope === "products" ? number("commercialPlanId") : null,
      templateId: copyingType ? source!.id : (source?.templateId ?? null),
      assumptions: {
        productVersion: text("productVersion") || null,
        periodDays: number("periodDays"),
        validUntil: text("validUntil"),
        evidence: text("evidence"),
        priceBrl: number("priceBrl"),
        minimumMarginPercent: number("minimumMarginPercent"),
        maximumCacBrl: number("maximumCacBrl"),
        ai: {
          currency: text("currency") as "BRL" | "USD",
          providerModel: text("providerModel") || null,
          perAttempt: number("perAttempt"),
          pricingSource: text("pricingSource") || null,
          pricingCheckedOn: text("pricingCheckedOn") || null,
          usdBrl: number("usdBrl"),
          exchangeSource: text("exchangeSource") || null,
          includedUnits: number("includedUnits"),
          maximumAttempts: number("maximumAttempts"),
          maximumCostPerCustomerBrl: number("maximumCostPerCustomerBrl"),
        },
        costs: Object.fromEntries(
          Object.keys(costLabels).map((k) => [k, number(k)]),
        ) as SaveFinancialPlan["assumptions"]["costs"],
        scenarios: (Object.keys(scenarioLabels) as ScenarioCode[]).map(
          (code) => ({
            code,
            customers: number(`${code}-customers`),
            attemptsPerCustomer: number(`${code}-attemptsPerCustomer`),
            cacBrl: number(`${code}-cacBrl`),
          }),
        ),
      },
    });
  }
  const field = (
    name: string,
    label: string,
    value?: string | number | null,
    type = "number",
    required = false,
    max?: number,
  ) => (
    <label className="col-md-6 col-xl-4" key={name}>
      {label}
      {required ? " *" : ""}
      <input
        className="form-control"
        name={name}
        aria-label={label}
        type={type}
        step={type === "number" ? "any" : undefined}
        min={type === "number" ? 0 : undefined}
        max={max}
        maxLength={type === "text" ? 1000 : undefined}
        defaultValue={value ?? ""}
        required={required}
      />
    </label>
  );
  return (
    <form onSubmit={submit} className="card">
      <fieldset className="card-body d-grid gap-4" disabled={busy}>
        <div>
          <h2 className="h5">Premissas da nova revisão</h2>
          <p className="text-muted mb-0">
            Valores numéricos em branco ficam como pendências. Informe zero
            somente quando houver justificativa. Todas as estimativas devem usar
            o mesmo período e pacote.
          </p>
        </div>
        <div className="row g-3">
          {field("name", "Nome do plano", source?.name, "text", true)}
          {field(
            "createdBy",
            "Responsável pelo registro",
            undefined,
            "text",
            true,
          )}
          {scope === "products" &&
            field(
              "productVersion",
              "Versão do produto",
              copyingType ? "" : a?.productVersion,
              "text",
              true,
            )}
          {scope === "products" && (
            <label className="col-md-6 col-xl-4">
              Plano comercial *
              <select
                className="form-select"
                name="commercialPlanId"
                aria-label="Plano comercial"
                required
                defaultValue={
                  copyingType ? "" : (source?.commercialPlanId ?? "")
                }
              >
                <option value="">Selecione</option>
                {plans.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>
          )}
          {field("periodDays", "Período contratado (dias)", a?.periodDays)}
          {field(
            "validUntil",
            "Validade das premissas",
            a?.validUntil,
            "date",
            true,
          )}
          {field("priceBrl", "Preço por cliente/pacote (R$)", a?.priceBrl)}
          {field(
            "minimumMarginPercent",
            "Margem mínima proposta (%)",
            a?.minimumMarginPercent,
            "number",
            false,
            99.99,
          )}
          {field("maximumCacBrl", "CAC máximo proposto (R$)", a?.maximumCacBrl)}
        </div>
        {scope === "products" && !plans.length && (
          <p className="text-warning">
            Este produto precisa de um{" "}
            <Link to="/planning">plano comercial</Link> para receber um plano
            financeiro.
          </p>
        )}
        <section>
          <h3 className="h6">IA e limites da entrega</h3>
          <p className="small">
            Uma tentativa corresponde à produção de um resultado: some o custo
            das chamadas necessárias. Para lotes, rateie o custo e conte as
            tentativas por resultado. Registre resolução e qualidade nas fontes.
            Inclua falhas cobradas e regenerações. Para entrega sem IA variável,
            declare modelo sem IA, tarifa e tentativas zero, explicando a
            premissa.
          </p>
          <div className="row g-3">
            {field(
              "providerModel",
              "Provedor e modelo",
              a?.ai.providerModel,
              "text",
            )}
            <label className="col-md-6 col-xl-4">
              Moeda da tarifa de IA *
              <select
                name="currency"
                className="form-select"
                aria-label="Moeda da tarifa de IA"
                defaultValue={a?.ai.currency ?? "BRL"}
              >
                <option value="BRL">BRL</option>
                <option value="USD">USD</option>
              </select>
            </label>
            {field(
              "perAttempt",
              "Custo por tentativa na moeda informada",
              a?.ai.perAttempt,
            )}
            {field(
              "pricingSource",
              "Fonte da tarifa / justificativa sem IA",
              a?.ai.pricingSource,
              "text",
            )}
            {field(
              "pricingCheckedOn",
              "Data da conferência da tarifa",
              a?.ai.pricingCheckedOn,
              "date",
            )}
            {field("usdBrl", "Câmbio BRL por USD (se aplicável)", a?.ai.usdBrl)}
            {field(
              "exchangeSource",
              "Fonte e data do câmbio",
              a?.ai.exchangeSource,
              "text",
            )}
            {field(
              "includedUnits",
              "Resultados úteis incluídos",
              a?.ai.includedUnits,
            )}
            {field(
              "maximumAttempts",
              "Máximo de tentativas por cliente",
              a?.ai.maximumAttempts,
            )}
            {field(
              "maximumCostPerCustomerBrl",
              "Teto de IA por cliente (R$)",
              a?.ai.maximumCostPerCustomerBrl,
            )}
          </div>
        </section>
        <section>
          <h3 className="h6">Custos e deduções</h3>
          <p className="small">
            Percentuais incidem sobre o preço bruto. Não repita deduções em
            outros custos. Investimento inicial inclui criação, testes e
            agentes; recarga de créditos não é consumo.
          </p>
          <div className="row g-3">
            {(Object.keys(costLabels) as CostKey[]).map((k) =>
              field(
                k,
                costLabels[k],
                a?.costs[k],
                "number",
                false,
                k.endsWith("Percent") ? 100 : undefined,
              ),
            )}
          </div>
        </section>
        <section>
          <h3 className="h6">Cenários no período contratado</h3>
          {(Object.entries(scenarioLabels) as [ScenarioCode, string][]).map(
            ([code, label]) => {
              const s = a?.scenarios.find((s) => s.code === code);
              return (
                <div key={code} className="border rounded p-3 mb-2">
                  <h4 className="h6">{label}</h4>
                  <div className="row g-3">
                    {field(
                      `${code}-customers`,
                      `${label}: clientes projetados`,
                      s?.customers,
                    )}
                    {field(
                      `${code}-attemptsPerCustomer`,
                      `${label}: tentativas por cliente`,
                      s?.attemptsPerCustomer,
                    )}
                    {field(`${code}-cacBrl`, `${label}: CAC (R$)`, s?.cacBrl)}
                  </div>
                </div>
              );
            },
          )}
        </section>
        <label>
          Premissas, fontes e justificativa dos valores zero *
          <textarea
            name="evidence"
            aria-label="Premissas, fontes e justificativa dos valores zero"
            className="form-control"
            rows={5}
            required
            maxLength={6000}
            defaultValue={a?.evidence ?? ""}
          />
        </label>
        <div className="d-flex gap-2 flex-wrap">
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" />
                Salvando...
              </>
            ) : (
              "Salvar revisão e calcular"
            )}
          </button>
          <button
            className="btn btn-outline-secondary"
            type="button"
            disabled={busy}
            onClick={onCancel}
          >
            Cancelar edição
          </button>
        </div>
      </fieldset>
    </form>
  );
}
