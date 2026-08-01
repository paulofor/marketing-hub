import {
  ArrowLeft,
  BarChart3,
  CheckCircle2,
  Clapperboard,
  ExternalLink,
  FileText,
  Lightbulb,
  Target,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useVideoReference } from "../../api/salesVideo/useVideoReferences";
import type { VideoReference } from "../../api/salesVideo/types";
import PageTitle from "../../components/PageTitle";
import { getStudioCommercialLabel } from "./audioVideoStudioLabels";
import "./AudioVideoStudioPage.css";

type AnalysisStage = {
  key: string;
  title: string;
  section: string;
  description: string;
  icon: typeof FileText;
  content: string[];
};

const fallbackStages = [
  {
    key: "evidencias",
    title: "1. Evidencias usadas",
    section: "Base da analise",
    description:
      "Dados técnicos, origem do vídeo, duração, formato, ritmo visual e sinais auditivos usados como prova.",
    icon: FileText,
  },
  {
    key: "diagnostico",
    title: "2. Diagnostico comercial",
    section: "Papel no funil",
    description:
      "Leitura de objetivo, tensão, promessa, gancho, emoção, prova e potencial de conversão.",
    icon: Target,
  },
  {
    key: "sequencia",
    title: "3. Analise por sequencia",
    section: "Frame a frame",
    description:
      "Quebra por blocos de tempo para entender cortes, viradas, repetição e recompensa visual.",
    icon: Clapperboard,
  },
  {
    key: "aprendizado",
    title: "4. Aprendizados do sistema",
    section: "Padrões vencedores",
    description:
      "Mecanismos que devem alimentar futuros roteiros, criativos, provas, CTAs e templates.",
    icon: Lightbulb,
  },
  {
    key: "melhorias",
    title: "5. Melhorias acionaveis",
    section: "Uso em vendas",
    description:
      "Ações práticas para transformar retenção em clique, cadastro, checkout ou compra.",
    icon: CheckCircle2,
  },
  {
    key: "decisao",
    title: "6. Decisao operacional",
    section: "Proximo movimento",
    description:
      "Síntese da abordagem escolhida, tradeoffs e como reaproveitar o aprendizado comercial.",
    icon: BarChart3,
  },
];

function markdownLines(section?: string | null) {
  if (!section) {
    return [];
  }

  return section
    .split("\n")
    .map((line) =>
      line
        .replace(/^[-*]\s+/, "")
        .replace(/^\d+\.\s+/, "")
        .replace(/\*\*/g, "")
        .replace(/`/g, "")
        .trim(),
    )
    .filter(Boolean);
}

function findMarkdownSection(notes: string, titles: string[]) {
  const escapedTitles = titles.map((title) =>
    title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
  );
  const titlePattern = escapedTitles.join("|");
  const match = notes.match(
    new RegExp(
      `(?:^|\\n)\\*\\*(?:${titlePattern})\\*\\*\\s*\\n([\\s\\S]*?)(?=\\n\\*\\*|$)`,
      "i",
    ),
  );
  return markdownLines(match?.[1]);
}

function uniqueLines(lines: string[]) {
  return Array.from(new Set(lines));
}

function buildStages(reference: VideoReference): AnalysisStage[] {
  const notes = reference.analysisNotes ?? "";
  const sectionContent: Record<string, string[]> = {
    evidencias: findMarkdownSection(notes, [
      "Evidências usadas",
      "Evidencias usadas",
    ]),
    diagnostico: findMarkdownSection(notes, [
      "Diagnóstico comercial",
      "Diagnostico comercial",
    ]),
    sequencia: findMarkdownSection(notes, [
      "Análise por sequência",
      "Analise por sequencia",
    ]),
    aprendizado: findMarkdownSection(notes, [
      "O que o sistema deve aprender desse vídeo",
      "O que o sistema deve aprender desse video",
    ]),
    melhorias: findMarkdownSection(notes, [
      "Melhorias acionáveis para usar em vendas",
      "Melhorias acionaveis para usar em vendas",
    ]),
    decisao: uniqueLines(
      findMarkdownSection(notes, ["Alternativas avaliadas"]).concat(
        markdownLines(
          notes.match(/Escolhi\s+a\s+terceira\s+abordagem[\s\S]*$/i)?.[0],
        ),
      ),
    ),
  };

  return fallbackStages.map((stage) => ({
    ...stage,
    content: sectionContent[stage.key] ?? [],
  }));
}

function getReferenceSummary(reference: VideoReference) {
  if (reference.analysisNotes) {
    return "Resultado de analise disponível para transformar vídeo vencedor em padrão reutilizável de criativo, roteiro e CTA.";
  }

  if (reference.status === "ANALYZED") {
    return "Video marcado como analisado, mas ainda sem relatório estruturado disponível.";
  }

  return "O resultado aparecerá aqui quando a análise comercial do vídeo for registrada pelo sistema.";
}

export default function AudioVideoStudioVideoAnalysisResultPage() {
  const { referenceId } = useParams();
  const referenceQuery = useVideoReference(referenceId);
  const reference = referenceQuery.data;
  const stages = reference ? buildStages(reference) : [];

  return (
    <div className="audio-video-studio-page">
      <Link
        className="audio-video-studio-page__secondary-action audio-video-studio-page__back-link"
        to="/audio-video-studio/videos-analysis"
      >
        <ArrowLeft size={16} aria-hidden="true" />
        Voltar para videos
      </Link>

      <PageTitle
        title="Resultado da analise"
        subtitle="Veja o que o sistema aprendeu com o video de referencia e como reaproveitar isso em criativos comerciais."
      />

      {referenceQuery.isLoading ? (
        <article className="audio-video-studio-page__project-card">
          Carregando resultado da analise...
        </article>
      ) : referenceQuery.isError || !reference ? (
        <article className="audio-video-studio-page__project-card">
          Nao foi possivel carregar o resultado da analise agora.
        </article>
      ) : (
        <>
          <section className="audio-video-studio-page__intro">
            <div>
              <p className="audio-video-studio-page__eyebrow">
                Video de referencia #{reference.id}
              </p>
              <h2>{reference.title}</h2>
              <p>{getReferenceSummary(reference)}</p>
            </div>
            <article
              className="audio-video-studio-page__status"
              aria-label="Status da analise"
            >
              <BarChart3 size={22} aria-hidden="true" />
              <span>Status</span>
              <strong>{getStudioCommercialLabel(reference.status)}</strong>
              <small>
                {reference.niche || "Nicho nao informado"} ·{" "}
                {reference.funnelStage
                  ? getStudioCommercialLabel(reference.funnelStage)
                  : "Funil nao informado"}
              </small>
            </article>
          </section>

          <section className="audio-video-studio-page__workflow">
            {stages.map((stage, index) => (
              <article
                className="audio-video-studio-page__workflow-step"
                key={stage.key}
              >
                <span>{index + 1}</span>
                <strong>{stage.section}</strong>
                <small>{stage.description}</small>
              </article>
            ))}
          </section>

          <section className="audio-video-studio-page__section">
            <div className="audio-video-studio-page__section-heading audio-video-studio-page__section-heading--actions">
              <div>
                <h2>Etapas do resultado</h2>
                <p>
                  A estrutura segue a lógica do estúdio principal: primeiro
                  evidência, depois diagnóstico, sequência, aprendizado e ação.
                </p>
              </div>
              <a
                className="audio-video-studio-page__secondary-action"
                href={reference.sourceUrl}
                target="_blank"
                rel="noreferrer"
              >
                <ExternalLink size={16} aria-hidden="true" />
                Abrir video
              </a>
            </div>

            <div className="audio-video-studio-page__stage-grid">
              {stages.map((stage) => (
                <article
                  className="audio-video-studio-page__stage-card audio-video-studio-page__analysis-stage-card"
                  key={stage.key}
                >
                  <div className="audio-video-studio-page__stage-card-header">
                    <span>{stage.section}</span>
                    <stage.icon size={22} aria-hidden="true" />
                  </div>
                  <h3>{stage.title}</h3>
                  {stage.content.length > 0 ? (
                    <ul className="audio-video-studio-page__analysis-list">
                      {stage.content.map((item, index) => (
                        <li key={`${stage.key}-${index}`}>{item}</li>
                      ))}
                    </ul>
                  ) : (
                    <p>
                      Esta etapa ainda não recebeu dados estruturados da
                      análise.
                    </p>
                  )}
                </article>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
