import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import axios from "axios";
import PageTitle from "../../components/PageTitle";
import { useExperiments } from "../../api/experiment/useExperiments";
import {
  AdsDraft,
  AdsCampaign,
  getAdsOverview,
  saveAdsDraft,
  adsAction,
  checkAdsConnection,
} from "../../api/chatgptAds";
import "./ChatgptAdsPage.css";
export const adsStatus: Record<string, string> = {
  DRAFT: "Em preparação",
  PUBLISHING: "Publicando pausada",
  AWAITING_REVIEW: "Em revisão na OpenAI",
  READY: "Pronta para ativar",
  ACTIVATING: "Ativando",
  ACTIVE: "Ativa no provedor",
  PAUSING: "Solicitando pausa",
  PAUSED: "Pausada",
  BLOCKED: "Precisa de atenção",
  RECOVERING: "Recuperando operação",
};
const blank = {
  experimentId: 0,
  creativeId: 0,
  name: "",
  title: "",
  body: "",
  imageUrl: "",
  targetUrl: "",
  contextHints: [],
  currency: "",
  totalBudget: 0,
  maxCpc: 0,
  startsAt: "",
  endsAt: "",
} satisfies AdsDraft;
const localTime = (v: string) =>
  v
    ? new Date(new Date(v).getTime() - new Date(v).getTimezoneOffset() * 60000)
        .toISOString()
        .slice(0, 16)
    : "";
export default function ChatgptAdsPage() {
  const cache = useQueryClient();
  const [search] = useSearchParams();
  const requestedId = Number(search.get("experimentId"));
  const selectedExperiment =
    Number.isSafeInteger(requestedId) && requestedId > 0
      ? requestedId
      : undefined;
  const query = useQuery({
    queryKey: ["chatgpt-ads", selectedExperiment],
    queryFn: () => getAdsOverview(selectedExperiment),
    refetchInterval: 30000,
  });
  const experiments = useExperiments();
  const [draft, setDraft] = useState<AdsDraft>({ ...blank });
  const [editing, setEditing] = useState<number>();
  useEffect(() => {
    setDraft({ ...blank, experimentId: selectedExperiment || 0 });
    setEditing(undefined);
  }, [selectedExperiment]);
  const [notice, setNotice] = useState("");
  const [decision, setDecision] = useState<AdsCampaign>();
  const [responsible, setResponsible] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const materials = useQuery({
    queryKey: ["chatgpt-materials", draft.experimentId],
    enabled: !!draft.experimentId,
    queryFn: async () => {
      const { data } = await axios.get(
        `/api/products/experiments/${draft.experimentId}/ads-in-use`,
      );
      return Array.isArray(data)
        ? data
        : data.ads.map((a: any) => ({ ...a, id: a.creativeId }));
    },
  });
  const mutate = useMutation({
    mutationFn: async (work: () => Promise<unknown>) => work(),
    onSuccess: () => {
      void cache.invalidateQueries({ queryKey: ["chatgpt-ads"] });
      setNotice(
        "Operação registrada. O acompanhamento é atualizado automaticamente.",
      );
      setDecision(undefined);
    },
    onError: (error: any) =>
      setNotice(
        error.response?.data?.detail ||
          error.response?.data?.message ||
          "Não foi possível concluir. Confira a conexão e tente novamente.",
      ),
  });
  const field = (key: keyof AdsDraft, value: unknown) =>
    setDraft((current) => ({ ...current, [key]: value }));
  const busy = mutate.isPending;
  return (
    <main className="chatgpt-ads-page">
      <header className="ads-hero">
        <div>
          <span className="ads-eyebrow">NOVO CANAL · HERMES</span>
          <PageTitle>ChatGPT Ads</PageTitle>
          <p>
            Apresente seu produto quando alguém estiver buscando uma solução.
          </p>
        </div>
        <a
          className="btn btn-outline-light"
          href="https://ads.openai.com/"
          target="_blank"
          rel="noreferrer"
        >
          Abrir gerenciador OpenAI
        </a>
      </header>
      <div className="ads-steps">
        <span>1. Prepare uma mensagem útil</span>
        <span>2. Autorize um teste limitado</span>
        <span>3. Acompanhe o resultado</span>
      </div>
      {notice && (
        <div role="status" className="alert alert-info">
          {notice}
        </div>
      )}
      {query.isError && (
        <div role="alert" className="alert alert-danger">
          Não foi possível consultar o canal.{" "}
          <button className="btn btn-link" onClick={() => void query.refetch()}>
            Tentar novamente
          </button>
        </div>
      )}
      <section className="ads-panel">
        <div className="d-flex flex-wrap justify-content-between gap-3">
          <div>
            <h2>Conexão da conta</h2>
            <p>
              {query.data?.connection.status === "active"
                ? `${query.data.connection.name} · ${query.data.connection.currency_code}`
                : "Conta ainda não conectada"}
            </p>
            <small>
              A chave de anúncios é própria deste canal. A conexão do ChatGPT e
              os limites da Meta permanecem separados.
            </small>
          </div>
          <button
            className="btn btn-outline-primary align-self-start"
            disabled={busy || query.isLoading}
            onClick={() => mutate.mutate(checkAdsConnection)}
          >
            {busy ? "Aguarde…" : "Verificar conexão"}
          </button>
        </div>
        <details className="mt-3">
          <summary>Como conectar e medir</summary>
          <p>
            Crie sua conta no gerenciador da OpenAI. O responsável pela
            infraestrutura configura a chave de anúncios no Hermes e a conexão
            protegida com o backend. Nenhuma chave é solicitada nesta página.
          </p>
          <p>
            Este painel acompanha impressões, cliques e gasto do provedor.
            Compras, entrega, reembolsos e contribuição permanecem no relatório
            do experimento. Pixel e envio de conversões à OpenAI não fazem parte
            desta primeira versão.
          </p>
          <a
            href="https://developers.openai.com/ads/api-overview"
            target="_blank"
            rel="noreferrer"
          >
            Documentação da conexão
          </a>
        </details>
      </section>
      <section className="ads-panel">
        <h2>{editing ? "Editar preparação" : "Preparar um anúncio"}</h2>
        <p>
          Use uma situação reconhecível, mostre um benefício e convide a ver uma
          amostra. Imagem no anúncio; vídeo demonstrativo pode ficar na página.
        </p>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            mutate.mutate(async () => {
              const result = await saveAdsDraft(draft, editing);
              setEditing(result.id);
              return result;
            });
          }}
        >
          <div className="ads-grid">
            <label>
              Experimento *
              <select
                required
                className="form-select"
                value={draft.experimentId || ""}
                disabled={!!editing}
                onChange={(e) =>
                  setDraft({ ...blank, experimentId: Number(e.target.value) })
                }
              >
                <option value="">Escolha o produto e experimento</option>
                {experiments.data
                  ?.filter(
                    (e) =>
                      e.platform === "CHATGPT_ADS" &&
                      (!selectedExperiment || e.id === selectedExperiment),
                  )
                  .map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.name} · #{e.id}
                      {e.platform !== "CHATGPT_ADS" ? " — outro canal" : ""}
                    </option>
                  ))}
              </select>
            </label>
            <label>
              Material do experimento *
              <select
                required
                className="form-select"
                value={draft.creativeId || ""}
                onChange={(e) => {
                  const m = materials.data?.find(
                    (a: any) => a.id === Number(e.target.value),
                  );
                  if (m)
                    setDraft((d) => ({
                      ...d,
                      creativeId: m.id,
                      title: m.headline || "",
                      body: m.primaryText || "",
                      imageUrl: m.imageUrl || "",
                      targetUrl: m.destinationUrl || "",
                    }));
                }}
              >
                <option value="">Escolha uma peça</option>
                {materials.data?.map((m: any) => (
                  <option key={m.id} value={m.id}>
                    {m.headline || `Peça ${m.id}`} ·{" "}
                    {m.status === "READY" ? "revisada" : "aguarda revisão"}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Nome desta preparação *
              <input
                required
                minLength={3}
                maxLength={200}
                className="form-control"
                value={draft.name}
                onChange={(e) => field("name", e.target.value)}
              />
            </label>
            <label>
              Título do anúncio *
              <input
                required
                minLength={3}
                maxLength={50}
                className="form-control"
                value={draft.title}
                onChange={(e) => field("title", e.target.value)}
              />
              <small>{draft.title.length}/50</small>
            </label>
            <label>
              Mensagem *
              <textarea
                required
                maxLength={100}
                className="form-control"
                value={draft.body}
                onChange={(e) => field("body", e.target.value)}
              />
              <small>
                {draft.body.length}/100 · Alterações precisam de revisão da peça
                correspondente.
              </small>
            </label>
            <label>
              Situações em que o produto ajuda *
              <textarea
                required
                className="form-control"
                value={draft.contextHints.join("\n")}
                placeholder="Uma situação por linha"
                onChange={(e) =>
                  field("contextHints", e.target.value.split("\n"))
                }
              />
              <small>
                Ex.: preparar a divulgação da semana. São sugestões de contexto,
                não garantia de uma conversa específica.
              </small>
            </label>
            <label>
              Imagem pública *
              <input
                required
                type="url"
                className="form-control"
                value={draft.imageUrl}
                onChange={(e) => field("imageUrl", e.target.value)}
              />
              <small>HTTPS, JPG/PNG/WebP, no mínimo 640 × 640.</small>
            </label>
            <label>
              Página de destino *
              <input
                required
                type="url"
                className="form-control"
                value={draft.targetUrl}
                onChange={(e) => field("targetUrl", e.target.value)}
              />
            </label>
            <label>
              Moeda da conta *
              <input
                required
                maxLength={3}
                pattern="[A-Z]{3}"
                placeholder="BRL ou USD"
                className="form-control"
                value={draft.currency}
                onChange={(e) =>
                  field("currency", e.target.value.toUpperCase())
                }
              />
            </label>
            <label>
              Teto total do teste *
              <input
                required
                type="number"
                min="1"
                step="0.01"
                className="form-control"
                value={draft.totalBudget || ""}
                onChange={(e) => field("totalBudget", Number(e.target.value))}
              />
              <small>Total na moeda da conta, não por dia.</small>
            </label>
            <label>
              Máximo por clique *
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                className="form-control"
                value={draft.maxCpc || ""}
                onChange={(e) => field("maxCpc", Number(e.target.value))}
              />
            </label>
            <label>
              Início *
              <input
                required
                type="datetime-local"
                className="form-control"
                value={localTime(draft.startsAt)}
                onChange={(e) =>
                  field(
                    "startsAt",
                    e.target.value
                      ? new Date(e.target.value).toISOString()
                      : "",
                  )
                }
              />
            </label>
            <label>
              Fim *
              <input
                required
                type="datetime-local"
                className="form-control"
                value={localTime(draft.endsAt)}
                onChange={(e) =>
                  field(
                    "endsAt",
                    e.target.value
                      ? new Date(e.target.value).toISOString()
                      : "",
                  )
                }
              />
              <small>Horário local deste navegador.</small>
            </label>
          </div>
          <div className="d-flex flex-wrap gap-3 mt-4">
            <button className="btn btn-primary" disabled={busy}>
              {busy ? "Salvando…" : "Salvar preparação sem publicar"}
            </button>
            {editing && (
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => {
                  setEditing(undefined);
                  setDraft({ ...blank });
                }}
              >
                Nova preparação
              </button>
            )}
            <Link to="/experiments/new" className="btn btn-link">
              Criar experimento próprio do canal
            </Link>
          </div>
        </form>
      </section>
      <section aria-label="Campanhas preparadas">
        <h2>Acompanhar campanhas</h2>
        {query.isLoading ? (
          <p>Carregando…</p>
        ) : !query.data?.campaigns.length ? (
          <p>Nenhuma preparação salva. O piloto pode começar por Capella.</p>
        ) : (
          query.data.campaigns.map((c) => (
            <article key={c.id} className="ads-panel">
              <div className="d-flex flex-wrap justify-content-between gap-2">
                <div>
                  <small>{c.productName}</small>
                  <h3>{c.draft.name}</h3>
                </div>
                <span
                  className={`ads-state ${c.status === "ACTIVE" ? "ads-active" : ""}`}
                >
                  {adsStatus[c.status] || "Aguardando atualização"}
                </span>
              </div>
              <div className="ads-grid">
                <div className="ads-preview">
                  {c.draft.imageUrl && (
                    <img
                      src={c.draft.imageUrl}
                      alt="Imagem da peça preparada"
                      loading="lazy"
                    />
                  )}
                  <strong>{c.draft.title}</strong>
                  <p>{c.draft.body}</p>
                  <small>Prévia ilustrativa do conteúdo</small>
                </div>
                <div>
                  <p>
                    <strong>
                      {c.draft.currency} {c.draft.totalBudget.toFixed(2)}
                    </strong>{" "}
                    no total · até {c.draft.currency}{" "}
                    {c.draft.maxCpc.toFixed(2)} por clique
                  </p>
                  <p>
                    {new Date(c.draft.startsAt).toLocaleString("pt-BR")} →{" "}
                    {new Date(c.draft.endsAt).toLocaleString("pt-BR")}
                  </p>
                  <div className="ads-metrics">
                    <div>
                      <strong>{c.metrics.impressions ?? "—"}</strong>
                      <span>Impressões</span>
                    </div>
                    <div>
                      <strong>{c.metrics.clicks ?? "—"}</strong>
                      <span>Cliques</span>
                    </div>
                    <div>
                      <strong>
                        {c.metrics.spend === undefined
                          ? "—"
                          : `${c.metrics.currency} ${c.metrics.spend.toFixed(2)}`}
                      </strong>
                      <span>Gasto informado</span>
                    </div>
                  </div>
                  <small>
                    — significa ainda não informado. Ativa no provedor não
                    comprova exposição ou venda.
                  </small>
                  <p className="mt-2">
                    <Link to={`/experiments/${c.experimentId}`}>
                      Abrir homologação, compras e resultado do experimento
                    </Link>
                  </p>
                </div>
              </div>
              {c.blockers.length > 0 && (
                <details className="mt-3">
                  <summary>
                    O que falta para avançar ({c.blockers.length})
                  </summary>
                  <ul>
                    {c.blockers.map((b) => (
                      <li key={b}>{b}</li>
                    ))}
                  </ul>
                </details>
              )}
              <div className="d-flex flex-wrap gap-2 mt-3">
                {c.status === "DRAFT" && (
                  <button
                    className="btn btn-outline-secondary"
                    disabled={busy}
                    onClick={() => {
                      setDraft(c.draft);
                      setEditing(c.id);
                    }}
                  >
                    Editar preparação
                  </button>
                )}
                {["DRAFT", "READY", "PAUSED"].includes(c.status) && (
                  <button
                    className="btn btn-outline-primary"
                    disabled={
                      busy || query.data?.connection.status !== "active"
                    }
                    onClick={() => {
                      setDecision(c);
                      setConfirmed(false);
                      setResponsible("");
                    }}
                  >
                    Conferir autorização
                  </button>
                )}
                <button
                  className="btn btn-primary"
                  disabled={busy || !c.canPublish}
                  onClick={() =>
                    mutate.mutate(() => adsAction(c.id, "publish"))
                  }
                >
                  Publicar pausada
                </button>
                <button
                  className="btn btn-success"
                  disabled={busy || !c.canActivate}
                  onClick={() =>
                    mutate.mutate(() => adsAction(c.id, "activate"))
                  }
                >
                  Ativar teste autorizado
                </button>
                {c.canPause && (
                  <button
                    className="btn btn-outline-danger"
                    disabled={busy}
                    onClick={() =>
                      mutate.mutate(() => adsAction(c.id, "pause"))
                    }
                  >
                    Pausar campanha
                  </button>
                )}
                {!!c.remote.adId && (
                  <button
                    className="btn btn-outline-secondary"
                    disabled={busy}
                    onClick={() => mutate.mutate(() => adsAction(c.id, "sync"))}
                  >
                    Consultar canal
                  </button>
                )}
                {c.status === "BLOCKED" && (
                  <button
                    className="btn btn-outline-secondary"
                    disabled={busy}
                    onClick={() =>
                      mutate.mutate(() => adsAction(c.id, "retry"))
                    }
                  >
                    Retomar operação preservada
                  </button>
                )}
              </div>
              <small className="d-block mt-2">
                Atualizado em {new Date(c.updatedAt).toLocaleString("pt-BR")}.
                Hermes acompanha a fila a cada 30 segundos.
              </small>
            </article>
          ))
        )}
      </section>
      {decision && (
        <section
          className="ads-panel ads-decision"
          aria-label="Autorização do canal"
        >
          <h2>Autorizar este teste no ChatGPT Ads</h2>
          <p>
            {decision.productName}: máximo de{" "}
            <strong>
              {decision.draft.currency} {decision.draft.totalBudget.toFixed(2)}{" "}
              no total
            </strong>
            , até {decision.draft.currency} {decision.draft.maxCpc.toFixed(2)}{" "}
            por clique, entre{" "}
            {new Date(decision.draft.startsAt).toLocaleString("pt-BR")} e{" "}
            {new Date(decision.draft.endsAt).toLocaleString("pt-BR")}.
          </p>
          <p>
            A campanha pode consumir o limite sem gerar compras. Os orçamentos
            de outros canais não serão usados.
          </p>
          <label className="d-block">
            Responsável *
            <input
              className="form-control"
              required
              maxLength={200}
              value={responsible}
              onChange={(e) => setResponsible(e.target.value)}
            />
          </label>
          <label className="d-block my-3">
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(e) => setConfirmed(e.target.checked)}
            />{" "}
            Confirmo este anúncio, esta conta, o teto e a janela acima para
            ChatGPT Ads.
          </label>
          <button
            className="btn btn-primary"
            disabled={busy || !confirmed || !responsible.trim()}
            onClick={() =>
              mutate.mutate(() =>
                adsAction(decision.id, "authorize", {
                  fingerprint: decision.fingerprint,
                  decidedBy: responsible,
                  confirmation: "AUTORIZO CHATGPT ADS",
                }),
              )
            }
          >
            {busy ? "Registrando…" : "Registrar autorização"}
          </button>{" "}
          <button
            className="btn btn-link"
            disabled={busy}
            onClick={() => setDecision(undefined)}
          >
            Voltar
          </button>
        </section>
      )}
    </main>
  );
}
