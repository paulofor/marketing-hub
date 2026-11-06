import { useState } from "react";
import axios from "axios";
import { Link } from "react-router-dom";
import { useSingleVideoBudget } from "../../api/financial/useSingleVideoBudget";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import { useVideoProjects } from "../../api/salesVideo/useVideoProjects";

/** Centraliza a decisão do vídeo único na demonstração financeira, mantendo produção separada. */
export default function SingleVideoBudgetPanel({
  productId,
  cycleId,
  chainId,
}: {
  productId: number;
  cycleId: number;
  chainId: number;
}) {
  const { query, authorize } = useSingleVideoBudget(
    productId,
    cycleId,
    chainId,
  );
  const projects = useVideoProjects();
  const [projectId, setProjectId] = useState("");
  const [limit, setLimit] = useState("");
  const [operator, setOperator] = useState("");
  const [source, setSource] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [requestKey, setRequestKey] = useState(() => createCycleRequestKey());
  const data = query.data;
  const candidates = (projects.data ?? []).filter(
    (p) =>
      p.productId === productId &&
      p.experimentId === data?.experimentId &&
      p.campaignKey === data?.productVersion &&
      p.salesVideoProfileId,
  );
  const selected = candidates.find((p) => p.id === Number(projectId));
  const existing = data?.authorizations.find(
    (a) => a.videoProjectId === selected?.id,
  );
  const error = authorize.error ?? query.error ?? projects.error;
  const edit = () => {
    setConfirmed(false);
    setRequestKey(createCycleRequestKey());
    authorize.reset();
  };
  return (
    <section
      className="card p-3 my-3"
      aria-label="Teto integral de um vídeo novo"
    >
      <h2 className="h5">Um único vídeo novo — teto integral em US$</h2>
      <p>
        O mesmo teto cobre agentes e revisões, imagens, clipes, voz, trilha,
        montagem, taxas e tentativas. Não é um valor por cena. Não inclui nem
        aprova gastos históricos; não autoriza mídia ou publicação.
      </p>
      {(query.isLoading || projects.isLoading) && (
        <p role="status">
          <span className="spinner-border spinner-border-sm" /> Consultando
          projetos e recibos…
        </p>
      )}
      {error && (
        <p role="alert">
          {axios.isAxiosError(error)
            ? (error.response?.data?.detail ??
              error.response?.data?.message ??
              "Não foi possível registrar. Atualize a consulta antes de tentar novamente.")
            : "Não foi possível consultar a autorização."}
        </p>
      )}
      {data && (
        <>
          {data.authorizations.map((a) => (
            <p key={a.eventId}>
              Recibo #{a.eventId} ·{" "}
              <Link to={`/audio-video-studio/projects/${a.videoProjectId}`}>
                Projeto #{a.videoProjectId}
              </Link>{" "}
              · US$ {a.totalLimitUsd.toFixed(2)} ·{" "}
              {a.current ? "Autorização registrada" : "Histórico"}
              <br />
              <small>{a.authorizationSource}</small>
            </p>
          ))}
          <p>{data.guidance}</p>
          {data.blocker && <p role="alert">{data.blocker}</p>}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (!selected?.salesVideoProfileId || !confirmed || existing)
                return;
              authorize.mutate({
                requestKey,
                expectedRevision: data.revision,
                chainDefinitionId: chainId,
                experimentId: data.experimentId,
                productVersion: data.productVersion,
                videoProjectId: selected.id,
                profileId: selected.salesVideoProfileId,
                totalLimitUsd: limit,
                operatorName: operator,
                authorizationSource: source,
                confirmed,
              });
            }}
          >
            <fieldset disabled={!data.canAuthorize || authorize.isPending}>
              <label className="d-block">
                Projeto existente *
                <select
                  className="form-select"
                  required
                  value={projectId}
                  onChange={(e) => {
                    edit();
                    setProjectId(e.target.value);
                  }}
                >
                  <option value="">Selecione o projeto desta versão</option>
                  {candidates.map((p) => (
                    <option key={p.id} value={p.id}>
                      #{p.id} · {p.title}
                    </option>
                  ))}
                </select>
              </label>
              {existing && (
                <p role="status">
                  Este projeto já tem um recibo. Reutilize-o; uma nova tentativa
                  não recebe outro teto.
                </p>
              )}
              <label className="d-block">
                Aprovado — teto total em US$ *
                <input
                  className="form-control"
                  type="number"
                  min="0.01"
                  max="999999.99"
                  step="0.01"
                  required
                  value={limit}
                  onChange={(e) => {
                    edit();
                    setLimit(e.target.value);
                  }}
                />
              </label>
              <label className="d-block">
                Operador que registra *
                <input
                  className="form-control"
                  required
                  maxLength={160}
                  value={operator}
                  onChange={(e) => {
                    edit();
                    setOperator(e.target.value);
                  }}
                />
              </label>
              <label className="d-block">
                Origem da autorização do cliente *
                <textarea
                  className="form-control"
                  required
                  maxLength={4000}
                  value={source}
                  onChange={(e) => {
                    edit();
                    setSource(e.target.value);
                  }}
                />
              </label>
              <label className="d-block my-2">
                <input
                  type="checkbox"
                  required
                  checked={confirmed}
                  onChange={(e) => setConfirmed(e.target.checked)}
                />{" "}
                Confirmo a autorização integral acima para um único vídeo novo
                neste projeto, incluindo revisões e tentativas, sem aprovar o
                histórico.
              </label>
              <button
                className="btn btn-primary"
                disabled={
                  !confirmed ||
                  !selected ||
                  Boolean(existing) ||
                  authorize.isPending
                }
              >
                {authorize.isPending && (
                  <span className="spinner-border spinner-border-sm" />
                )}{" "}
                Registrar autorização
              </button>
            </fieldset>
          </form>
        </>
      )}
    </section>
  );
}
