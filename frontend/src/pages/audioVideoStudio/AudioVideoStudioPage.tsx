import {
  BadgeCheck,
  Clapperboard,
  FileText,
  Music,
  PlayCircle,
  Save,
  Scissors,
  Sparkles,
  Timer,
  Volume2,
} from "lucide-react";
import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  useCreateVideoProject,
  useUpdateVideoProject,
  useVideoProject,
  useVideoProjects,
} from "../../api/salesVideo/useVideoProjects";
import type { VideoProject, VideoProjectPayload } from "../../api/salesVideo/types";
import PageTitle from "../../components/PageTitle";
import "./AudioVideoStudioPage.css";

type StudioBriefing = {
  title: string;
  story: string;
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

const exampleStory =
  "Uma consultora independente sente que sua presenca digital nao mostra sua autoridade real. Ela tenta postar melhor, ajustar foto, escrever bio e criar conteudo, mas tudo parece solto. Ao entrar no Metodo MUSA, ela recebe um diagnostico guiado por IA que transforma sinais dispersos em uma direcao clara de imagem, conteudo e posicionamento. Em poucos dias, ela entende o que precisa ajustar, passa a se apresentar com mais seguranca e convida outras pessoas para fazerem o mesmo diagnostico.";

const defaultBriefing: StudioBriefing = {
  title: "MUSA - video manifesto de presenca digital",
  story: exampleStory,
  product: "Metodo MUSA",
  audience: "Mulheres que vendem sua imagem, conhecimento ou atendimento",
  pain: "Esta se esforcando para aparecer melhor, mas sua presenca digital nao traduz autoridade",
  promise: "Sair da sensacao de improviso e enxergar os proximos ajustes de imagem com clareza",
  mechanism: "Diagnostico de presenca publica guiado por IA",
  proof: "Antes e depois da clareza de posicionamento, bio, imagem e direcao de conteudo",
  cta: "Fazer o diagnostico MUSA",
};

function buildBriefingFromProject(project: VideoProject): StudioBriefing {
  return {
    title: project.title,
    story: project.storyText || project.objective,
    product: project.contextType || defaultBriefing.product,
    audience: project.targetChannel || defaultBriefing.audience,
    pain: project.hookText || project.objective,
    promise: project.primaryMetric || project.objective,
    mechanism: project.productionMode || defaultBriefing.mechanism,
    proof: project.visualReferences || defaultBriefing.proof,
    cta: project.ctaText || defaultBriefing.cta,
  };
}

export default function AudioVideoStudioPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const parsedProjectId = projectId ? Number(projectId) : undefined;
  const editableProjectId =
    parsedProjectId && Number.isFinite(parsedProjectId)
      ? parsedProjectId
      : undefined;
  const selectedProjectQuery = useVideoProject(editableProjectId);
  const selectedProject = selectedProjectQuery.data;
  const videoProjectsQuery = useVideoProjects();
  const createVideoProject = useCreateVideoProject();
  const updateVideoProject = useUpdateVideoProject();
  const [saveFeedback, setSaveFeedback] = useState("");
  const [briefing, setBriefing] = useState<StudioBriefing>(defaultBriefing);

  const isEditingProject = Boolean(editableProjectId);
  const isSavingProject =
    createVideoProject.isPending || updateVideoProject.isPending;

  useEffect(() => {
    if (selectedProject) {
      setBriefing(buildBriefingFromProject(selectedProject));
      setSaveFeedback("");
    }
  }, [selectedProject]);

  const scriptDraft = useMemo(
    () => [
      `Historia: ${briefing.story}`,
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

  const buildProjectPayload = (): VideoProjectPayload => ({
    productId: selectedProject?.productId,
    experimentId: selectedProject?.experimentId,
    salesVideoProfileId: selectedProject?.salesVideoProfileId,
    campaignKey: selectedProject?.campaignKey ?? undefined,
    contextType: selectedProject?.contextType || "PDE",
    productionMode: selectedProject?.productionMode || "STORY_FIRST_AUDIO_VIDEO",
    targetChannel: selectedProject?.targetChannel || "PDE_AND_SOCIAL",
    format: selectedProject?.format || "VERTICAL_9_16",
    title: briefing.title,
    objective:
      selectedProject?.objective ||
      "Testar uma narrativa audiovisual de 3 minutos para aumentar desejo, confianca e acao no Metodo MUSA.",
    storyText: briefing.story,
    funnelStage: selectedProject?.funnelStage || "AWARENESS",
    primaryMetric: selectedProject?.primaryMetric || "DIAGNOSTIC_START",
    hookText: `${briefing.audience}, se ${briefing.pain.toLowerCase()}, este video mostra um caminho mais simples.`,
    scriptText: scriptDraft.join("\n\n"),
    scenePlan: selectedProject?.scenePlan || scenePrompts.join("\n"),
    visualReferences: briefing.proof,
    voiceoverPlan:
      selectedProject?.voiceoverPlan ||
      "Voz proxima, confiante e acolhedora, com ritmo medio e pausas curtas para reforcar pontos de virada.",
    soundtrackPlan:
      selectedProject?.soundtrackPlan ||
      "Trilha leve, moderna e aspiracional, sempre abaixo da narracao.",
    captionPlan:
      selectedProject?.captionPlan ||
      "Legendas curtas com palavras-chave de dor, mecanismo, prova e CTA.",
    ctaText: briefing.cta,
    targetDurationSeconds: selectedProject?.targetDurationSeconds || 180,
    providerPlan:
      selectedProject?.providerPlan ||
      "Comecar com roteiro e storyboard; depois testar narracao, cenas-chave e montagem em jobs auditaveis.",
    editingNotes:
      selectedProject?.editingNotes ||
      "Priorizar cortes limpos, prova visual concreta e CTA sem excesso de texto.",
    qualityGate:
      selectedProject?.qualityGate ||
      "Aprovar somente se a historia estiver clara, o mecanismo parecer plausivel, o audio for compreensivel e o CTA estiver conectado ao funil.",
    status: selectedProject?.status || "READY_FOR_SCRIPT",
    createdBy: isEditingProject ? undefined : "codex-mkt",
    updatedBy: "codex-mkt",
  });

  const handleSaveProject = async () => {
    setSaveFeedback("");
    try {
      if (editableProjectId) {
        const project = await updateVideoProject.mutateAsync({
          projectId: editableProjectId,
          payload: buildProjectPayload(),
        });
        setSaveFeedback(`Projeto atualizado: #${project.id} - ${project.title}`);
        return;
      }

      const project = await createVideoProject.mutateAsync(buildProjectPayload());
      setSaveFeedback(`Projeto exemplo criado: #${project.id} - ${project.title}`);
    } catch {
      setSaveFeedback(
        "Nao foi possivel salvar o projeto agora. Revise a conexao com o backend e tente novamente.",
      );
    }
  };

  const recentProjects = videoProjectsQuery.data?.slice(0, 4) ?? [];

  return (
    <div className="audio-video-studio-page">
      <PageTitle
        title={
          isEditingProject
            ? "Editor de Audio e Video"
            : "Estudio de Audio e Video"
        }
        subtitle={
          isEditingProject
            ? "Projeto carregado para continuar roteiro, cenas, audio, montagem e revisao comercial."
            : "Todo audio ou video nasce de um projeto. O primeiro passo do projeto e contar uma historia forte o suficiente para vender uma transformacao."
        }
      />

      {isEditingProject ? (
        <Link
          className="audio-video-studio-page__secondary-action audio-video-studio-page__back-link"
          to="/audio-video-studio/projects"
        >
          Voltar para lista de projetos
        </Link>
      ) : null}

      {selectedProjectQuery.isLoading ? (
        <article className="audio-video-studio-page__project-card">
          Carregando projeto selecionado...
        </article>
      ) : null}

      {selectedProjectQuery.isError ? (
        <article className="audio-video-studio-page__project-card">
          Nao foi possivel carregar este projeto.
        </article>
      ) : null}

      <section className="audio-video-studio-page__intro">
        <div>
          <p className="audio-video-studio-page__eyebrow">
            Experimento 3 minutos
          </p>
          <h2>Projeto primeiro, historia primeiro, producao depois.</h2>
          <p>
            O Estudio organiza a producao audiovisual como um ativo comercial:
            historia, roteiro, cenas, voz, trilha, montagem e revisao antes de
            qualquer renderizacao.
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
            <h2>{isEditingProject ? "Projeto carregado" : "Projeto de exemplo"}</h2>
            <p>
              {isEditingProject
                ? "Continue o trabalho a partir dos dados persistidos neste projeto."
                : "Ajuste a historia base e crie um projeto persistido para testar o Estudio."}
            </p>
          </div>
          <label>
            Titulo do projeto
            <input value={briefing.title} onChange={updateBriefing("title")} />
          </label>
          <label>
            Historia inicial
            <textarea
              value={briefing.story}
              onChange={updateBriefing("story")}
              rows={7}
            />
          </label>
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
          <button
            className="audio-video-studio-page__primary-action"
            type="button"
            onClick={handleSaveProject}
            disabled={isSavingProject || selectedProjectQuery.isLoading}
          >
            <Save size={18} aria-hidden="true" />
            {isSavingProject
              ? "Salvando projeto..."
              : isEditingProject
                ? "Salvar continuidade"
                : "Criar projeto exemplo"}
          </button>
          {saveFeedback ? (
            <p className="audio-video-studio-page__feedback">{saveFeedback}</p>
          ) : null}
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
          <h2>Projetos recentes do estudio</h2>
          <p>
            Lista operacional para confirmar se o projeto exemplo foi gravado e
            seguir os proximos testes.
          </p>
        </div>
        <div className="audio-video-studio-page__project-list">
          {videoProjectsQuery.isLoading ? (
            <article className="audio-video-studio-page__project-card">
              Carregando projetos...
            </article>
          ) : recentProjects.length > 0 ? (
            recentProjects.map((project) => (
              <article
                className="audio-video-studio-page__project-card"
                key={project.id}
              >
                <span>#{project.id}</span>
                <h3>{project.title}</h3>
                <p>{project.storyText || project.objective}</p>
                <small>{project.status}</small>
              </article>
            ))
          ) : (
            <article className="audio-video-studio-page__project-card">
              Nenhum projeto criado ainda. Use o exemplo MUSA para iniciar os
              testes.
            </article>
          )}
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
          apos criar o projeto exemplo, evoluir jobs auditaveis de roteiro, voz,
          cenas, montagem e revisao.
        </span>
      </div>
    </div>
  );
}
