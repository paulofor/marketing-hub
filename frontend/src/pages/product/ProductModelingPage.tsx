import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useProducts } from "../../api/product/useProducts";
import {
  useModelingAction,
  useProductModelings,
  type ProductModeling,
} from "../../api/product/useProductModeling";

const steps = [
  [
    "01",
    "Investigar",
    "Argos",
    "Sinais no exterior e alternativas brasileiras",
  ],
  ["02", "Adaptar ao Brasil", "Atena", "Três vantagens próprias e Big Idea"],
  ["03", "Conferir economia", "Plutus", "Custos e contribuição projetada"],
  ["04", "Construir", "Dédalo", "Experiência funcionando e prova"],
  [
    "05",
    "Comunicar e revisar",
    "Íris · Apolo · Psique · Têmis",
    "Benefício real e revisão independente",
  ],
  [
    "06",
    "Testar e aprender",
    "Hermes · Atena",
    "Compras, entrega e contribuição",
  ],
];
const statuses: Record<string, string> = {
  DRAFT: "Preparada",
  READY_FOR_RESEARCH: "Na fila de Argos",
  RESEARCHING: "Argos pesquisando",
  COMPLETED: "Pesquisa concluída",
  FAILED: "Pesquisa interrompida",
  AWAITING_CUSTOMER_EVIDENCE: "Aprofundamento pendente",
  ARCHIVED: "Arquivada",
};

function ModelingCard({ item }: { item: ProductModeling }) {
  const action = useModelingAction();
  const [confirm, setConfirm] = useState(false);
  return (
    <article className="card mb-3">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between gap-2">
          <h2 className="h5">{item.productName}</h2>
          <span className="badge bg-light text-dark border">
            {statuses[item.status] ?? item.status}
          </span>
        </div>
        <p>{item.desiredOutcome}</p>
        {item.researchScope === "INTERNATIONAL_TO_BR_V1" && (
          <p className="small text-primary">
            Referências internacionais → {item.targetMarket}
          </p>
        )}
        <p className="text-secondary">{item.guidance}</p>
        {item.status === "FAILED" && item.researchRecovery && (
          <p>{item.researchRecovery.reason}</p>
        )}
        <div className="d-flex flex-wrap gap-3">
          {item.referenceUrls.map((url) => (
            <a key={url} href={url} target="_blank" rel="noopener noreferrer">
              {new URL(url).hostname}
            </a>
          ))}
        </div>
        <div className="d-flex flex-wrap gap-2 mt-3">
          <Link className="btn btn-outline-primary" to={item.researchUrl}>
            Ver fontes e percurso
          </Link>
          {item.productUrl && (
            <Link className="btn btn-outline-secondary" to={item.productUrl}>
              Big Idea do produto
            </Link>
          )}
          {item.status === "DRAFT" && (
            <button
              className="btn btn-primary"
              onClick={() => setConfirm(!confirm)}
            >
              Preparar início da pesquisa
            </button>
          )}
          {item.researchRecovery?.available && (
            <button
              className="btn btn-primary"
              onClick={() => setConfirm(!confirm)}
            >
              Retomar pesquisa com Argos
            </button>
          )}
        </div>
        {confirm && (
          <div className="alert alert-warning mt-3">
            <p>
              A pesquisa usa os agentes e pode consumir IA. Confira a
              autorização e os limites deste produto antes de iniciar. Não
              autoriza mídia nem publicação.
            </p>
            <button
              className="btn btn-primary"
              disabled={action.isPending}
              onClick={() =>
                action.mutate(
                  item.researchRecovery?.available
                    ? { resume: item.id }
                    : { start: item.id },
                  { onSuccess: () => setConfirm(false) },
                )
              }
            >
              {action.isPending && (
                <span className="spinner-border spinner-border-sm me-2" />
              )}
              {item.researchRecovery?.available
                ? "Confirmar retomada da pesquisa"
                : "Iniciar pesquisa com Argos"}
            </button>
          </div>
        )}
        {item.chainUrl && (
          <Link className="btn btn-success mt-3" to={item.chainUrl}>
            Continuar na cadeia · ciclo #{item.learningCycleId}
          </Link>
        )}
        {item.status === "COMPLETED" && !item.chainUrl && item.productId && (
          <div className="mt-3">
            <h3 className="h6">Levar as fontes à cadeia</h3>
            {item.availableCycles.length ? (
              item.availableCycles.map((c) => (
                <button
                  key={c.cycleId}
                  className="btn btn-outline-primary me-2"
                  disabled={action.isPending}
                  onClick={() =>
                    action.mutate({ adopt: item.id, cycleId: c.cycleId })
                  }
                >
                  {action.isPending && (
                    <span className="spinner-border spinner-border-sm me-2" />
                  )}
                  Usar no ciclo #{c.cycleId} · {c.productVersion}
                </button>
              ))
            ) : (
              <p>
                Confira as fontes e uma preparação ainda sem execução para
                vincular este estudo. O histórico permanece preservado.
              </p>
            )}
            <small className="d-block text-secondary">
              A vinculação não inicia agentes nem transfere autorizações. O
              backend mantém planejamento, construção, revisões e ativação no
              ciclo escolhido.
            </small>
          </div>
        )}
        {action.isError && (
          <div role="alert" className="alert alert-danger mt-3">
            Não foi possível iniciar. Confira o estado e tente novamente sem
            criar outra preparação.
          </div>
        )}
        {item.stages.length > 0 && (
          <div className="mt-3">
            <h3 className="h6">Passagens registradas</h3>
            {item.stages.map((stage) => (
              <div key={stage.taskId} className="border-top py-2">
                <strong>{stage.agent}</strong> · {stage.activity} ·{" "}
                {stage.status}
                {stage.resultSummary && <p>{stage.resultSummary}</p>}
                {stage.activityUrl && (
                  <Link to={stage.activityUrl}>Abrir pesquisa e entrega</Link>
                )}
              </div>
            ))}
          </div>
        )}
        {item.internationalAssessment?.brazilAdaptation && (
          <section
            className="mt-3 p-3 rounded bg-light"
            aria-label="Adaptação ao Brasil"
          >
            <h3 className="h6">Nossa oportunidade no Brasil</h3>
            {Object.entries({
              recognizedDesire: "Desejo da cliente",
              firstValue: "Primeiro valor",
              localization: "Adaptação à rotina brasileira",
              localAlternatives: "Alternativas locais",
              paidValue: "Por que pagar",
              economicsHypotheses: "Economia projetada",
              mainRisk: "Principal risco",
              validationPlan: "Como validar",
            }).map(([key, label]) => (
              <p key={key}>
                <strong>{label}:</strong>{" "}
                {item.internationalAssessment?.brazilAdaptation[key]}
              </p>
            ))}
            <details>
              <summary>Conferir evidências internacionais e limites</summary>
              {item.internationalAssessment.references.map((reference) => (
                <div key={reference.referenceUrl} className="border-top py-2">
                  <a
                    href={reference.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Fonte · {reference.originMarket}
                  </a>
                  <p>{reference.observedEvidence}</p>
                  <p className="small text-secondary">{reference.limitation}</p>
                </div>
              ))}
            </details>
          </section>
        )}
        <details className="mt-3">
          <summary>Contexto preservado</summary>
          <p>
            Modelagem #{item.id} · Pesquisa #{item.discoveryCycleId}. Fontes →
            aprendizado → decisão → entrega → resultado.
          </p>
          <p>
            {item.bigIdeaSnapshot?.centralIdea ??
              "Big Idea ainda não definida. Atena deverá propor a direção na estratégia."}
          </p>
          <p>
            Concorrência oferece sinais; vendas e margem do nosso produto
            continuam por comprovar.
          </p>
        </details>
      </div>
    </article>
  );
}
export default function ProductModelingPage() {
  const [params] = useSearchParams();
  const [productId, setProductId] = useState(params.get("productId") ?? "");
  const [name, setName] = useState("");
  const [outcome, setOutcome] = useState("");
  const [urls, setUrls] = useState("");
  const [requestKey, setRequestKey] = useState(() => createCycleRequestKey());
  const products = useProducts();
  const models = useProductModelings();
  const save = useModelingAction();
  const edit = () => {
    setRequestKey(createCycleRequestKey());
    save.reset();
  };
  return (
    <main className="container py-4" style={{ maxWidth: 1120 }}>
      <header
        className="p-4 mb-4 rounded-4"
        style={{ background: "linear-gradient(120deg,#e8f3ff,#e9f8f0)" }}
      >
        <span className="badge bg-primary mb-2">
          Do mercado internacional para o Brasil
        </span>
        <h1>Modelar por referências</h1>
        <p className="mb-0">
          Investigue produtos que já conquistaram público no exterior e
          transforme esse aprendizado em uma experiência própria para a cliente
          brasileira.
        </p>
      </header>
      <div className="row g-2 mb-4">
        {steps.map(([n, title, agent, output]) => (
          <div key={n} className="col-12 col-sm-6 col-lg-4">
            <div className="border rounded p-3 h-100">
              <span className="text-primary fw-bold">{n}</span>
              <h2 className="h6 mt-2">{title}</h2>
              <small>
                {agent}
                <br />
                <span className="text-secondary">{output}</span>
              </small>
            </div>
          </div>
        ))}
      </div>
      <p className="text-secondary">
        Argos confere sinais de adoção e monetização, fontes e limites. Atena
        propõe a adaptação ao Brasil. O sucesso aqui será medido por compras,
        entrega útil e contribuição — a existência de concorrentes é um ponto de
        partida.
      </p>
      <form
        className="card mb-4"
        onSubmit={(event) => {
          event.preventDefault();
          save.mutate({
            requestKey,
            productId: productId ? Number(productId) : null,
            newProductName: productId ? null : name,
            desiredOutcome: outcome,
            referenceUrls: urls
              .split(/\n/)
              .map((x) => x.trim())
              .filter(Boolean),
          });
        }}
      >
        <div className="card-body">
          <h2 className="h5">Preparar uma modelagem</h2>
          <label className="form-label" htmlFor="model-product">
            Produto *
          </label>
          <select
            className="form-select mb-3"
            id="model-product"
            value={productId}
            onChange={(e) => {
              setProductId(e.target.value);
              edit();
            }}
          >
            <option value="">Novo produto</option>
            {products.data?.map((p) => (
              <option key={p.id} value={p.id}>
                {p.internalName || p.name}
              </option>
            ))}
          </select>
          {!productId && (
            <>
              <label className="form-label" htmlFor="model-name">
                Nome provisório *
              </label>
              <input
                id="model-name"
                className="form-control mb-3"
                required
                maxLength={191}
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  edit();
                }}
              />
            </>
          )}
          <label className="form-label" htmlFor="model-outcome">
            O que a cliente deseja conseguir? *
          </label>
          <textarea
            id="model-outcome"
            className="form-control mb-3"
            required
            maxLength={2000}
            rows={3}
            value={outcome}
            onChange={(e) => {
              setOutcome(e.target.value);
              edit();
            }}
            placeholder="Um resultado concreto, com menos esforço e uma experiência melhor."
          />
          <label className="form-label" htmlFor="model-urls">
            Links das referências *
          </label>
          <textarea
            id="model-urls"
            className="form-control"
            required
            rows={3}
            value={urls}
            onChange={(e) => {
              setUrls(e.target.value);
              edit();
            }}
            placeholder="https://www.exemplo.com/"
          />
          <small className="text-secondary">
            Uma a cinco páginas públicas HTTPS, uma por linha. Priorize
            referências internacionais; o agente também pesquisa alternativas
            brasileiras. Não informe credenciais ou links privados.
          </small>
          <p className="mt-3">
            Salvar preserva a intenção e a Big Idea atual. Não inicia pesquisa,
            muda experimentos ou autoriza gastos.
          </p>
          <button
            className="btn btn-primary"
            disabled={save.isPending || save.isSuccess}
          >
            {save.isPending && (
              <span className="spinner-border spinner-border-sm me-2" />
            )}
            Salvar preparação
          </button>
          {save.isSuccess && (
            <div role="status" className="alert alert-success mt-3">
              Preparação salva. Veja o percurso abaixo.
            </div>
          )}
          {save.isError && (
            <div role="alert" className="alert alert-danger mt-3">
              Não foi possível salvar. Confira os campos e use URLs públicas
              HTTPS sem parâmetros.
            </div>
          )}
        </div>
      </form>
      <h2 className="h4">Modelagens e entregas</h2>
      {models.isLoading && <p role="status">Carregando preparações…</p>}
      {models.isError && (
        <p role="alert">Não foi possível consultar as modelagens.</p>
      )}
      {models.data?.length === 0 && <p>Nenhuma preparação registrada.</p>}
      {models.data?.map((item) => <ModelingCard key={item.id} item={item} />)}
    </main>
  );
}
