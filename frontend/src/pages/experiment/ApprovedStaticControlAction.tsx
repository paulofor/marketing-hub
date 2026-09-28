import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import axios from "axios";
import type { Experiment } from "../../api/experiment/useExperiments";
import {
  type ApprovedVisualAssetOption,
  useCreateApprovedVisualAssetCreative,
} from "../../api/creative/useApprovedVisualAssetCreatives";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import {
  CREATIVE_PUBLICATION_COPY_LIMITS,
  plannedCreativeCopy,
  publicationCopyLength,
} from "./creativePublicationCopy";

interface Props {
  experiment: Experiment;
  asset: ApprovedVisualAssetOption;
  locked: boolean;
  alreadyLinkedCreativeId?: number;
}

export default function ApprovedStaticControlAction({
  experiment,
  asset,
  locked,
  alreadyLinkedCreativeId,
}: Props) {
  const [open, setOpen] = useState(false);
  const [copy, setCopy] = useState(() => plannedCreativeCopy(experiment));
  const [error, setError] = useState("");
  const [createdId, setCreatedId] = useState<number>();
  const create = useCreateApprovedVisualAssetCreative(experiment.id, asset.id);

  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, [open]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (locked || create.isPending || alreadyLinkedCreativeId) return;
    setError("");
    try {
      const result = await create.mutateAsync(copy);
      setCreatedId(result.id);
      setOpen(false);
    } catch (cause) {
      setError(
        axios.isAxiosError(cause)
          ? cause.response?.data?.message ||
              cause.response?.data?.detail ||
              "Não foi possível vincular o controle. A mesma seleção não será duplicada."
          : "Não foi possível vincular o controle. Tente novamente.",
      );
    }
  }

  if (alreadyLinkedCreativeId) {
    return (
      <div role="status" className="small text-success mt-2">
        Controle vinculado ao anúncio #{alreadyLinkedCreativeId}. Copy e gates
        devem ser tratados no cartão desse anúncio.
      </div>
    );
  }

  return (
    <>
      <button
        type="button"
        className="btn btn-sm btn-outline-primary mt-2"
        disabled={locked || !experiment.instagramAccount}
        onClick={() => {
          setCopy(plannedCreativeCopy(experiment));
          setOpen(true);
          setError("");
        }}
      >
        Usar como controle estático
      </button>
      {!experiment.instagramAccount && (
        <div className="small text-warning mt-2">
          Salve primeiro a identidade oficial do Instagram.
        </div>
      )}
      {createdId && (
        <div role="status" className="small text-success mt-2">
          Anúncio #{createdId} cadastrado e enviado para revisão.
        </div>
      )}
      {open &&
        createPortal(
          <div
            className="modal d-block"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`static-control-title-${asset.id}`}
            tabIndex={-1}
            style={{ background: "rgba(0,0,0,.45)", height: "100dvh" }}
          >
            <div
              className="modal-dialog modal-dialog-scrollable"
              style={{
                maxWidth: "min(540px, calc(100vw - 1rem))",
                maxHeight: "calc(100dvh - 1rem)",
              }}
            >
              <form className="modal-content" onSubmit={submit}>
                <div className="modal-header">
                  <h2
                    className="modal-title h5"
                    id={`static-control-title-${asset.id}`}
                  >
                    Usar ativo #{asset.id} como controle estático
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
                    Os pixels e o SHA-256 aprovados serão preservados. A nova
                    combinação de imagem, texto e destino ainda passará por
                    Têmis e pela aprovação humana.
                  </p>
                  <img
                    src={resolveAssetUrl(asset.assetUrl)}
                    alt={asset.label}
                    style={{
                      maxHeight: 300,
                      width: "100%",
                      objectFit: "contain",
                    }}
                  />
                  <div className="small text-muted text-break">
                    <strong>{asset.label}</strong>
                    <br />
                    Plano #{asset.commercialPlanId} · SHA-256{" "}
                    {asset.contentSha256}
                  </div>
                  <label className="form-label">
                    Título do anúncio
                    <input
                      required
                      aria-label="Título do controle estático"
                      maxLength={CREATIVE_PUBLICATION_COPY_LIMITS.headline}
                      className="form-control"
                      value={copy.headline}
                      onChange={(event) =>
                        setCopy({ ...copy, headline: event.target.value })
                      }
                    />
                    <span className="form-text">
                      {publicationCopyLength(copy.headline)}/
                      {CREATIVE_PUBLICATION_COPY_LIMITS.headline}
                    </span>
                  </label>
                  <label className="form-label">
                    Texto principal
                    <textarea
                      required
                      aria-label="Texto principal do controle estático"
                      maxLength={CREATIVE_PUBLICATION_COPY_LIMITS.primaryText}
                      rows={4}
                      className="form-control"
                      value={copy.primaryText}
                      onChange={(event) =>
                        setCopy({ ...copy, primaryText: event.target.value })
                      }
                    />
                    <span className="form-text">
                      {publicationCopyLength(copy.primaryText)}/
                      {CREATIVE_PUBLICATION_COPY_LIMITS.primaryText}
                    </span>
                  </label>
                  <label className="form-label">
                    Descrição curta (opcional)
                    <input
                      aria-label="Descrição do controle estático"
                      maxLength={CREATIVE_PUBLICATION_COPY_LIMITS.description}
                      className="form-control"
                      value={copy.description}
                      onChange={(event) =>
                        setCopy({ ...copy, description: event.target.value })
                      }
                    />
                    <span className="form-text">
                      {publicationCopyLength(copy.description)}/
                      {CREATIVE_PUBLICATION_COPY_LIMITS.description}
                    </span>
                  </label>
                  <div className="small">
                    Botão: <strong>Saiba mais</strong>
                    <br />
                    Destino: {experiment.followUpActionUrl}
                  </div>
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
