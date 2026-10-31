import VideoBudgetForm from "./VideoBudgetForm";
import { Link, useSearchParams } from "react-router-dom";
import { CircleDollarSign, Film, RefreshCw } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useProducts } from "../../api/product/useProducts";
import {
  cycleError,
  useLearningCycles,
} from "../../api/learningCycle/useLearningCycles";
import { useVideoBudget } from "../../api/financial/useVideoBudget";
import "./VideoFinancePage.css";

const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "USD",
});
const date = (value: string) => new Date(value).toLocaleString("pt-BR");
const identifier = (value: string | null) =>
  value && /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value))
    ? Number(value)
    : undefined;

/** Apresenta o financeiro das duas peças com seleção explícita e contexto oficial do ciclo. */
export default function VideoFinancePage() {
  const [params, setParams] = useSearchParams();
  const productId = identifier(params.get("productId"));
  const cycleId = identifier(params.get("cycleId"));
  const products = useProducts();
  const cycles = useLearningCycles(productId);
  const selectedCycle = cycles.data?.find((item) => item.id === cycleId);
  const chainId = params.has("chainId")
    ? identifier(params.get("chainId"))
    : selectedCycle?.chainDefinitionId;
  const invalid = ["productId", "cycleId", "chainId"].some(
    (key) => params.has(key) && !identifier(params.get(key)),
  );
  const query = useVideoBudget(
    invalid ? undefined : productId,
    cycleId,
    chainId,
  );
  const budget = query.data;
  const unavailable = Boolean(
    productId && cycleId && cycles.isSuccess && !selectedCycle,
  );
  return (
    <div className="video-finance-page">
      <PageTitle subtitle="Invista em peças que mostrem o valor real do produto e ajudem a transformar interesse em vendas.">
        Financeiro de vídeos
      </PageTitle>
      <div className="d-flex flex-wrap gap-2 mb-4">
        {budget ? (
          <Link className="btn btn-outline-primary" to={budget.cycleUrl}>
            Voltar ao ciclo #{budget.cycleId}
          </Link>
        ) : null}
        <Link
          className="btn btn-outline-secondary"
          to="/financial/video-providers"
        >
          Consultar saldo dos provedores
        </Link>
      </div>
      <section
        className="card card-body mb-4"
        aria-label="Selecionar contexto financeiro"
      >
        <div className="row g-3">
          <div className="col-12 col-md-6">
            <label className="form-label d-block">
              Produto *
              <select
                className="form-select"
                value={productId ?? ""}
                onChange={(event) =>
                  setParams(
                    event.target.value ? { productId: event.target.value } : {},
                  )
                }
              >
                <option value="">Selecione o produto</option>
                {products.data?.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.internalName || item.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="col-12 col-md-6">
            <label className="form-label d-block">
              Ciclo e experimento *
              <select
                className="form-select"
                value={cycleId ?? ""}
                disabled={!productId || cycles.isLoading}
                onChange={(event) => {
                  const item = cycles.data?.find(
                    (cycle) => cycle.id === Number(event.target.value),
                  );
                  setParams(
                    item
                      ? {
                          productId: String(item.productId),
                          chainId: String(item.chainDefinitionId),
                          cycleId: String(item.id),
                        }
                      : { productId: String(productId) },
                  );
                }}
              >
                <option value="">Selecione o ciclo</option>
                {cycles.data?.map((item) => (
                  <option key={item.id} value={item.id}>
                    Ciclo #{item.id} · experimento #{item.experimentId} ·{" "}
                    {item.stageLabel}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
      </section>
      {invalid || unavailable ? (
        <p className="alert alert-warning" role="alert">
          O contexto solicitado é inválido ou não pertence a este produto.
          Selecione o ciclo correto.
        </p>
      ) : null}
      {products.isError || cycles.isError || query.isError ? (
        <p className="alert alert-danger" role="alert">
          {query.isError
            ? cycleError(query.error)
            : "Não foi possível carregar os produtos e ciclos. Atualize a página para tentar novamente."}
        </p>
      ) : null}
      {query.isError ? (
        <button
          className="btn btn-outline-primary mb-3"
          disabled={query.isFetching}
          onClick={() => query.refetch()}
        >
          {query.isFetching ? (
            <span className="spinner-border spinner-border-sm me-2" />
          ) : null}
          Tentar novamente
        </button>
      ) : null}
      {products.isLoading ||
      (productId && cycles.isLoading) ||
      (cycleId && query.isLoading) ? (
        <p role="status">Carregando contexto financeiro...</p>
      ) : null}
      {!cycleId && !invalid ? (
        <p className="text-body-secondary">
          Escolha o produto e o ciclo para consultar ou registrar o teto das
          duas peças.
        </p>
      ) : null}
      {productId && cycles.isSuccess && !cycles.data.length ? (
        <p>
          Nenhum ciclo registrado para este produto.{" "}
          <Link
            to={`/business-process-chains/learning-cycles?productId=${productId}`}
          >
            Abrir ciclos de aprendizado e vendas
          </Link>
        </p>
      ) : null}
      {budget && !invalid && !unavailable && !query.isError ? (
        <>
          <section
            className="card card-body mb-4"
            aria-label="Contexto e limite dos vídeos"
          >
            <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start">
              <div>
                <h2 className="h4">
                  {budget.internalName || budget.productName}
                </h2>
                <p className="mb-2">{budget.productName}</p>
              </div>
              <button
                className="btn btn-outline-secondary btn-sm"
                disabled={query.isFetching}
                onClick={() => query.refetch()}
              >
                {query.isFetching ? (
                  <span className="spinner-border spinner-border-sm me-2" />
                ) : (
                  <RefreshCw size={15} className="me-2" />
                )}
                Atualizar leitura
              </button>
            </div>
            <p>
              Ciclo #{budget.cycleId} · Experimento #{budget.experimentId} ·
              Cadeia #{budget.chainDefinitionId} · Processo #
              {budget.processDefinitionId}
            </p>
            <p className="small text-body-secondary">
              Versão: {budget.productVersion} · Etapa: {budget.stageLabel}
            </p>
            <p className="video-finance-instruction">
              <CircleDollarSign aria-hidden="true" size={24} />
              <span>{budget.instruction}</span>
            </p>
            <div className="row g-3 mb-3">
              <div className="col-12 col-lg-6">
                <div className="video-finance-piece">
                  <Film size={20} aria-hidden="true" />
                  <div>
                    <h3 className="h6">1. Anúncio</h3>
                    <p className="mb-0">
                      Comunicar o benefício e atrair visitas qualificadas para a
                      experiência real.
                    </p>
                  </div>
                </div>
              </div>
              <div className="col-12 col-lg-6">
                <div className="video-finance-piece">
                  <Film size={20} aria-hidden="true" />
                  <div>
                    <h3 className="h6">2. Demonstração na entrada</h3>
                    <p className="mb-0">
                      Mostrar como obter o primeiro resultado e facilitar o
                      próximo passo até a compra.
                    </p>
                  </div>
                </div>
              </div>
            </div>
            <strong className="h5 mb-2">
              {budget.status === "LIMIT_RECORDED" && budget.currentAuthorization
                ? `Teto registrado: ${money.format(budget.currentAuthorization.budgetLimitUsd)}`
                : "Aguardando teto autorizado"}
            </strong>
            <p className="mb-0 small">
              A avaliação de Plutus e os controles de custo do Estúdio continuam
              obrigatórios. Registrar o teto não inicia a produção.
            </p>
          </section>
          {budget.blocker ? (
            <p role="status" className="alert alert-info">
              {budget.blocker}
            </p>
          ) : null}
          {budget.currentAuthorization ? (
            <section
              className="alert alert-success"
              aria-label="Autorização registrada"
            >
              <strong>Teto disponível para avaliação financeira.</strong>
              <p className="mb-1">
                Registrado por {budget.currentAuthorization.operatorName} em{" "}
                {date(budget.currentAuthorization.createdAt)}.
              </p>
              <p className="mb-1">
                {budget.currentAuthorization.justification}
              </p>
              <Link to={budget.cycleUrl}>Continuar no ciclo com este teto</Link>
            </section>
          ) : null}
          {budget.canAuthorize ? (
            <VideoBudgetForm
              key={`${budget.cycleId}-${budget.revision}`}
              budget={budget}
            />
          ) : null}
          <section
            className="card card-body mt-4"
            aria-label="Histórico das autorizações"
          >
            <h2 className="h5">Histórico das autorizações</h2>
            {!budget.history.length ? (
              <p className="mb-0">
                Nenhuma autorização registrada neste ciclo.
              </p>
            ) : (
              <ol className="video-finance-history">
                {budget.history.map((item) => (
                  <li key={item.eventId}>
                    <strong>
                      {money.format(item.budgetLimitUsd)} ·{" "}
                      {item.current
                        ? "Vigente nesta versão"
                        : "Registro histórico"}
                    </strong>
                    <p>
                      {item.operatorName} · {date(item.createdAt)}
                      <br />
                      Versão: {item.productVersion}
                    </p>
                    <p>{item.justification}</p>
                    <small>Referência: {item.reference}</small>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}
