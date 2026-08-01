import { FormEvent, useState } from "react";
import { BarChart3, ExternalLink, PlusCircle, Upload } from "lucide-react";
import { Link } from "react-router-dom";
import {
  useCreateVideoReference,
  useVideoReferences,
} from "../../api/salesVideo/useVideoReferences";
import PageTitle from "../../components/PageTitle";
import { getStudioCommercialLabel } from "./audioVideoStudioLabels";
import "./AudioVideoStudioPage.css";

const initialForm = {
  title: "",
  sourceUrl: "",
  sourcePlatform: "",
  niche: "",
  funnelStage: "",
  primaryLearningGoal: "",
  successEvidence: "",
  createdBy: "operador@marketinghub.io",
};

function formatDate(value?: string | null) {
  if (!value) {
    return "Sem data";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function getStatusLearningAction(status: string) {
  if (status === "ANALYZED") {
    return "Aprendizado pronto para reaproveitar em roteiro, gancho e CTA.";
  }

  if (status === "ANALYZING") {
    return "Sistema analisando estrutura, retencao, prova e chamada.";
  }

  if (status === "REJECTED") {
    return "Referencia bloqueada; revisar URL, direitos de uso ou relevancia.";
  }

  return "Na fila para extrair gancho, ritmo, prova, objecoes e CTA.";
}

export default function AudioVideoStudioVideosAnalysisPage() {
  const referencesQuery = useVideoReferences();
  const createReference = useCreateVideoReference();
  const [form, setForm] = useState(initialForm);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileInputVersion, setFileInputVersion] = useState(0);
  const [formError, setFormError] = useState("");

  const references = referencesQuery.data ?? [];

  function updateField(field: keyof typeof initialForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError("");
    if (!selectedFile && !form.sourceUrl.trim()) {
      setFormError("Envie um arquivo de video ou informe uma URL publica.");
      return;
    }

    const basePayload = {
      title: form.title,
      sourcePlatform: form.sourcePlatform,
      niche: form.niche,
      funnelStage: form.funnelStage,
      primaryLearningGoal: form.primaryLearningGoal,
      successEvidence: form.successEvidence,
      createdBy: form.createdBy,
    };

    createReference.mutate(
      selectedFile
        ? {
            ...basePayload,
            file: selectedFile,
          }
        : {
            ...basePayload,
            sourceUrl: form.sourceUrl,
          },
      {
        onSuccess: () => {
          setForm(initialForm);
          setSelectedFile(null);
          setFileInputVersion((current) => current + 1);
        },
      },
    );
  }

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title="Videos para analise"
        subtitle="Envie videos de sucesso para o sistema analisar e transformar em aprendizado de gancho, ritmo, prova, oferta e CTA."
      />

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <div>
            <h2>Enviar video de referencia</h2>
            <p>
              Cadastre videos externos que funcionaram no mercado para alimentar
              a fila de aprendizado do Estudio de Audio e Video.
            </p>
          </div>
        </div>

        <form
          className="audio-video-studio-page__reference-form"
          onSubmit={handleSubmit}
          aria-label="Enviar video para analise"
        >
          <label className="audio-video-studio-page__reference-form-wide">
            Arquivo do video
            <input
              key={fileInputVersion}
              type="file"
              accept="video/mp4,video/quicktime,video/webm,video/x-m4v"
              onChange={(event) =>
                setSelectedFile(event.target.files?.[0] ?? null)
              }
            />
            <small>
              Envie MP4, MOV, WEBM ou M4V. O arquivo sera armazenado e colocado
              na fila de aprendizado.
            </small>
          </label>

          <label>
            Titulo do video
            <input
              value={form.title}
              onChange={(event) => updateField("title", event.target.value)}
              placeholder="Ex.: Reels com gancho de transformacao imediata"
              required
            />
          </label>

          <label>
            URL publica do video, se nao fizer upload
            <input
              value={form.sourceUrl}
              onChange={(event) => updateField("sourceUrl", event.target.value)}
              placeholder="https://..."
            />
          </label>

          <label>
            Plataforma
            <input
              value={form.sourcePlatform}
              onChange={(event) =>
                updateField("sourcePlatform", event.target.value)
              }
              placeholder="TikTok, Instagram, YouTube, Drive..."
            />
          </label>

          <label>
            Nicho ou produto
            <input
              value={form.niche}
              onChange={(event) => updateField("niche", event.target.value)}
              placeholder="Ex.: beleza, fitness, produtividade, MUSA"
            />
          </label>

          <label>
            Papel no funil
            <input
              value={form.funnelStage}
              onChange={(event) =>
                updateField("funnelStage", event.target.value)
              }
              placeholder="Topo, retargeting, landing, checkout..."
            />
          </label>

          <label>
            O que queremos aprender
            <textarea
              value={form.primaryLearningGoal}
              onChange={(event) =>
                updateField("primaryLearningGoal", event.target.value)
              }
              placeholder="Ex.: entender como o gancho prende atencao nos 3 primeiros segundos e como o CTA reduz esforco"
              rows={4}
              required
            />
          </label>

          <label className="audio-video-studio-page__reference-form-wide">
            Evidencia de sucesso
            <textarea
              value={form.successEvidence}
              onChange={(event) =>
                updateField("successEvidence", event.target.value)
              }
              placeholder="Views, comentarios, vendas, compartilhamentos, campanha onde apareceu, observacoes do operador..."
              rows={4}
            />
          </label>

          <button
            className="audio-video-studio-page__primary-action"
            type="submit"
            disabled={createReference.isPending}
          >
            {selectedFile ? (
              <Upload size={18} aria-hidden="true" />
            ) : (
              <PlusCircle size={18} aria-hidden="true" />
            )}
            {createReference.isPending
              ? "Enviando para analise..."
              : "Enviar para analise"}
          </button>
        </form>

        {formError ? (
          <p className="audio-video-studio-page__duration-block">{formError}</p>
        ) : null}
        {createReference.isSuccess ? (
          <p className="audio-video-studio-page__feedback">
            Video enviado para a fila de analise.
          </p>
        ) : null}
        {createReference.isError ? (
          <p className="audio-video-studio-page__duration-block">
            Nao foi possivel enviar o video para analise agora.
          </p>
        ) : null}
      </section>

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <div>
            <h2>Fila de aprendizado</h2>
            <p>
              Cada item deve virar aprendizado reutilizavel para criativos,
              roteiros, ofertas, provas e novos cortes comerciais.
            </p>
          </div>
        </div>

        {referencesQuery.isLoading ? (
          <article className="audio-video-studio-page__project-card">
            Carregando videos enviados para analise...
          </article>
        ) : referencesQuery.isError ? (
          <article className="audio-video-studio-page__project-card">
            Nao foi possivel carregar a fila de analise agora.
          </article>
        ) : references.length === 0 ? (
          <article className="audio-video-studio-page__project-card">
            Nenhum video de referencia enviado para analise.
          </article>
        ) : (
          <div className="audio-video-studio-page__project-table-wrapper">
            <table className="audio-video-studio-page__project-table">
              <thead>
                <tr>
                  <th>Video enviado</th>
                  <th>Origem</th>
                  <th>Aprendizado desejado</th>
                  <th>Status</th>
                  <th>Enviado em</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {references.map((reference) => (
                  <tr key={reference.id}>
                    <td>
                      <strong>#{reference.id}</strong>
                      <span>{reference.title}</span>
                      <small>{reference.niche || "Nicho nao informado"}</small>
                    </td>
                    <td>
                      {reference.sourcePlatform || "Origem nao informada"}
                      <small>
                        Funil:{" "}
                        {reference.funnelStage
                          ? getStudioCommercialLabel(reference.funnelStage)
                          : "Nao informado"}
                      </small>
                    </td>
                    <td>
                      <span>{reference.primaryLearningGoal}</span>
                      {reference.successEvidence ? (
                        <small>{reference.successEvidence}</small>
                      ) : null}
                    </td>
                    <td>
                      {getStudioCommercialLabel(reference.status)}
                      <small>{getStatusLearningAction(reference.status)}</small>
                    </td>
                    <td>{formatDate(reference.createdAt)}</td>
                    <td>
                      <Link
                        className="audio-video-studio-page__project-open-link"
                        to={`/audio-video-studio/videos-analysis/${reference.id}/results`}
                      >
                        <BarChart3 size={16} aria-hidden="true" />
                        <span>Ver analise</span>
                      </Link>
                      <a
                        className="audio-video-studio-page__project-open-link"
                        href={reference.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <ExternalLink size={16} aria-hidden="true" />
                        <span>Abrir video</span>
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
