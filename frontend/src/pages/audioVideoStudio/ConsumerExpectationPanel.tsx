import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Link } from "react-router-dom";

type Protocol = {
  contractVersion: string;
  purpose: string;
  axes: string[];
  discovery: string[];
  limits: string[];
  sources: {
    id: string;
    title: string;
    url: string;
    design: string;
    finding: string;
    limits: string;
    access: string;
  }[];
};
type Plan = {
  fingerprint: string;
  status: string;
  variants: {
    code: string;
    label: string;
    owner: string;
    concept: string;
    familiarAnchor: string;
    change: string;
    comprehensionCheck: string;
    hypothesis: string;
    risk: string;
  }[];
  gaps: string[];
  validation: string[];
  metrics: string[];
  persistence: string;
};
type Learning = {
  productId: number;
  cycleId: number;
  experimentId: number;
  productVersion: string;
  status: string;
  measurementEventId: number | null;
  observedAt: string | null;
  metrics: Record<string, unknown>;
  sourceFingerprint: string | null;
  nextOwner: string;
  guidance: string;
  cycleUrl: string;
};
const base = "/api/consumer-expectations/v1";
const labels: Record<string, string> = {
  humanVisitors: "Visitantes humanos",
  netSales: "Compras líquidas",
  refunds: "Reembolsos",
  spendBrl: "Aquisição (R$)",
  revenueBrl: "Receita líquida (R$)",
  contributionBrl: "Contribuição (R$)",
  deliveryVerified: "Entrega conciliada",
  useVerified: "Uso conciliado",
  periodStart: "Início da medição",
  periodEnd: "Fim da medição",
  measurementLimitations: "Limites da medição",
};

export default function ConsumerExpectationPanel() {
  const protocol = useQuery({
    queryKey: ["consumer-expectation-protocol", "v1"],
    queryFn: async () => (await axios.get<Protocol>(`${base}/protocol`)).data,
  });
  const [input, setInput] = useState({
    audience: "",
    context: "",
    surface: "AD",
    expectedExperience: "",
    desiredOutcome: "",
    actualDelivery: "",
    evidenceBasis: "HYPOTHESIS",
    evidenceReference: "",
    deliveryProofReference: "",
  });
  const [productId, setProductId] = useState("");
  const [cycleId, setCycleId] = useState("");
  const plan = useMutation({
    mutationFn: async () =>
      (await axios.post<Plan>(`${base}/plans/preview`, input)).data,
  });
  const learning = useMutation({
    mutationFn: async () =>
      (
        await axios.get<Learning>(
          `${base}/products/${productId}/cycles/${cycleId}/learning`,
        )
      ).data,
  });
  const fields = [
    ["audience", "Público", 500],
    ["context", "Situação e canal", 500],
    ["expectedExperience", "O que a pessoa espera", 1000],
    ["desiredOutcome", "O que ela deseja alcançar", 1000],
    ["actualDelivery", "O que o produto realmente entrega", 1000],
    ["evidenceReference", "Fonte da expectativa (referência e data)", 1200],
    ["deliveryProofReference", "Referência da prova funcional", 1200],
  ] as const;
  return (
    <section
      className="audio-video-studio-page__section"
      aria-labelledby="expectation-title"
    >
      <h2 id="expectation-title">Expectativas e surpresa positiva</h2>
      <p>
        Prepare hipóteses para anúncios, ofertas, produtos e experiências. A
        ferramenta não gera ativos finais, não salva a prévia e não inicia
        campanhas ou tarefas pagas.
      </p>
      {protocol.isLoading && <p role="status">Carregando referências...</p>}
      {protocol.isError && (
        <p role="alert">
          Não foi possível carregar as referências. Tente novamente mais tarde.
        </p>
      )}
      {protocol.data && (
        <details>
          <summary>Base científica, dimensões e limites</summary>
          <p>{protocol.data.purpose}</p>
          <ul>
            {protocol.data.axes.map((axis) => (
              <li key={axis}>{axis}</li>
            ))}
          </ul>
          <ul>
            {protocol.data.discovery.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          {protocol.data.sources.map((source) => (
            <article key={source.id} className="mb-3">
              <a href={source.url} target="_blank" rel="noreferrer">
                {source.title}
              </a>
              <p>
                {source.design}: {source.finding}
              </p>
              <p>Limite: {source.limits}</p>
              <small>{source.access}</small>
            </article>
          ))}
          <ul>
            {protocol.data.limits.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </details>
      )}
      <form
        onSubmit={(event) => {
          event.preventDefault();
          plan.mutate();
        }}
        className="row g-3 mt-1"
      >
        <fieldset disabled={plan.isPending} className="row g-3">
          {fields.map(([name, label, max]) => (
            <div className="col-12 col-md-6" key={name}>
              <label className="form-label" htmlFor={`expectation-${name}`}>
                {label}
                {!name.endsWith("Reference") ? " *" : ""}
              </label>
              <textarea
                className="form-control"
                id={`expectation-${name}`}
                required={!name.endsWith("Reference")}
                maxLength={max}
                value={input[name]}
                onChange={(event) => {
                  plan.reset();
                  setInput({ ...input, [name]: event.target.value });
                }}
              />
            </div>
          ))}
          <div className="col-12 col-md-6">
            <label htmlFor="expectation-surface" className="form-label">
              Onde aplicar *
            </label>
            <select
              id="expectation-surface"
              className="form-select"
              value={input.surface}
              onChange={(e) => {
                plan.reset();
                setInput({ ...input, surface: e.target.value });
              }}
            >
              <option value="AD">Propaganda</option>
              <option value="OFFER">Oferta</option>
              <option value="PRODUCT">Produto</option>
              <option value="DIGITAL_EXPERIENCE">Experiência digital</option>
            </select>
          </div>
          <div className="col-12 col-md-6">
            <label htmlFor="expectation-basis" className="form-label">
              Origem da expectativa *
            </label>
            <select
              id="expectation-basis"
              className="form-select"
              value={input.evidenceBasis}
              onChange={(e) => {
                plan.reset();
                setInput({ ...input, evidenceBasis: e.target.value });
              }}
            >
              <option value="HYPOTHESIS">Hipótese ainda não observada</option>
              <option value="PUBLIC_SOURCE">Fonte pública</option>
              <option value="VOLUNTARY_BEHAVIOR">
                Comportamento voluntário registrado
              </option>
            </select>
          </div>
          <div className="col-12">
            <button
              type="submit"
              className="btn btn-primary"
              disabled={plan.isPending || !protocol.data}
            >
              {plan.isPending ? (
                <>
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />{" "}
                  Preparando...
                </>
              ) : (
                "Preparar três alternativas"
              )}
            </button>
          </div>
        </fieldset>
      </form>
      {plan.isError && (
        <p role="alert">
          Não foi possível preparar a prévia. Confira os campos e tente
          novamente.
        </p>
      )}
      {plan.data && (
        <div aria-live="polite" className="mt-3">
          <h3>Hipóteses para revisão</h3>
          <p>{plan.data.persistence}</p>
          {plan.data.variants.map((variant) => (
            <article key={variant.code} className="border rounded p-3 mb-3">
              <h4>{variant.label}</h4>
              <p>{variant.concept}</p>
              <p>Referência familiar: {variant.familiarAnchor}</p>
              <p>{variant.change}</p>
              <p>{variant.hypothesis}</p>
              <p>Conferir: {variant.comprehensionCheck}</p>
              <p>Risco: {variant.risk}</p>
              <small>Responsável: {variant.owner}</small>
            </article>
          ))}
          <h4>Pendências de evidência</h4>
          <ul>
            {plan.data.gaps.map((gap) => (
              <li key={gap}>{gap}</li>
            ))}
          </ul>
          <h4>Como testar e medir</h4>
          <ul>
            {[...plan.data.validation, ...plan.data.metrics].map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          <details>
            <summary>Identificador reproduzível da prévia</summary>
            <code style={{ overflowWrap: "anywhere" }}>
              {plan.data.fingerprint}
            </code>
          </details>
        </div>
      )}
      <h3 className="mt-4">Aprender com um ciclo existente</h3>
      <p>
        Consulta somente leitura. Os números vêm da conciliação do ciclo; não
        são estimados a partir de atenção ou surpresa.
      </p>
      <form
        className="row g-3"
        onSubmit={(e) => {
          e.preventDefault();
          learning.mutate();
        }}
      >
        <div className="col-12 col-md-4">
          <label htmlFor="expectation-product" className="form-label">
            ID do produto *
          </label>
          <input
            id="expectation-product"
            type="number"
            min="1"
            step="1"
            required
            className="form-control"
            value={productId}
            disabled={learning.isPending}
            onChange={(e) => {
              learning.reset();
              setProductId(e.target.value);
            }}
          />
        </div>
        <div className="col-12 col-md-4">
          <label htmlFor="expectation-cycle" className="form-label">
            ID do ciclo *
          </label>
          <input
            id="expectation-cycle"
            type="number"
            min="1"
            step="1"
            required
            className="form-control"
            value={cycleId}
            disabled={learning.isPending}
            onChange={(e) => {
              learning.reset();
              setCycleId(e.target.value);
            }}
          />
        </div>
        <div className="col-12">
          <button
            className="btn btn-outline-primary"
            type="submit"
            disabled={learning.isPending}
          >
            {learning.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm"
                  aria-hidden="true"
                />{" "}
                Consultando...
              </>
            ) : (
              "Consultar aprendizado"
            )}
          </button>
        </div>
      </form>
      {learning.isError && (
        <p role="alert">
          Consulta indisponível ou ciclo não encontrado neste produto. Nenhum
          dado foi considerado zero.
        </p>
      )}
      {learning.data && (
        <div aria-live="polite" className="mt-3">
          <p>
            Produto {learning.data.productId} · ciclo {learning.data.cycleId} ·
            experimento {learning.data.experimentId} · versão{" "}
            {learning.data.productVersion}
          </p>
          <p>{learning.data.guidance}</p>
          <p>Responsável: {learning.data.nextOwner}</p>
          <dl>
            {Object.entries(learning.data.metrics).map(([key, value]) => (
              <div key={key}>
                <dt>{labels[key] ?? key}</dt>
                <dd>
                  {value == null
                    ? "Desconhecido"
                    : typeof value === "boolean"
                      ? value
                        ? "Sim"
                        : "Não comprovado"
                      : Array.isArray(value)
                        ? value.join("; ")
                        : String(value)}
                </dd>
              </div>
            ))}
          </dl>
          {learning.data.observedAt && (
            <p>
              Recibo {learning.data.measurementEventId} registrado em{" "}
              {learning.data.observedAt}.
            </p>
          )}
          <Link to={learning.data.cycleUrl}>
            Abrir o ciclo e suas evidências
          </Link>
        </div>
      )}
    </section>
  );
}
