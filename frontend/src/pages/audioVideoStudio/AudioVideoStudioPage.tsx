import {
  Clapperboard,
  FileText,
  Music,
  Scissors,
  Sparkles,
  Volume2,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import "./AudioVideoStudioPage.css";

const productionPillars = [
  {
    icon: FileText,
    title: "Roteiro longo",
    description:
      "Estrutura narrativa por atos, promessa, progressao emocional e CTA comercial.",
  },
  {
    icon: Clapperboard,
    title: "Varias cenas",
    description:
      "Planejamento de takes, continuidade visual, ritmo e funcao de cada cena.",
  },
  {
    icon: Volume2,
    title: "Narracao",
    description:
      "Direcao de voz, pausas, enfase e alinhamento com o nivel de sofisticacao da oferta.",
  },
  {
    icon: Music,
    title: "Trilha sonora",
    description:
      "Camada sonora planejada para aumentar retencao, desejo e percepcao premium.",
  },
  {
    icon: Scissors,
    title: "Pos-producao",
    description:
      "Montagem, cortes, legendas, revisao editorial e acabamento antes de publicar.",
  },
  {
    icon: Sparkles,
    title: "PDE premium",
    description:
      "Videos para elevar valor percebido de produtos digitais com IA aplicada ao dia a dia.",
  },
];

const currentFlows = [
  "Criativos de experimentos continuam nas telas de experimentos e campanhas.",
  "Videos para PDEs continuam dentro dos produtos e jornadas especificas.",
  "Videos organicos curtos continuam nos fluxos atuais de producao rapida.",
  "Aprovacao e provedores seguem nas areas operacionais ja existentes.",
];

const buildSteps = [
  "Briefing audiovisual com objetivo comercial e publico.",
  "Roteiro estruturado por cenas e funcao de cada bloco.",
  "Mapa de referencias visuais, personagens, cenarios e continuidade.",
  "Plano de voz, trilha, legenda e ritmo de edicao.",
  "Fila de renderizacao, revisao, custos e artefatos auditaveis.",
];

export default function AudioVideoStudioPage() {
  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title="Estudio de Audio e Video"
        subtitle="Area nova para construir producoes audiovisuais sofisticadas sem deslocar os fluxos atuais de videos rapidos, criativos, PDEs e organicos."
      />

      <section className="audio-video-studio-page__intro">
        <div>
          <p className="audio-video-studio-page__eyebrow">Construcao gradual</p>
          <h2>Videos com narrativa, som, cenas e acabamento de maior valor.</h2>
          <p>
            Este cockpit inicia a separacao operacional entre videos de rotina
            e producoes sofisticadas. O foco do estudio e criar pecas que
            aumentem desejo, confianca e conversao para produtos digitais.
          </p>
        </div>
        <div className="audio-video-studio-page__status" aria-label="Status do modulo">
          <span>Fase inicial</span>
          <strong>Planejamento ativo</strong>
        </div>
      </section>

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <h2>Escopo do estudio</h2>
          <p>
            A primeira versao organiza o tipo de producao que sera evoluido no
            modulo antes de automatizar cadastros, jobs e revisoes.
          </p>
        </div>
        <div className="audio-video-studio-page__grid">
          {productionPillars.map((pillar) => (
            <article className="audio-video-studio-page__pillar" key={pillar.title}>
              <pillar.icon size={22} aria-hidden="true" />
              <h3>{pillar.title}</h3>
              <p>{pillar.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="audio-video-studio-page__columns">
        <div className="audio-video-studio-page__panel">
          <h2>O que continua onde esta</h2>
          <ul>
            {currentFlows.map((flow) => (
              <li key={flow}>{flow}</li>
            ))}
          </ul>
        </div>
        <div className="audio-video-studio-page__panel">
          <h2>Proximas etapas de construcao</h2>
          <ol>
            {buildSteps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </div>
      </section>
    </div>
  );
}
