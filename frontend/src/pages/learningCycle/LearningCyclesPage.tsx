import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useProducts } from "../../api/product/useProducts";
import { useBusinessProcessChains } from "../../api/businessProcessChain/useBusinessProcessChains";
import {
  useCycleCatalog,
  useLearningCycles,
  type CycleEvent,
  type LearningCycle,
} from "../../api/learningCycle/useLearningCycles";
import PageTitle from "../../components/PageTitle";
import LearningCycleCreateForm from "./LearningCycleCreateForm";
import LearningCycleCommandForm from "./LearningCycleCommandForm";
import LearningCycleAutomaticMeasurement from "./LearningCycleAutomaticMeasurement";
import LearningCycleDiagram from "./LearningCycleDiagram";
import "./LearningCyclesPage.css";

const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});
const date = (value: string) => new Date(value).toLocaleString("pt-BR");
const statuses: Record<string, string> = {
  OPEN: "Em andamento",
  ADJUSTED: "Encerrado para ajuste",
  CLOSED: "Encerrado",
  INCONCLUSIVE: "Inconclusivo",
};
function Audit({ events }: { events: CycleEvent[] }) {
  return (
    <ol className="cycle-audit">
      {events.map((event) => (
        <li key={event.id}>
          <strong>{event.summary}</strong>
          <div className="small">
            {date(event.createdAt)} · {event.operatorName}
          </div>
          <div>Fonte: {event.evidenceReference}</div>
          {event.action === "MEASURE" ? (
            <div>
              <p
                className={
                  event.evidence.dataValid === true &&
                  event.evidence.testDataExcluded === true
                    ? "text-success"
                    : "text-danger"
                }
              >
                {event.evidence.dataValid === true &&
                event.evidence.testDataExcluded === true
                  ? event.evidence.automatic === true
                    ? "Dados reconciliados automaticamente e segregados."
                    : "Dados declarados válidos e segregados."
                  : "Medição pendente de correção; não usar para concluir desempenho comercial."}
              </p>
              <p>
                {event.evidence.automatic === true
                  ? "Leitura automática das fontes oficiais: "
                  : "Leitura declarada pelo operador: "}
                {String(event.evidence.sessions)} sessões ·{" "}
                {String(event.evidence.netSales)} vendas líquidas ·{" "}
                {money.format(Number(event.evidence.revenueBrl))} de receita ·{" "}
                {money.format(Number(event.evidence.contributionBrl))} de
                contribuição.
              </p>
              <p className="small">
                Período: {String(event.evidence.periodStart)} a{" "}
                {String(event.evidence.periodEnd)} · fonte:{" "}
                {String(event.evidence.source)}.
              </p>
            </div>
          ) : null}
          {event.action === "MEASUREMENT_BLOCKED" ? (
            <p className="text-danger">
              Fonte bloqueada: {String(event.evidence.blocker ?? event.summary)}
            </p>
          ) : null}
          {event.evidence.learning ? (
            <p>Aprendizado: {String(event.evidence.learning)}</p>
          ) : null}
          {event.evidence.rootCause ? (
            <p>Causa: {String(event.evidence.rootCause)}</p>
          ) : null}
          {event.evidence.nextHypothesis ? (
            <p>Próxima hipótese: {String(event.evidence.nextHypothesis)}</p>
          ) : null}
        </li>
      ))}
    </ol>
  );
}
export default function LearningCyclesPage() {
  const [params, setParams] = useSearchParams();
  const products = useProducts();
  const chains = useBusinessProcessChains();
  const productId = Number(params.get("productId")) || undefined;
  const explicitCycle = params.has("cycleId");
  const selectedId = Number(params.get("cycleId"));
  const requestedChainId = Number(params.get("chainId")) || undefined;
  const queryChainId =
    requestedChainId || (!explicitCycle ? chains.data?.[0]?.id : undefined);
  const cycles = useLearningCycles(productId, queryChainId);
  const [creating, setCreating] = useState(false);
  const [predecessor, setPredecessor] = useState<LearningCycle>();
  const cycle = explicitCycle
    ? cycles.data?.find((item) => item.id === selectedId)
    : cycles.data?.[0];
  const chainId =
    requestedChainId ||
    (explicitCycle ? cycle?.chainDefinitionId : queryChainId);
  const catalog = useCycleCatalog(chainId, productId);
  function select(key: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(key, value);
    if (key !== "cycleId") next.delete("cycleId");
    else {
      const selected = cycles.data?.find((item) => item.id === Number(value));
      if (selected) next.set("chainId", String(selected.chainDefinitionId));
    }
    setParams(next);
    setCreating(false);
    setPredecessor(undefined);
  }
  function updated(value: LearningCycle) {
    const next = new URLSearchParams(params);
    next.set("cycleId", String(value.id));
    next.set("chainId", String(value.chainDefinitionId));
    setParams(next);
    setCreating(false);
    setPredecessor(undefined);
  }
  return (
    <div className="learning-cycles-page">
      {catalog.data?.entry?.activitySequenceNumber ? (
        <Link
          to={catalog.data.entry.parentUrl}
          className="btn btn-primary me-2 mb-3"
        >
          Voltar à atividade {catalog.data.entry.activitySequenceNumber} do
          Processo {catalog.data.entry.sequenceNumber}
        </Link>
      ) : null}
      <Link
        to={`/business-process-chains${chainId ? `?chainId=${chainId}${productId ? `&productId=${productId}` : ""}` : ""}`}
        className="btn btn-outline-secondary mb-3"
      >
        Voltar à Cadeia de Valor
      </Link>
      <PageTitle>Ciclos de aprendizado e vendas</PageTitle>
      {catalog.data?.entry ? (
        <nav aria-label="Local do ciclo na cadeia" className="mb-3">
          <Link to={`/business-process-chains?chainId=${chainId}`}>
            {catalog.data.entry.chainName}
          </Link>
          {" → "}
          <Link to={catalog.data.entry.parentUrl}>
            {catalog.data.entry.sequenceNumber}.{" "}
            {catalog.data.entry.parentProcessName}
          </Link>
          {catalog.data.entry.activityName ? (
            <>
              {" → Atividade "}
              {catalog.data.entry.activitySequenceNumber}
              {": "}
              {catalog.data.entry.activityName}
            </>
          ) : null}
          {" → Subprocesso: "}
          {catalog.data.entry.processName}
        </nav>
      ) : null}
      <p>
        Este subprocesso é executado pela atividade “Conduzir o ciclo de
        aprendizado e vendas” do processo de venda. Cada ciclo corresponde a um
        experimento e conserva seu aprendizado, suas métricas e a próxima ação.
      </p>
      <div className="cycle-form-grid mb-3">
        <label className="form-label">
          Cadeia de Valor *
          <select
            className="form-select"
            value={chainId ?? ""}
            onChange={(event) => select("chainId", event.target.value)}
          >
            <option value="">Selecione</option>
            {catalog.data?.entry &&
            chainId &&
            chains.data &&
            !chains.data.some((chain) => chain.id === chainId) ? (
              <option value={chainId}>
                {catalog.data.entry.chainName} · histórico do ciclo
              </option>
            ) : null}
            {chains.data?.map((chain) => (
              <option value={chain.id} key={chain.id}>
                {chain.name} · v{chain.versionNumber}
              </option>
            ))}
          </select>
        </label>
        <label className="form-label">
          Produto *
          <select
            className="form-select"
            value={productId ?? ""}
            onChange={(event) => select("productId", event.target.value)}
          >
            <option value="">Selecione</option>
            {products.data?.map((product) => (
              <option value={product.id} key={product.id}>
                {product.internalName || product.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {products.isError ||
      chains.isError ||
      cycles.isError ||
      catalog.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar os ciclos e seus requisitos. Confira a
          disponibilidade do backend e atualize a leitura.
        </div>
      ) : null}
      {(cycles.isLoading && productId) || catalog.isLoading ? (
        <p role="status">Carregando ciclos e BPM...</p>
      ) : null}
      {explicitCycle && productId && cycles.isSuccess && !cycle ? (
        <p className="alert alert-warning" role="alert">
          O ciclo solicitado não foi encontrado neste produto e cadeia.
          Selecione o histórico correto para continuar; nenhum outro ciclo foi
          aberto automaticamente.
        </p>
      ) : null}
      {productId && catalog.data ? (
        <div className="d-flex flex-wrap gap-2 mb-3">
          <button
            type="button"
            className="btn btn-primary"
            disabled={!catalog.data.entry?.canStartCycle}
            onClick={() => {
              setPredecessor(undefined);
              setCreating(true);
            }}
          >
            Abrir ciclo
          </button>
          <button
            type="button"
            className="btn btn-outline-primary"
            disabled={cycles.isFetching || catalog.isFetching}
            onClick={() => {
              void cycles.refetch();
              void catalog.refetch();
            }}
          >
            {cycles.isFetching || catalog.isFetching ? (
              <span
                className="spinner-border spinner-border-sm me-2"
                role="status"
                aria-label="Atualizando"
              />
            ) : null}
            Atualizar leitura
          </button>
          <Link
            className="btn btn-outline-secondary"
            to={`/products/${productId}/value-chain-history`}
          >
            Atividades da cadeia do produto
          </Link>
        </div>
      ) : null}
      {catalog.data && !catalog.data.entry?.canStartCycle ? (
        <p className="alert alert-warning" role="alert">
          Para abrir uma nova iteração, selecione a cadeia vigente com o ciclo
          integrado ao processo de venda. O histórico existente permanece
          disponível.
        </p>
      ) : null}
      {creating && predecessor && catalog.data?.successorChainName ? (
        <p className="alert alert-info">
          O sucessor usará {catalog.data.successorChainName}. O ciclo anterior
          conserva sua versão e seu histórico.
        </p>
      ) : null}
      {creating && productId && chainId && catalog.data ? (
        <LearningCycleCreateForm
          key={`${productId}-${predecessor?.id ?? "new"}`}
          productId={productId}
          chainId={
            predecessor
              ? (catalog.data.successorChainDefinitionId ?? chainId)
              : chainId
          }
          catalog={catalog.data}
          predecessor={predecessor}
          onCreated={updated}
          onCancel={() => setCreating(false)}
        />
      ) : null}
      {productId && !cycles.isLoading && cycles.data?.length === 0 ? (
        <p>
          Nenhum ciclo registrado. Adote uma referência histórica ou escolha um
          novo experimento planejado.
        </p>
      ) : null}
      {cycles.data && cycles.data.length > 0 ? (
        <label className="form-label w-100">
          Histórico de ciclos
          <select
            className="form-select"
            value={cycle?.id ?? ""}
            onChange={(event) => select("cycleId", event.target.value)}
          >
            {cycles.data.map((item) => (
              <option value={item.id} key={item.id}>
                Ciclo #{item.id} · experimento #{item.experimentId} ·{" "}
                {statuses[item.status] ?? item.status}
              </option>
            ))}
          </select>
        </label>
      ) : null}
      {cycle ? (
        <>
          <section className="card card-body mb-3">
            <h2 className="h4">
              Ciclo #{cycle.id} · experimento #{cycle.experimentId}
            </h2>
            <p>
              <strong>{cycle.stageLabel}</strong> ·{" "}
              {statuses[cycle.status] ?? cycle.status}
              {cycle.baseline ? " · referência histórica" : ""}
            </p>
            <p>
              Versão: {cycle.productVersion} · Teto total:{" "}
              {money.format(cycle.budgetLimitBrl)} · {date(cycle.windowStart)} a{" "}
              {date(cycle.windowEnd)}
            </p>
            <p>Hipótese: {String(cycle.brief.hypothesis)}</p>
            <p>Variável: {String(cycle.brief.mainChange)}</p>
            <p>Critério: {String(cycle.brief.successCriterion)}</p>
            <div className="d-flex flex-wrap gap-2">
              <Link
                to={`/experiments/${cycle.experimentId}`}
                className="btn btn-outline-primary"
              >
                Abrir experimento
              </Link>
              <Link to={cycle.workUrl} className="btn btn-outline-secondary">
                Abrir atividade orientada
              </Link>
              {cycle.canCreateSuccessor ? (
                <button
                  className="btn btn-primary"
                  type="button"
                  onClick={() => {
                    setPredecessor(cycle);
                    setCreating(true);
                  }}
                >
                  Criar ciclo sucessor com aprendizado
                </button>
              ) : null}
              {cycle.previousCycleId ? (
                <button
                  className="btn btn-outline-secondary"
                  type="button"
                  onClick={() =>
                    select("cycleId", String(cycle.previousCycleId))
                  }
                >
                  Ver ciclo anterior
                </button>
              ) : null}
              {cycle.successorCycleId ? (
                <button
                  className="btn btn-outline-secondary"
                  type="button"
                  onClick={() =>
                    select("cycleId", String(cycle.successorCycleId))
                  }
                >
                  Ver sucessor
                </button>
              ) : null}
            </div>
          </section>
          {cycle.inheritedLearning.cycleId ? (
            <section className="card card-body mb-3">
              <h3 className="h5">
                Aprendizado recebido do experimento #
                {cycle.inheritedLearning.experimentId}
              </h3>
              <p>
                Versão anterior: {cycle.inheritedLearning.productVersion}.
                Evidência histórica preservada para orientar esta hipótese.
              </p>
              <Audit events={cycle.inheritedLearning.events ?? []} />
            </section>
          ) : null}
          {cycle.stage === "MEASUREMENT" ? (
            <LearningCycleAutomaticMeasurement
              cycle={cycle}
              onUpdated={updated}
            />
          ) : null}
          {catalog.data &&
          (cycle.stage !== "MEASUREMENT" ||
            cycle.events[cycle.events.length - 1]?.action ===
              "MEASUREMENT_BLOCKED") ? (
            <LearningCycleCommandForm
              key={`${cycle.id}-${cycle.revision}`}
              cycle={cycle}
              catalog={catalog.data}
              onUpdated={updated}
            />
          ) : null}
          <details className="card card-body mb-3">
            <summary>
              Histórico de decisões e evidências ({cycle.events.length})
            </summary>
            <Audit events={cycle.events} />
          </details>
        </>
      ) : null}
      {catalog.data ? (
        <details className="card card-body">
          <summary>BPM · decisões e retornos do ciclo</summary>
          <LearningCycleDiagram
            diagram={cycle?.diagram ?? catalog.data.diagram}
            currentStage={cycle?.stage}
          />
        </details>
      ) : null}
    </div>
  );
}
