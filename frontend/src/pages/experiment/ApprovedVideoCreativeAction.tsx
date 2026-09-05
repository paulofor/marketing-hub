import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";
import axios from "axios";
import type { Experiment } from "../../api/experiment/useExperiments";
import type { ExperimentVideoAsset } from "../../api/experiment/useExperimentVideoAssets";
import { useCreateVideoCreative } from "../../api/creative/useCreateVideoCreative";

interface Props {
  experiment: Experiment;
  video: ExperimentVideoAsset;
  videos: ExperimentVideoAsset[];
  locked: boolean;
}

function plannedCopy(experiment: Experiment) {
  try {
    const plan = JSON.parse(experiment.adCopy || "{}");
    const variant = (plan.adCopy ?? plan).primaryTextVariants?.[0];
    return {
      headline: variant?.headline || "",
      primaryText: (
        variant?.lengthVariants?.media ||
        variant?.primaryText ||
        ""
      ).replace(/\\n/g, "\n"),
      description: variant?.description || "",
    };
  } catch {
    return { headline: "", primaryText: "", description: "" };
  }
}

export default function ApprovedVideoCreativeAction({
  experiment,
  video,
  videos,
  locked,
}: Props) {
  const [open, setOpen] = useState(false);
  const [copy, setCopy] = useState(() => plannedCopy(experiment));
  const [replacement, setReplacement] = useState("");
  const [error, setError] = useState("");
  const [createdId, setCreatedId] = useState<number>();
  const create = useCreateVideoCreative(experiment.id, video.id);
  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, [open]);
  const replacements = videos.filter(
    (candidate) =>
      candidate.id !== video.id &&
      candidate.slot === "AD" &&
      candidate.reviewStatus === "REJECTED" &&
      candidate.requiredForRelease,
  );

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (locked || create.isPending) return;
    setError("");
    try {
      const result = await create.mutateAsync({
        ...copy,
        replacesVideoAssetId: replacement ? Number(replacement) : undefined,
      });
      setCreatedId(result.id);
      setOpen(false);
    } catch (cause) {
      setError(
        axios.isAxiosError(cause)
          ? cause.response?.data?.message ||
              cause.response?.data?.detail ||
              "Não foi possível cadastrar o anúncio. Tente novamente; a mesma seleção não será duplicada."
          : "Não foi possível cadastrar o anúncio. Tente novamente.",
      );
    }
  }

  return (
    <>
      <button
        type="button"
        className="btn btn-sm btn-primary mt-2"
        disabled={locked}
        onClick={() => {
          setOpen(true);
          setError("");
        }}
      >
        Usar vídeo em anúncio
      </button>
      {createdId && (
        <div role="status" className="small mt-2">
          Anúncio #{createdId} cadastrado. Acompanhe a revisão e a aprovação
          final em{" "}
          <Link to={`/experiments/${experiment.id}?tab=creatives`}>
            Criativos
          </Link>
          .
        </div>
      )}
      {open &&
        createPortal(
          <div
            className="modal d-block"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`video-creative-title-${video.id}`}
            tabIndex={-1}
            style={{ background: "rgba(0,0,0,.45)", height: "100dvh" }}
          >
            <div
              className="modal-dialog modal-dialog-scrollable"
              style={{
                maxWidth: "min(500px, calc(100vw - 1rem))",
                maxHeight: "calc(100dvh - 1rem)",
              }}
            >
              <form className="modal-content" onSubmit={submit}>
                <div className="modal-header">
                  <h2
                    className="modal-title h5"
                    id={`video-creative-title-${video.id}`}
                  >
                    Usar vídeo #{video.id} em anúncio
                  </h2>
                  <button
                    type="button"
                    className="btn-close"
                    aria-label="Fechar"
                    disabled={create.isPending}
                    onClick={() => setOpen(false)}
                  />
                </div>
                <div className="modal-body d-grid gap-3">
                  <p className="mb-0">
                    Revise o texto do plano. O vídeo aprovado será enviado a
                    Têmis junto com a mensagem e o destino. Depois, o anúncio
                    precisa de aprovação final.
                  </p>
                  <video
                    src={video.assetUrl || undefined}
                    controls
                    preload="metadata"
                    poster={video.thumbnailUrl || undefined}
                    style={{ maxHeight: 260, width: "100%" }}
                  />
                  <label className="form-label">
                    Título do anúncio
                    <input
                      required
                      maxLength={255}
                      className="form-control"
                      value={copy.headline}
                      onChange={(e) =>
                        setCopy({ ...copy, headline: e.target.value })
                      }
                    />
                  </label>
                  <label className="form-label">
                    Texto principal
                    <textarea
                      required
                      maxLength={5000}
                      rows={4}
                      className="form-control"
                      value={copy.primaryText}
                      onChange={(e) =>
                        setCopy({ ...copy, primaryText: e.target.value })
                      }
                    />
                  </label>
                  <label className="form-label">
                    Descrição curta (opcional)
                    <input
                      maxLength={255}
                      className="form-control"
                      value={copy.description}
                      onChange={(e) =>
                        setCopy({ ...copy, description: e.target.value })
                      }
                    />
                  </label>
                  {replacements.length > 0 && (
                    <label className="form-label">
                      Vídeo reprovado que esta peça substitui
                      <select
                        className="form-select"
                        value={replacement}
                        onChange={(e) => setReplacement(e.target.value)}
                      >
                        <option value="">Não substituir outro vídeo</option>
                        {replacements.map((candidate) => (
                          <option key={candidate.id} value={candidate.id}>
                            Vídeo #{candidate.id} — reprovado
                          </option>
                        ))}
                      </select>
                      <span className="form-text">
                        O vídeo escolhido continua reprovado no histórico e
                        deixa de ser exigido para esta campanha.
                      </span>
                    </label>
                  )}
                  <div className="small">
                    Botão: <strong>Saiba mais</strong>
                    <br />
                    Destino: {experiment.followUpActionUrl}
                  </div>
                  <p className="small text-muted mb-0">
                    Esta etapa aproveita a mídia existente. O orçamento e o
                    período da campanha permanecem os cadastrados no
                    experimento.
                  </p>
                  {error && (
                    <div className="alert alert-danger mb-0" role="alert">
                      {error}
                    </div>
                  )}
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    disabled={create.isPending}
                    onClick={() => setOpen(false)}
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={
                      create.isPending ||
                      locked ||
                      !copy.headline.trim() ||
                      !copy.primaryText.trim() ||
                      !experiment.followUpActionUrl
                    }
                  >
                    {create.isPending
                      ? "Cadastrando..."
                      : "Cadastrar e enviar para revisão"}
                  </button>
                </div>
              </form>
            </div>
          </div>,
          document.body,
        )}
    </>
  );
}
