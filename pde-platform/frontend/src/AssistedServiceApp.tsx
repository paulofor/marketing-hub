import React, { FormEvent, useEffect, useMemo, useState } from "react";
import {
  BookOpen,
  Check,
  ChevronRight,
  ClipboardCheck,
  Clock3,
  KeyRound,
  LoaderCircle,
  Mail,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";
import type { ProductExperience } from "./musaExperiences";

type MissionInteraction = {
  missionId: string;
  questionKey: string;
  answerText: string;
};

type Workspace = {
  product: ProductExperience;
  email: string;
  accessSource: string;
  subscriptionStatus: "ACTIVE" | "TRIAL";
  completedMissions: number;
  totalMissions: number;
  progressPercent: number;
  completedMissionIds: string[];
  missionInteractions: MissionInteraction[];
  deliveryArtifacts: DeliveryArtifact[];
  supportStatus: "NONE" | "OPEN" | "RESOLVED";
};

type DeliveryArtifact = {
  missionId: string;
  title: string;
  version: string;
  createdAt: string;
  content: string;
  downloadUrl: string;
};

type AccessResponse = {
  token: string;
  productSlug: string;
  email: string;
  source: string;
  accessUrl: string;
};

type MagicLinkResponse = {
  productSlug: string;
  email: string;
  deliveryStatus: string;
  accessUrl?: string;
};

type AssistedServiceAppProps = {
  productSlug: string;
};

const testAccessEnabled = import.meta.env.VITE_PDE_ENABLE_DEV_ACCESS === "true";

/** Renderiza produtos PDE assistidos usando somente o contrato público versionado. */
export function AssistedServiceApp({ productSlug }: AssistedServiceAppProps) {
  const [product, setProduct] = useState<ProductExperience | null>(null);
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [accessToken, setAccessToken] = useState(() =>
    accessTokenFromLocation(productSlug),
  );
  const [email, setEmail] = useState("");
  const [noteByMission, setNoteByMission] = useState<Record<string, string>>(
    {},
  );
  const [answersByMission, setAnswersByMission] = useState<
    Record<string, Record<string, string>>
  >({});
  const [supportMessage, setSupportMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  /** Carrega o produto e restaura a área da cliente quando existe token local. */
  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const productResponse = await fetch(
          `/api/pde/products/${encodeURIComponent(productSlug)}`,
        );
        if (!productResponse.ok)
          throw new Error("Não foi possível carregar este produto agora.");
        const loadedProduct =
          (await productResponse.json()) as ProductExperience;
        if (!active) return;
        setProduct(loadedProduct);
        document.title = loadedProduct.name;
        void trackEvent("PAGE_VIEW", loadedProduct);
        if (accessToken) {
          const loadedWorkspace = await fetchWorkspace(accessToken);
          if (!active) return;
          setWorkspace(loadedWorkspace);
          setEmail(loadedWorkspace.email);
          setNoteByMission(
            interactionsByMission(loadedWorkspace.missionInteractions),
          );
          setAnswersByMission(
            allInteractionsByMission(loadedWorkspace.missionInteractions),
          );
          void trackEvent("SCREEN_VIEW", loadedProduct, {
            accessToken,
            email: loadedWorkspace.email,
            provider: loadedWorkspace.accessSource,
            metadata: { screenName: "assisted_workspace" },
          });
        }
      } catch (requestError) {
        if (active) setError(readError(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, [accessToken, productSlug]);

  const completed = useMemo(
    () => new Set(workspace?.completedMissionIds ?? []),
    [workspace?.completedMissionIds],
  );
  const deliveryReleased = completed.has("entrega-completa-48h");
  const deliveryByMission = useMemo(
    () =>
      new Map(
        (workspace?.deliveryArtifacts ?? []).map((artifact) => [
          artifact.missionId,
          artifact,
        ]),
      ),
    [workspace?.deliveryArtifacts],
  );

  /** Solicita acesso real ou cria acesso segregado quando a homologação local está habilitada. */
  async function requestAccess(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    setError("");
    try {
      if (product) {
        void trackEvent("LOGIN_STARTED", product, { email });
      }
      const endpoint = testAccessEnabled
        ? "/api/pde/access/dev"
        : "/api/pde/access/login-link";
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productSlug, email }),
      });
      if (!response.ok) throw new Error(await responseMessage(response));
      if (testAccessEnabled) {
        const access = (await response.json()) as AccessResponse;
        persistAccessToken(productSlug, access.token);
        setAccessToken(access.token);
        setMessage(
          "Acesso de homologação liberado. Nenhuma compra ou venda foi registrada.",
        );
        if (product) {
          void trackEvent("LOGIN_COMPLETED", product, {
            accessToken: access.token,
            email: access.email,
            provider: access.source,
          });
        }
      } else {
        const link = (await response.json()) as MagicLinkResponse;
        setMessage(
          link.deliveryStatus === "SENT"
            ? "Enviamos o link de acesso para o seu e-mail."
            : "O pedido de acesso foi recebido. Verifique seu e-mail em alguns minutos.",
        );
      }
    } catch (requestError) {
      setError(readError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  /** Salva a evidência escrita da etapa antes de marcá-la como concluída. */
  async function completeMission(missionId: string) {
    if (!accessToken || !workspace) return;
    setSubmitting(true);
    setMessage("");
    setError("");
    try {
      const structuredAnswers = answersByMission[missionId] ?? {};
      const note = noteByMission[missionId]?.trim();
      const answers = Object.fromEntries(
        Object.entries(structuredAnswers)
          .map(([key, value]) => [key, value.trim()])
          .filter(([, value]) => value),
      );
      if (note) answers.customerEvidence = note;
      const requiredAnswerKeys =
        missionId === "entrada-guiada"
          ? [
              "services",
              "repeatedQuestions",
              "policies",
              "tone",
              "anonymousScenarios",
            ]
          : missionId === "primeira-aplicacao-e-revisao"
            ? [
                "selectedResponses",
                "qualificationBlock",
                "escalationRule",
                "applicationStatus",
                "applicationOutcome",
                "applicationReview",
              ]
            : [];
      if (requiredAnswerKeys.some((key) => !answers[key])) {
        throw new Error(
          "Preencha todos os campos desta etapa antes de concluir.",
        );
      }
      if (Object.keys(answers).length > 0) {
        const interaction = await fetch(
          `/api/pde/access/${encodeURIComponent(accessToken)}/missions/${encodeURIComponent(missionId)}/interactions`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ answers }),
          },
        );
        if (!interaction.ok)
          throw new Error(await responseMessage(interaction));
      }
      const response = await fetch(
        `/api/pde/access/${encodeURIComponent(accessToken)}/missions/${encodeURIComponent(missionId)}/complete`,
        { method: "POST" },
      );
      if (!response.ok) throw new Error(await responseMessage(response));
      const updated = (await response.json()) as Workspace;
      setWorkspace(updated);
      setMessage("Etapa salva. Você pode sair e retomar com o mesmo acesso.");
      void trackEvent("MISSION_COMPLETED", workspace.product, {
        accessToken,
        email: workspace.email,
        provider: workspace.accessSource,
        metadata: { missionId },
      });
    } catch (requestError) {
      setError(readError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  /** Atualiza um campo estruturado de briefing ou primeira aplicação sem misturar etapas. */
  function setMissionAnswer(missionId: string, key: string, value: string) {
    setAnswersByMission((current) => ({
      ...current,
      [missionId]: { ...(current[missionId] ?? {}), [key]: value },
    }));
  }

  /** Registra uma solicitação rastreável de suporte ou revisão dentro do próprio acesso. */
  async function requestSupport(event: FormEvent) {
    event.preventDefault();
    if (!accessToken || !supportMessage.trim()) return;
    setSubmitting(true);
    setMessage("");
    setError("");
    try {
      const response = await fetch(
        `/api/pde/access/${encodeURIComponent(accessToken)}/support-requests`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ message: supportMessage.trim() }),
        },
      );
      if (!response.ok) throw new Error(await responseMessage(response));
      setWorkspace((await response.json()) as Workspace);
      setSupportMessage("");
      setMessage(
        "Pedido de suporte registrado. A equipe responderá no e-mail deste acesso.",
      );
    } catch (requestError) {
      setError(readError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  /** Remove somente o token local e preserva o progresso persistido no backend. */
  function leaveWorkspace() {
    window.localStorage.removeItem(storageKey(productSlug));
    const url = new URL(window.location.href);
    url.searchParams.delete("access");
    window.history.replaceState({}, "", url);
    setAccessToken("");
    setWorkspace(null);
    setMessage("Sessão encerrada. Seu progresso permanece salvo.");
  }

  if (loading) {
    return (
      <main className="assisted-pde-state" role="status">
        <LoaderCircle className="assisted-pde-spinner" />
        <p>Carregando sua experiência...</p>
      </main>
    );
  }

  if (!product) {
    return (
      <main className="assisted-pde-state assisted-pde-error" role="alert">
        <h1>Não foi possível abrir o produto</h1>
        <p>{error || "Tente novamente em alguns minutos."}</p>
        <button onClick={() => window.location.reload()}>
          Tentar novamente
        </button>
      </main>
    );
  }

  if (!workspace) {
    return (
      <main
        className="assisted-pde-shell"
        style={
          {
            "--assisted-primary": product.theme.primary,
            "--assisted-accent": product.theme.accent,
            "--assisted-background": product.theme.background,
          } as React.CSSProperties
        }
      >
        <section className="assisted-pde-entry" data-testid="assisted-entry">
          <div className="assisted-pde-copy">
            <span className="assisted-pde-kicker">
              Experiência assistida e manual
            </span>
            <h1>{product.name}</h1>
            <p className="assisted-pde-promise">{product.promise}</p>
            <div className="assisted-pde-trust-grid">
              <span>
                <Clock3 /> Microvalor em até 12 horas
              </span>
              <span>
                <ShieldCheck /> Revisão humana antes do uso
              </span>
              <span>
                <RotateCcw /> Progresso salvo para retomar
              </span>
            </div>
          </div>
          <aside className="assisted-pde-access-card">
            <KeyRound />
            <h2>Acesse sua área</h2>
            <p>Use o e-mail informado na compra. Não pedimos senha.</p>
            <form onSubmit={requestAccess}>
              <label htmlFor="assisted-email">E-mail</label>
              <input
                id="assisted-email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="voce@empresa.com.br"
              />
              <button type="submit" disabled={submitting}>
                {submitting ? (
                  <LoaderCircle className="assisted-pde-spinner" />
                ) : (
                  <Mail />
                )}
                {testAccessEnabled
                  ? "Entrar na homologação"
                  : "Receber link de acesso"}
              </button>
            </form>
            {message ? (
              <p className="assisted-pde-success" role="status">
                {message}
              </p>
            ) : null}
            {error ? (
              <p className="assisted-pde-form-error" role="alert">
                {error}
              </p>
            ) : null}
            <small>{product.audience}</small>
          </aside>
        </section>
        <section
          className="assisted-pde-preview"
          aria-labelledby="assisted-diagnostic-title"
        >
          <div>
            <span className="assisted-pde-kicker">Antes de começar</span>
            <h2 id="assisted-diagnostic-title">{product.diagnostic.title}</h2>
            <p>{product.diagnostic.intro}</p>
          </div>
          <ul>
            {product.diagnostic.questions.map((question) => (
              <li key={question}>{question}</li>
            ))}
          </ul>
        </section>
      </main>
    );
  }

  return (
    <main
      className="assisted-pde-shell assisted-pde-workspace"
      style={
        {
          "--assisted-primary": workspace.product.theme.primary,
          "--assisted-accent": workspace.product.theme.accent,
          "--assisted-background": workspace.product.theme.background,
        } as React.CSSProperties
      }
      data-testid="assisted-workspace"
    >
      <header className="assisted-pde-header">
        <div>
          <span className="assisted-pde-kicker">Sua área de trabalho</span>
          <h1>{workspace.product.name}</h1>
          <p>{workspace.email}</p>
        </div>
        <button className="assisted-pde-secondary" onClick={leaveWorkspace}>
          Sair
        </button>
      </header>

      <section
        className="assisted-pde-progress"
        aria-label="Progresso da experiência"
      >
        <div>
          <span>Progresso</span>
          <strong>{workspace.progressPercent}%</strong>
        </div>
        <div className="assisted-pde-progress-track">
          <span style={{ width: `${workspace.progressPercent}%` }} />
        </div>
        <p>
          {workspace.completedMissions} de {workspace.totalMissions} etapas
          concluídas. Seu progresso fica salvo.
        </p>
      </section>

      <section
        className="assisted-pde-service-notice"
        aria-label="Prazos e privacidade"
      >
        <ShieldCheck />
        <div>
          <strong>
            Seus prazos começam após pagamento aprovado e entrada completa.
          </strong>
          <p>
            Não informe nomes, telefones, endereços ou conversas identificáveis.
            Use cinco situações equivalentes ou exemplos anonimizados.
          </p>
        </div>
      </section>

      {message ? (
        <p className="assisted-pde-success" role="status">
          {message}
        </p>
      ) : null}
      {error ? (
        <p className="assisted-pde-form-error" role="alert">
          {error}
        </p>
      ) : null}

      <section
        className="assisted-pde-missions"
        aria-labelledby="assisted-journey-title"
      >
        <div className="assisted-pde-section-heading">
          <ClipboardCheck />
          <div>
            <span className="assisted-pde-kicker">Jornada guiada</span>
            <h2 id="assisted-journey-title">Da entrada ao primeiro uso</h2>
          </div>
        </div>
        <div className="assisted-pde-mission-list">
          {workspace.product.missions.map((mission) => {
            const isCompleted = completed.has(mission.id);
            const isOperational = mission.completionRole === "OPERATION";
            const delivery = deliveryByMission.get(mission.id);
            return (
              <article
                className={
                  isCompleted
                    ? "assisted-pde-mission is-complete"
                    : "assisted-pde-mission"
                }
                key={mission.id}
              >
                <div className="assisted-pde-mission-index">
                  {isCompleted ? <Check /> : mission.day}
                </div>
                <div>
                  <h3>{mission.title}</h3>
                  <p>
                    <strong>Objetivo:</strong> {mission.principle}
                  </p>
                  <p>
                    <strong>O que fazer:</strong> {mission.action}
                  </p>
                  <p>
                    <strong>Conclusão:</strong> {mission.evidence}
                  </p>
                  <small>{mission.visualCue}</small>
                  {isCompleted && delivery ? (
                    <div
                      className="assisted-pde-personal-delivery"
                      data-testid={`delivery-${mission.id}`}
                    >
                      <span>Entrega personalizada · {delivery.version}</span>
                      <h4>{delivery.title}</h4>
                      <p>
                        Gerada para este acesso em{" "}
                        {new Date(delivery.createdAt).toLocaleString("pt-BR")}.
                      </p>
                      <pre>{delivery.content}</pre>
                      <a href={delivery.downloadUrl}>
                        Baixar entrega personalizada
                      </a>
                    </div>
                  ) : null}
                  {!isCompleted && isOperational ? (
                    <div className="assisted-pde-operation-state" role="status">
                      <Clock3 />
                      <span>
                        Aguardando a equipe concluir esta etapa. Você não
                        precisa simular o trabalho da operação.
                      </span>
                    </div>
                  ) : null}
                  {!isCompleted && !isOperational ? (
                    <div className="assisted-pde-mission-action">
                      {mission.id === "entrada-guiada" ? (
                        <>
                          {[
                            ["services", "Serviços principais"],
                            [
                              "repeatedQuestions",
                              "Dúvidas e objeções recorrentes",
                            ],
                            [
                              "policies",
                              "Regras de preço, agenda e área atendida",
                            ],
                            ["tone", "Tom de voz desejado"],
                            [
                              "anonymousScenarios",
                              "Cinco situações equivalentes ou exemplos anonimizados",
                            ],
                          ].map(([key, label]) => (
                            <React.Fragment key={key}>
                              <label htmlFor={`mission-${mission.id}-${key}`}>
                                {label}
                              </label>
                              <textarea
                                id={`mission-${mission.id}-${key}`}
                                rows={2}
                                required
                                maxLength={2000}
                                value={
                                  answersByMission[mission.id]?.[key] ?? ""
                                }
                                onChange={(event) =>
                                  setMissionAnswer(
                                    mission.id,
                                    key,
                                    event.target.value,
                                  )
                                }
                              />
                            </React.Fragment>
                          ))}
                        </>
                      ) : mission.id === "primeira-aplicacao-e-revisao" ? (
                        <>
                          <label
                            htmlFor={`mission-${mission.id}-applicationStatus`}
                          >
                            Situação da primeira aplicação
                          </label>
                          <select
                            id={`mission-${mission.id}-applicationStatus`}
                            required
                            value={
                              answersByMission[mission.id]?.applicationStatus ??
                              ""
                            }
                            onChange={(event) =>
                              setMissionAnswer(
                                mission.id,
                                "applicationStatus",
                                event.target.value,
                              )
                            }
                          >
                            <option value="">Selecione</option>
                            <option value="PLANNED">
                              Apenas planejada — ainda não concluir
                            </option>
                            <option value="APPLIED">
                              Aplicada manualmente e revisada
                            </option>
                          </select>
                          {[
                            ["selectedResponses", "Três respostas escolhidas"],
                            [
                              "qualificationBlock",
                              "Bloco de qualificação escolhido",
                            ],
                            [
                              "escalationRule",
                              "Regra de escalonamento escolhida",
                            ],
                            [
                              "applicationOutcome",
                              "Retorno observado na primeira aplicação",
                            ],
                            [
                              "applicationReview",
                              "Revisão da primeira aplicação",
                            ],
                          ].map(([key, label]) => (
                            <React.Fragment key={key}>
                              <label htmlFor={`mission-${mission.id}-${key}`}>
                                {label}
                              </label>
                              <textarea
                                id={`mission-${mission.id}-${key}`}
                                rows={2}
                                required
                                maxLength={2000}
                                value={
                                  answersByMission[mission.id]?.[key] ?? ""
                                }
                                onChange={(event) =>
                                  setMissionAnswer(
                                    mission.id,
                                    key,
                                    event.target.value,
                                  )
                                }
                              />
                            </React.Fragment>
                          ))}
                        </>
                      ) : (
                        <>
                          <label htmlFor={`mission-note-${mission.id}`}>
                            Sua evidência ou observação nesta etapa
                          </label>
                          <textarea
                            id={`mission-note-${mission.id}`}
                            rows={3}
                            maxLength={1200}
                            value={noteByMission[mission.id] ?? ""}
                            onChange={(event) =>
                              setNoteByMission((current) => ({
                                ...current,
                                [mission.id]: event.target.value,
                              }))
                            }
                            placeholder="Escreva apenas informações necessárias e sem dados pessoais dos seus clientes."
                          />
                        </>
                      )}
                      <button
                        disabled={submitting}
                        onClick={() => completeMission(mission.id)}
                      >
                        Concluir etapa <ChevronRight />
                      </button>
                    </div>
                  ) : null}
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <section
        className="assisted-pde-materials"
        aria-labelledby="assisted-materials-title"
      >
        <div className="assisted-pde-section-heading">
          <BookOpen />
          <div>
            <span className="assisted-pde-kicker">
              Modelos-base complementares
            </span>
            <h2 id="assisted-materials-title">Biblioteca de apoio</h2>
          </div>
        </div>
        {deliveryReleased ? (
          <div className="assisted-pde-material-grid">
            {workspace.product.supportMaterials.map((material) => (
              <article key={material.title}>
                <span>{material.type}</span>
                <h3>{material.title}</h3>
                <p>{material.description}</p>
                <a
                  href={material.url}
                  target="_blank"
                  rel="noreferrer"
                  onClick={() =>
                    void trackEvent("MATERIAL_OPEN", workspace.product, {
                      accessToken,
                      email: workspace.email,
                      provider: workspace.accessSource,
                      metadata: { materialTitle: material.title },
                    })
                  }
                >
                  Abrir material
                </a>
              </article>
            ))}
          </div>
        ) : (
          <div className="assisted-pde-materials-locked" role="status">
            <Clock3 />
            <p>
              Materiais liberados após a equipe concluir a entrega completa. O
              progresso permanece salvo enquanto você aguarda.
            </p>
          </div>
        )}
      </section>

      <footer className="assisted-pde-completion">
        <ShieldCheck />
        <p>{workspace.product.completionOffer}</p>
      </footer>

      <section
        className="assisted-pde-support"
        aria-labelledby="assisted-support-title"
      >
        <Mail />
        <div>
          <h2 id="assisted-support-title">Suporte e revisão</h2>
          <p>
            Peça esclarecimento, renovação do acesso ou revisão do kit. A
            resposta será enviada ao e-mail desta área.
          </p>
          {workspace.supportStatus === "OPEN" ? (
            <p className="assisted-pde-success" role="status">
              Existe um pedido de suporte aberto para este acesso.
            </p>
          ) : (
            <form onSubmit={requestSupport}>
              <label htmlFor="assisted-support-message">
                Como podemos ajudar?
              </label>
              <textarea
                id="assisted-support-message"
                rows={3}
                maxLength={2000}
                required
                value={supportMessage}
                onChange={(event) => setSupportMessage(event.target.value)}
              />
              <button type="submit" disabled={submitting}>
                Solicitar suporte ou revisão
              </button>
            </form>
          )}
        </div>
      </section>
    </main>
  );
}

/** Consulta a área autenticada usando o token de acesso. */
async function fetchWorkspace(token: string) {
  const response = await fetch(
    `/api/pde/access/${encodeURIComponent(token)}/workspace`,
  );
  if (!response.ok) throw new Error(await responseMessage(response));
  return (await response.json()) as Workspace;
}

/** Consolida as respostas anteriores para restaurar o formulário da etapa. */
function interactionsByMission(interactions: MissionInteraction[] | undefined) {
  const result: Record<string, string> = {};
  for (const interaction of interactions ?? []) {
    if (interaction.questionKey === "customerEvidence")
      result[interaction.missionId] = interaction.answerText;
  }
  return result;
}

/** Agrupa todas as respostas estruturadas para restaurar briefing e aplicação após recarga. */
function allInteractionsByMission(
  interactions: MissionInteraction[] | undefined,
) {
  const result: Record<string, Record<string, string>> = {};
  for (const interaction of interactions ?? []) {
    result[interaction.missionId] ??= {};
    result[interaction.missionId][interaction.questionKey] =
      interaction.answerText;
  }
  return result;
}

/** Resolve o token na URL ou no armazenamento segregado por produto. */
function accessTokenFromLocation(productSlug: string) {
  const pathToken = window.location.pathname.match(
    /^\/access\/([^/]+)\/?$/,
  )?.[1];
  if (pathToken) {
    const decodedToken = decodeURIComponent(pathToken);
    persistAccessToken(productSlug, decodedToken);
    return decodedToken;
  }
  const queryToken = new URLSearchParams(window.location.search).get("access");
  if (queryToken) {
    persistAccessToken(productSlug, queryToken);
    return queryToken;
  }
  return window.localStorage.getItem(storageKey(productSlug)) ?? "";
}

/** Persiste o acesso sem misturar produtos distintos no mesmo navegador. */
function persistAccessToken(productSlug: string, token: string) {
  window.localStorage.setItem(storageKey(productSlug), token);
}

/** Define a chave local estável e segregada por produto. */
function storageKey(productSlug: string) {
  return `pde-access:${productSlug}`;
}

/** Obtém mensagem de erro segura retornada pela API. */
async function responseMessage(response: Response) {
  try {
    const body = (await response.json()) as {
      message?: string;
      error?: string;
    };
    return (
      body.message || body.error || "A solicitação não pôde ser concluída."
    );
  } catch {
    return "A solicitação não pôde ser concluída.";
  }
}

/** Normaliza falhas desconhecidas para uma orientação legível. */
function readError(error: unknown) {
  return error instanceof Error
    ? error.message
    : "A solicitação não pôde ser concluída.";
}

type TrackingOptions = {
  accessToken?: string;
  email?: string;
  provider?: string;
  metadata?: Record<string, unknown>;
};

/** Registra observabilidade funcional sem bloquear o acesso ou a entrega do produto. */
async function trackEvent(
  eventType: string,
  product: ProductExperience,
  options: TrackingOptions = {},
) {
  try {
    await fetch("/api/pde/access/events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        productSlug: product.slug,
        eventType,
        accessToken: options.accessToken,
        email: options.email,
        provider: options.provider,
        source: testAccessEnabled ? "mh_test" : "pde-assisted-service",
        pageUrl: window.location.href,
        metadata: {
          visitorId: stableTrackingId(
            "pde-assisted-visitor",
            window.localStorage,
          ),
          sessionId: stableTrackingId(
            "pde-assisted-session",
            window.sessionStorage,
          ),
          experienceVersion: product.experienceVersion,
          layoutKey: product.layoutKey,
          deviceType: window.innerWidth < 768 ? "mobile" : "desktop",
          viewportWidth: window.innerWidth,
          viewportHeight: window.innerHeight,
          userAgent: navigator.userAgent,
          ...options.metadata,
        },
      }),
    });
  } catch {
    // A telemetria é auxiliar e nunca pode impedir a cliente de consumir o produto.
  }
}

/** Mantém identificadores first-party segregados sem incluir dados pessoais. */
function stableTrackingId(key: string, storage: Storage) {
  const existing = storage.getItem(key);
  if (existing) return existing;
  const generated = window.crypto.randomUUID();
  storage.setItem(key, generated);
  return generated;
}
