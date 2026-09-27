import { FormEvent, useMemo, useState } from "react";
import axios from "axios";
import { toast } from "react-toastify";
import type { Experiment } from "../../api/experiment/useExperiments";
import { useUploadExperimentAdVideo } from "../../api/experiment/useUploadExperimentAdVideo";

interface Props {
  experiment: Experiment;
  locked: boolean;
  metadataReader?: (file: File) => Promise<VideoMetadata>;
}

interface VideoMetadata {
  durationSeconds: number;
  width: number;
  height: number;
}

const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;

function readVideoMetadata(file: File) {
  return new Promise<VideoMetadata>((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const video = document.createElement("video");
    const release = () => URL.revokeObjectURL(objectUrl);
    video.preload = "metadata";
    video.onloadedmetadata = () => {
      const metadata = {
        durationSeconds: Math.ceil(video.duration),
        width: video.videoWidth,
        height: video.videoHeight,
      };
      release();
      resolve(metadata);
    };
    video.onerror = () => {
      release();
      reject(new Error("Não foi possível ler o MP4 selecionado."));
    };
    video.src = objectUrl;
    video.load();
  });
}

function isVerticalNineBySixteen(metadata: VideoMetadata) {
  if (!metadata.width || !metadata.height) return false;
  return Math.abs(metadata.width / metadata.height - 9 / 16) <= 0.03;
}

function parseSourceIds(value: string) {
  const parts = value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
  if (parts.length > 10 || parts.some((item) => !/^\d+$/.test(item))) {
    return undefined;
  }
  const ids = parts.map(Number);
  return ids.every((id) => Number.isSafeInteger(id) && id > 0) &&
    new Set(ids).size === ids.length
    ? ids
    : undefined;
}

export default function ExperimentAdVideoUploadPanel({
  experiment,
  locked,
  metadataReader = readVideoMetadata,
}: Props) {
  const upload = useUploadExperimentAdVideo(experiment.id);
  const [file, setFile] = useState<File>();
  const [metadata, setMetadata] = useState<VideoMetadata>();
  const [fileError, setFileError] = useState("");
  const [objective, setObjective] = useState(
    experiment.primaryVariable?.trim() ||
      "Comparar demonstração em vídeo com o criativo estático de controle.",
  );
  const [primaryMetric, setPrimaryMetric] = useState(
    experiment.primaryMetric?.trim() ||
      "Compras líquidas e contribuição após mídia (R$)",
  );
  const [script, setScript] = useState("");
  const [visualSourceKey, setVisualSourceKey] = useState("");
  const [visualSourceCreativeIds, setVisualSourceCreativeIds] = useState("");
  const [visualSourceVideoAssetIds, setVisualSourceVideoAssetIds] =
    useState("");
  const [visualSourceDescription, setVisualSourceDescription] = useState("");
  const [productionReference, setProductionReference] = useState("");
  const [hlsPlaybackUrl, setHlsPlaybackUrl] = useState("");
  const [audioConfirmed, setAudioConfirmed] = useState(false);

  const canSubmit = useMemo(() => {
    const creativeIds = parseSourceIds(visualSourceCreativeIds);
    const videoAssetIds = parseSourceIds(visualSourceVideoAssetIds);
    const sourceCount =
      creativeIds && videoAssetIds
        ? creativeIds.length + videoAssetIds.length
        : 0;
    return (
      !locked &&
      !upload.isPending &&
      Boolean(file) &&
      Boolean(metadata) &&
      !fileError &&
      audioConfirmed &&
      Boolean(objective.trim()) &&
      Boolean(primaryMetric.trim()) &&
      Boolean(script.trim()) &&
      Boolean(visualSourceKey.trim()) &&
      sourceCount > 0 &&
      sourceCount <= 10 &&
      Boolean(visualSourceDescription.trim()) &&
      Boolean(productionReference.trim())
    );
  }, [
    audioConfirmed,
    file,
    fileError,
    locked,
    metadata,
    objective,
    primaryMetric,
    productionReference,
    script,
    upload.isPending,
    visualSourceCreativeIds,
    visualSourceVideoAssetIds,
    visualSourceDescription,
    visualSourceKey,
  ]);

  async function selectFile(selected?: File) {
    setFile(undefined);
    setMetadata(undefined);
    setFileError("");
    if (!selected) return;
    if (!selected.name.toLowerCase().endsWith(".mp4")) {
      setFileError("Selecione um arquivo MP4.");
      return;
    }
    if (selected.size > MAX_FILE_SIZE_BYTES) {
      setFileError("O vídeo deve ter no máximo 50 MB.");
      return;
    }
    try {
      const detected = await metadataReader(selected);
      if (detected.durationSeconds < 6 || detected.durationSeconds > 60) {
        setFileError("O anúncio deve ter entre 6 e 60 segundos.");
        return;
      }
      if (!isVerticalNineBySixteen(detected)) {
        setFileError("O vídeo precisa estar no formato vertical 9:16.");
        return;
      }
      setFile(selected);
      setMetadata(detected);
    } catch (error) {
      setFileError(
        error instanceof Error
          ? error.message
          : "Não foi possível ler o MP4 selecionado.",
      );
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit || !file || !metadata) return;
    const sourceCreativeIds = parseSourceIds(visualSourceCreativeIds);
    const sourceVideoAssetIds = parseSourceIds(visualSourceVideoAssetIds);
    if (
      !sourceCreativeIds ||
      !sourceVideoAssetIds ||
      sourceCreativeIds.length + sourceVideoAssetIds.length === 0
    ) {
      return;
    }
    try {
      await upload.mutateAsync({
        file,
        objective: objective.trim(),
        primaryMetric: primaryMetric.trim(),
        script: script.trim(),
        durationSeconds: metadata.durationSeconds,
        hasAudio: true,
        visualSourceKey: visualSourceKey.trim(),
        visualSourceCreativeIds: sourceCreativeIds,
        visualSourceVideoAssetIds: sourceVideoAssetIds,
        visualSourceDescription: visualSourceDescription.trim(),
        productionReference: productionReference.trim(),
        hlsPlaybackUrl: hlsPlaybackUrl.trim(),
        requiredForRelease: true,
      });
      toast.success("Vídeo enviado e mantido pendente de revisão humana.");
      setFile(undefined);
      setMetadata(undefined);
      setAudioConfirmed(false);
    } catch (cause) {
      setFileError(
        axios.isAxiosError(cause)
          ? cause.response?.data?.message ||
              cause.response?.data?.detail ||
              "Não foi possível enviar o vídeo."
          : "Não foi possível enviar o vídeo.",
      );
    }
  }

  return (
    <section
      className="card"
      aria-labelledby="experiment-ad-video-upload-title"
    >
      <div className="card-body">
        <h5 id="experiment-ad-video-upload-title" className="card-title">
          Anexar vídeo vertical finalizado
        </h5>
        <p className="text-muted small">
          Use um MP4 9:16 produzido a partir de ativos autorizados. O upload não
          gera custo e não publica campanha; a peça ainda passa por revisão
          humana e técnica.
        </p>
        <form className="d-grid gap-3" onSubmit={submit}>
          <label className="form-label">
            Arquivo MP4
            <input
              className="form-control"
              type="file"
              accept="video/mp4,.mp4"
              disabled={locked || upload.isPending}
              onChange={(event) => void selectFile(event.target.files?.[0])}
            />
          </label>
          {metadata ? (
            <p className="small mb-0" role="status">
              {metadata.width}×{metadata.height} · {metadata.durationSeconds}s ·
              vertical 9:16
            </p>
          ) : null}
          <label className="form-label">
            Objetivo do teste
            <input
              className="form-control"
              value={objective}
              onChange={(event) => setObjective(event.target.value)}
            />
          </label>
          <label className="form-label">
            Métrica primária
            <input
              className="form-control"
              value={primaryMetric}
              onChange={(event) => setPrimaryMetric(event.target.value)}
            />
          </label>
          <label className="form-label">
            Roteiro exibido no vídeo
            <textarea
              className="form-control"
              rows={5}
              value={script}
              onChange={(event) => setScript(event.target.value)}
            />
          </label>
          <label className="form-label">
            Chave dos ativos visuais de origem
            <input
              className="form-control"
              placeholder="produto-experimento-assets-v1"
              value={visualSourceKey}
              onChange={(event) => setVisualSourceKey(event.target.value)}
            />
          </label>
          <label className="form-label" htmlFor="video-source-creative-ids">
            IDs dos criativos aprovados usados no vídeo
          </label>
          <input
            id="video-source-creative-ids"
            className="form-control"
            inputMode="numeric"
            placeholder="522, 523"
            aria-describedby="video-source-creative-ids-help"
            value={visualSourceCreativeIds}
            onChange={(event) => setVisualSourceCreativeIds(event.target.value)}
          />
          <span id="video-source-creative-ids-help" className="form-text">
            Opcional quando a montagem usa um vídeo já aprovado. Separe os IDs
            por vírgula.
          </span>
          <label className="form-label" htmlFor="video-source-asset-ids">
            IDs dos vídeos aprovados usados como fonte
          </label>
          <input
            id="video-source-asset-ids"
            className="form-control"
            inputMode="numeric"
            placeholder="47"
            aria-describedby="video-source-asset-ids-help"
            value={visualSourceVideoAssetIds}
            onChange={(event) =>
              setVisualSourceVideoAssetIds(event.target.value)
            }
          />
          <span id="video-source-asset-ids-help" className="form-text">
            Informe ao menos uma fonte aprovada entre criativos e vídeos, com no
            máximo 10 IDs no total. O backend confirma aprovação, produto e
            experimento de origem.
          </span>
          <label className="form-label">
            Evidência dos ativos usados
            <textarea
              className="form-control"
              rows={3}
              value={visualSourceDescription}
              onChange={(event) =>
                setVisualSourceDescription(event.target.value)
              }
            />
          </label>
          <label className="form-label">
            Referência versionada da produção
            <input
              className="form-control"
              placeholder="scripts/.../criar-video-v1.sh"
              value={productionReference}
              onChange={(event) => setProductionReference(event.target.value)}
            />
          </label>
          <label className="form-label" htmlFor="video-hls-playback-url">
            HLS público do vídeo
          </label>
          <input
            id="video-hls-playback-url"
            aria-describedby="video-hls-playback-url-help"
            className="form-control"
            type="url"
            placeholder="https://.../index.m3u8"
            value={hlsPlaybackUrl}
            onChange={(event) => setHlsPlaybackUrl(event.target.value)}
          />
          <span className="form-text" id="video-hls-playback-url-help">
            Opcional para anúncio e obrigatório quando a peça será usada em uma
            experiência PDE pública.
          </span>
          <label className="form-check">
            <input
              className="form-check-input"
              type="checkbox"
              checked={audioConfirmed}
              onChange={(event) => setAudioConfirmed(event.target.checked)}
            />
            <span className="form-check-label">
              Confirmei a reprodução e o arquivo possui áudio utilizável.
            </span>
          </label>
          {fileError ? (
            <div className="alert alert-danger mb-0" role="alert">
              {fileError}
            </div>
          ) : null}
          <button
            type="submit"
            className="btn btn-primary justify-self-start"
            disabled={!canSubmit}
          >
            {upload.isPending
              ? "Enviando..."
              : "Enviar para revisão do experimento"}
          </button>
        </form>
      </div>
    </section>
  );
}
