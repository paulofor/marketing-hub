import {
  BadgeCheck,
  Clapperboard,
  FileText,
  Music,
  PlayCircle,
  Scissors,
  Sparkles,
  Timer,
  Volume2,
} from "lucide-react";
import { useMemo, useState, type ChangeEvent } from "react";
import PageTitle from "../../components/PageTitle";
import "./AudioVideoStudioPage.css";

type StudioBriefing = {
  product: string;
  audience: string;
  pain: string;
  promise: string;
  mechanism: string;
  proof: string;
  cta: string;
};

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

const scriptBlocks = [
  {
    time: "0:00-0:15",
    title: "Gancho",
    objective: "Quebrar rolagem com dor clara e promessa especifica.",
  },
  {
    time: "0:15-0:45",
    title: "Dor e custo oculto",
    objective: "Mostrar o prejuizo pratico de continuar sem resolver.",
  },
  {
    time: "0:45-1:20",
    title: "Mecanismo",
    objective:
      "Apresentar a nova forma de obter o resultado com menos esforco.",
  },
  {
    time: "1:20-2:05",
    title: "Demonstracao",
    objective: "Exibir processo, exemplo, tela, antes/depois ou prova visual.",
  },
  {
    time: "2:05-2:35",
    title: "Oferta",
    objective: "Conectar promessa, entregaveis, bonus e reducao de risco.",
  },
  {
    time: "2:35-3:00",
    title: "CTA",
    objective: "Dar uma acao simples e direta para o proximo passo do funil.",
  },
];

const productionChecklist = [
  "Briefing comercial preenchido",
  "Roteiro narrado com ate 390 palavras",
  "6 blocos de cena com funcao clara",
  "Voz definida com ritmo, pausas e emocao",
  "Trilha escolhida sem competir com a narracao",
  "Legenda planejada para consumo sem audio",
  "CTA final conectado ao funil de venda",
];

const scenePrompts = [
  "Cena de abertura com rosto, movimento ou demonstracao visual imediata.",
  "Cena de contraste mostrando a dor antes da solucao.",
  "Cena do mecanismo com objeto, tela ou metafora visual simples.",
  "Cena de prova com resultado, depoimento, dado ou transformacao.",
  "Cena da oferta com entregaveis e ganho percebido.",
  "Cena final com CTA, URL, produto ou proximo passo.",
];

export default function AudioVideoStudioPage() {
  const [briefing, setBriefing] = useState<StudioBriefing>({
    product: "Produto digital com IA aplicada",
    audience: "Pessoa com dor urgente e pouco tempo",
    pain: "Esta perdendo tempo tentando resolver manualmente",
    promise: "Conseguir um resultado pratico em poucos dias",
    mechanism: "Metodo guiado por IA com passos simples",
    proof: "Exemplo visual do antes e depois",
    cta: "Entrar na lista de interesse",
  });

  const scriptDraft = useMemo(
    () => [
      `Gancho: ${briefing.audience}, se ${briefing.pain.toLowerCase()}, este video mostra um caminho mais simples.`,
      `Promessa: com ${briefing.product}, a proposta e ${briefing.promise.toLowerCase()}.`,
      `Mecanismo: a solucao usa ${briefing.mechanism.toLowerCase()}, reduzindo esforco e aumentando clareza.`,
      `Prova: use ${briefing.proof.toLowerCase()} para tornar o ganho visivel antes da oferta.`,
      `CTA: ${briefing.cta}.`,
    ],
    [briefing],
  );

  const updateBriefing =
    (field: keyof StudioBriefing) =>
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setBriefing((current) => ({ ...current, [field]: event.target.value }));
    };

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title="Estudio de Audio e Video"
        subtitle="Cockpit inicial para experimentar videos curtos de 3 minutos com roteiro, cenas, audio, montagem e revisao comercial."
      />

      <section className="audio-video-studio-page__intro">
        <div>
          <p className="audio-video-studio-page__eyebrow">
            Experimento 3 minutos
          </p>
          <h2>Videos com narrativa, som, cenas e acabamento de maior valor.</h2>
          <p>
            Este cockpit inicia a separacao operacional entre videos de rotina e
            producoes sofisticadas. O foco do estudio e criar pecas que aumentem
            desejo, confianca e conversao para produtos digitais.
          </p>
        </div>
        <div
          className="audio-video-studio-page__status"
          aria-label="Status do modulo"
        >
          <Timer size={22} aria-hidden="true" />
          <span>Formato base</span>
          <strong>Video curto de 3 minutos</strong>
          <small>
            180 segundos para gancho, mecanismo, prova, oferta e CTA.
          </small>
        </div>
      </section>

      <section className="audio-video-studio-page__workspace">
        <form
          className="audio-video-studio-page__briefing"
          aria-label="Briefing do video de 3 minutos"
        >
          <div className="audio-video-studio-page__section-heading">
            <h2>Briefing rapido</h2>
            <p>
              Preencha a base comercial antes de gerar roteiro, cenas e audio.
            </p>
          </div>
          <label>
            Produto
            <input
              value={briefing.product}
              onChange={updateBriefing("product")}
            />
          </label>
          <label>
            Publico
            <input
              value={briefing.audience}
              onChange={updateBriefing("audience")}
            />
          </label>
          <label>
            Dor principal
            <textarea
              value={briefing.pain}
              onChange={updateBriefing("pain")}
              rows={2}
            />
          </label>
          <label>
            Promessa
            <textarea
              value={briefing.promise}
              onChange={updateBriefing("promise")}
              rows={2}
            />
          </label>
          <label>
            Mecanismo
            <input
              value={briefing.mechanism}
              onChange={updateBriefing("mechanism")}
            />
          </label>
          <label>
            Prova visual
            <input value={briefing.proof} onChange={updateBriefing("proof")} />
          </label>
          <label>
            CTA
            <input value={briefing.cta} onChange={updateBriefing("cta")} />
          </label>
        </form>

        <div className="audio-video-studio-page__draft">
          <div className="audio-video-studio-page__section-heading">
            <h2>Rascunho narrativo</h2>
            <p>
              Base pronta para transformar em roteiro falado e plano de cenas.
            </p>
          </div>
          <ol>
            {scriptDraft.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ol>
          <div className="audio-video-studio-page__audio-card">
            <Volume2 size={20} aria-hidden="true" />
            <div>
              <strong>Direcao de audio</strong>
              <span>
                Voz proxima, ritmo medio, pausas curtas e trilha baixa para
                manter clareza.
              </span>
            </div>
          </div>
        </div>
      </section>

      <section className="audio-video-studio-page__section">
        <div className="audio-video-studio-page__section-heading">
          <h2>Estrutura de 3 minutos</h2>
          <p>
            Sequencia minima para testar retencao, desejo e acao sem depender de
            improviso.
          </p>
        </div>
        <div className="audio-video-studio-page__timeline">
          {scriptBlocks.map((block) => (
            <article
              className="audio-video-studio-page__timeline-block"
              key={block.time}
            >
              <span>{block.time}</span>
              <h3>{block.title}</h3>
              <p>{block.objective}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="audio-video-studio-page__columns">
        <div className="audio-video-studio-page__panel">
          <h2>Plano basico de cenas</h2>
          <ul>
            {scenePrompts.map((prompt) => (
              <li key={prompt}>{prompt}</li>
            ))}
          </ul>
        </div>
        <div className="audio-video-studio-page__panel">
          <h2>Checklist de producao</h2>
          <ul className="audio-video-studio-page__checklist">
            {productionChecklist.map((item) => (
              <li key={item}>
                <BadgeCheck size={16} aria-hidden="true" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
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
            <article
              className="audio-video-studio-page__pillar"
              key={pillar.title}
            >
              <pillar.icon size={22} aria-hidden="true" />
              <h3>{pillar.title}</h3>
              <p>{pillar.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="audio-video-studio-page__columns">
        <div className="audio-video-studio-page__panel">
          <h2>Experimentos recomendados</h2>
          <ol>
            <li>Video educativo com promessa forte para publico frio.</li>
            <li>Video demonstrativo com prova visual para leads mornos.</li>
            <li>Video de oferta com urgencia leve para remarketing.</li>
          </ol>
        </div>
        <div className="audio-video-studio-page__panel">
          <h2>Recursos atuais conectaveis</h2>
          <ul>
            <li>Gerador de imagem para referencias visuais e cenas-chave.</li>
            <li>
              Fluxos de videos de produto para assets curtos ja existentes.
            </li>
            <li>Revisao comercial antes de usar em campanha ou PDE.</li>
          </ul>
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

      <div className="audio-video-studio-page__next-action">
        <PlayCircle size={22} aria-hidden="true" />
        <strong>Proximo incremento:</strong>
        <span>
          persistir projetos do estudio e criar jobs auditaveis de roteiro, voz,
          cenas e montagem.
        </span>
      </div>
    </div>
  );
}
