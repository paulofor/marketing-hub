import {
  BadgeCheck,
  Clapperboard,
  FileText,
  ListChecks,
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
import { useSalesVideoJobs } from "../../api/salesVideo/useSalesVideoJobs";
import { useAsset } from "../../api/media/useAsset";
import type {
  VideoProject,
  VideoProjectPayload,
  VideoProjectStatus,
} from "../../api/salesVideo/types";
import PageTitle from "../../components/PageTitle";
import { getStudioCommercialLabel } from "./audioVideoStudioLabels";
import "./AudioVideoStudioPage.css";

type StudioBriefing = {
  productId: string;
  campaignKey: string;
  videoCategory: string;
  contextType: string;
  productionMode: string;
  targetChannel: string;
  format: string;
  title: string;
  story: string;
  product: string;
  audience: string;
  pain: string;
  promise: string;
  mechanism: string;
  proof: string;
  cta: string;
  characterBible: string;
  environmentBible: string;
  objectBible: string;
  visualStyleGuide: string;
  imageGenerationPlan: string;
  continuityRules: string;
  targetDurationSeconds: string;
  funnelStage: string;
  primaryMetric: string;
  providerPlan: string;
  voiceoverPlan: string;
  soundtrackPlan: string;
  captionPlan: string;
  editingNotes: string;
  qualityGate: string;
  status: VideoProjectStatus;
};

type StudioCategoryOption = {
  value: string;
  label: string;
  durationRule: string;
  commercialUse: string;
};

type StudioSelectOption = {
  value: string;
  label: string;
  description: string;
};

type StudioPreset = {
  key: string;
  label: string;
  badge: string;
  description: string;
  briefing: StudioBriefing;
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

const videoCategoryOptions: StudioCategoryOption[] = [
  {
    value: "COMMERCIAL_SHORT",
    label: "Video comercial curto",
    durationRule: "6 a 60 segundos",
    commercialUse:
      "Hero, anuncio, Reels, Stories, retargeting e clique para diagnostico.",
  },
  {
    value: "LONG_FORM",
    label: "Video longo / VSL",
    durationRule: "180 segundos ou mais",
    commercialUse:
      "Paywall, pagina de venda, objecoes, prova e explicacao da oferta.",
  },
  {
    value: "INSTITUTIONAL_CONTENT",
    label: "Institucional / conteudo",
    durationRule: "duracao livre",
    commercialUse: "Conteudo de marca, autoridade, onboarding ou nutricao.",
  },
];

const campaignOptions: StudioSelectOption[] = [
  {
    value: "musa-pde-entry-v7-espelho-antes-de-sair",
    label: "MUSA v7 - vídeo leve dos 7 dias",
    description:
      "Hero curto para explicar a jornada MUSA pelo espelho, diagnóstico e plano de 7 dias.",
  },
  {
    value: "musa-video-manifesto-presenca-digital",
    label: "MUSA manifesto - presença digital",
    description:
      "Vídeo mais longo para história, mecanismo, prova, oferta e CTA.",
  },
];

const targetChannelOptions: StudioSelectOption[] = [
  {
    value: "PDE_HERO_DIAGNOSTIC",
    label: "Hero do PDE para diagnóstico",
    description:
      "Primeira dobra da experiência, levando a mulher para o diagnóstico gratuito.",
  },
  {
    value: "PDE_AND_SOCIAL",
    label: "PDE e redes sociais",
    description:
      "Peça reutilizável em página, Reels, TikTok, Shorts e remarketing.",
  },
  {
    value: "SOCIAL_REELS_STORIES",
    label: "Reels, Stories e TikTok",
    description: "Criativo vertical de atenção rápida para tráfego frio.",
  },
  {
    value: "PAYWALL_OFFER",
    label: "Oferta antes do checkout",
    description:
      "Vídeo para explicar valor, reduzir objeções e empurrar para compra.",
  },
];

const funnelStageOptions: StudioSelectOption[] = [
  {
    value: "AWARENESS_TO_DIAGNOSTIC",
    label: "Anúncio para diagnóstico",
    description:
      "Atrai público frio e conduz para o primeiro passo gratuito do funil.",
  },
  {
    value: "AWARENESS",
    label: "Descoberta da dor",
    description: "Gera identificação inicial com a dor e o desejo da cliente.",
  },
  {
    value: "DIAGNOSTIC_TO_PAYWALL",
    label: "Diagnóstico para oferta",
    description:
      "Conecta valor percebido no diagnóstico ao acesso completo do produto.",
  },
  {
    value: "RETARGETING_PURCHASE",
    label: "Remarketing para compra",
    description:
      "Retoma quem já demonstrou interesse e reforça motivo para avançar.",
  },
];

const primaryMetricOptions: StudioSelectOption[] = [
  {
    value:
      "CTA_CLICK_TO_DIAGNOSTIC; apoio: VIDEO_PLAY, VIDEO_75, DIAGNOSTIC_COMPLETED, PAYWALL_VIEWED, CHECKOUT_STARTED, PURCHASE",
    label: "Clique no diagnóstico",
    description:
      "Métrica principal para vídeos de entrada; acompanha retenção, paywall, checkout e compra.",
  },
  {
    value: "DIAGNOSTIC_START",
    label: "Início do diagnóstico",
    description:
      "Boa métrica quando o criativo está mais próximo da experiência gratuita.",
  },
  {
    value: "PAYWALL_VIEWED",
    label: "Visualização da oferta",
    description:
      "Mede se o vídeo aumenta chegada na etapa em que a compra é apresentada.",
  },
  {
    value: "PURCHASE",
    label: "Compra",
    description:
      "Use quando o vídeo estiver diretamente ligado à página de venda ou checkout.",
  },
];

const productionModeOptions: StudioSelectOption[] = [
  {
    value: "CINEMATIC_SCENE_BLUEPRINT",
    label: "Vídeo narrativo com cenas",
    description:
      "Cenas cinematográficas planejadas antes de renderizar em Luma, Kling ou similar.",
  },
  {
    value: "STORY_FIRST_AUDIO_VIDEO",
    label: "Roteiro com voz e montagem",
    description:
      "Começa pela história, depois organiza narração, trilha, cenas e edição.",
  },
  {
    value: "AVATAR_EXPLAINER",
    label: "Apresentadora explicando",
    description:
      "Formato com avatar/apresentadora quando a clareza da explicação for prioridade.",
  },
];

const formatOptions: StudioSelectOption[] = [
  {
    value: "VERTICAL_9_16",
    label: "Vertical para Reels/TikTok/Shorts",
    description:
      "Formato principal para mobile, hero vertical e criativos sociais.",
  },
  {
    value: "SQUARE_1_1",
    label: "Quadrado para feed",
    description: "Peça de feed e variações de mídia social.",
  },
  {
    value: "HORIZONTAL_16_9",
    label: "Horizontal para página ou YouTube",
    description: "Vídeo amplo para VSL, YouTube ou apresentação em desktop.",
  },
];

const statusOptions: { value: VideoProjectStatus; label: string }[] = [
  { value: "DRAFT", label: "Rascunho" },
  { value: "READY_FOR_SCRIPT", label: "Pronto para roteiro" },
  { value: "READY_FOR_RENDER", label: "Pronto para render" },
  { value: "IN_PRODUCTION", label: "Em producao" },
  { value: "READY_FOR_REVIEW", label: "Pronto para revisao" },
  { value: "APPROVED", label: "Aprovado" },
  { value: "ARCHIVED", label: "Arquivado" },
];

function getOptionLabel(options: StudioSelectOption[], value?: string | null) {
  return (
    options.find((option) => option.value === value)?.label ??
    getStudioCommercialLabel(value)
  );
}

function getOptionDescription(
  options: StudioSelectOption[],
  value?: string | null,
) {
  return options.find((option) => option.value === value)?.description ?? "";
}

function optionsWithCurrent(
  options: StudioSelectOption[],
  currentValue: string,
) {
  if (
    !currentValue ||
    options.some((option) => option.value === currentValue)
  ) {
    return options;
  }
  return [
    {
      value: currentValue,
      label: getStudioCommercialLabel(currentValue),
      description:
        "Valor já salvo no projeto. Troque por uma opção da lista quando quiser padronizar.",
    },
    ...options,
  ];
}

const studioWorkflowSteps = [
  {
    title: "Blueprint",
    description: "Produto, funil, promessa, categoria, duracao e metrica.",
  },
  {
    title: "Pre-producao",
    description: "Personagem, ambiente, objetos, frames-chave e continuidade.",
  },
  {
    title: "Producao",
    description: "Provider, cenas, voz, trilha, legendas e montagem.",
  },
  {
    title: "Aprovacao",
    description: "Gate comercial, HLS/fallback e liberacao para uso no funil.",
  },
  {
    title: "Aprendizado",
    description:
      "Play, retencao, CTA, diagnostico, paywall, checkout e compra.",
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

const longFormScriptBlocks = [
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

const heroScriptBlocks = [
  {
    time: "0:00-0:08",
    title: "Dor do espelho",
    objective:
      "Gerar identificacao imediata com a sensacao de faltar presenca.",
  },
  {
    time: "0:08-0:16",
    title: "Resultado possivel",
    objective:
      "Mostrar elegancia acessivel, sem luxo e sem transformacao exagerada.",
  },
  {
    time: "0:16-0:26",
    title: "Mecanismo MUSA",
    objective:
      "Tangibilizar ruido visual, peca-sinal, cor, acabamento e postura.",
  },
  {
    time: "0:26-0:34",
    title: "Diagnostico",
    objective: "Levar ao clique no plano MUSA de 7 dias.",
  },
];

const productionChecklist = [
  "Briefing comercial preenchido",
  "Personagens com imagens mestre aprovadas",
  "Ambientes com placas visuais e mapa de continuidade",
  "Objetos/produto com referencias separadas",
  "Roteiro narrado compatível com a duracao escolhida",
  "Blocos de cena com funcao clara",
  "Voz definida com ritmo, pausas e emocao",
  "Trilha escolhida sem competir com a narracao",
  "Legenda planejada para consumo sem audio",
  "CTA final conectado ao funil de venda",
];

const defaultScenePrompts = [
  "Cena de abertura com rosto, movimento ou demonstracao visual imediata.",
  "Cena de contraste mostrando a dor antes da solucao.",
  "Cena do mecanismo com objeto, tela ou metafora visual simples.",
  "Cena de prova com resultado, depoimento, dado ou transformacao.",
  "Cena da oferta com entregaveis e ganho percebido.",
  "Cena final com CTA, URL, produto ou proximo passo.",
];

const musaV7ScenePrompts = [
  "Cena 1 (6-8s): mulher urbana brasileira diante do espelho, pronta para sair, ajustando manga, cabelo ou acessorio, com duvida discreta e luz natural suave.",
  "Cena 2 (6-8s): a mesma mulher caminha em ambiente urbano claro, com roupa simples, melhor acabamento e postura mais segura, comunicando intencao sem ostentacao.",
  "Cena 3 (8-10s): cortes proximos de maos retirando excesso visual, escolhendo peca-sinal, comparando cor, ajustando acabamento e alinhando postura no espelho.",
  "Cena 4 (6-8s): mulher segura o celular, inicia o diagnostico sem mostrar UI legivel e termina olhando no espelho com sorriso discreto e postura mais segura.",
];

const exampleStory =
  "Uma consultora independente sente que sua presenca digital nao mostra sua autoridade real. Ela tenta postar melhor, ajustar foto, escrever bio e criar conteudo, mas tudo parece solto. Ao entrar no Metodo MUSA, ela recebe um diagnostico guiado por IA que transforma sinais dispersos em uma direcao clara de imagem, conteudo e posicionamento. Em poucos dias, ela entende o que precisa ajustar, passa a se apresentar com mais seguranca e convida outras pessoas para fazerem o mesmo diagnostico.";

const defaultBriefing: StudioBriefing = {
  productId: "",
  campaignKey: "musa-video-manifesto-presenca-digital",
  videoCategory: "LONG_FORM",
  contextType: "PDE",
  productionMode: "STORY_FIRST_AUDIO_VIDEO",
  targetChannel: "PDE_AND_SOCIAL",
  format: "VERTICAL_9_16",
  title: "MUSA - video manifesto de presenca digital",
  story: exampleStory,
  product: "Metodo MUSA",
  audience: "Mulheres que vendem sua imagem, conhecimento ou atendimento",
  pain: "Esta se esforcando para aparecer melhor, mas sua presenca digital nao traduz autoridade",
  promise:
    "Sair da sensacao de improviso e enxergar os proximos ajustes de imagem com clareza",
  mechanism: "Diagnostico de presenca publica guiado por IA",
  proof:
    "Antes e depois da clareza de posicionamento, bio, imagem e direcao de conteudo",
  cta: "Fazer o diagnostico MUSA",
  characterBible:
    "Personagem principal: mulher consultora, 35-45 anos, rosto frontal, tres quartos, perfil, corpo inteiro, figurino principal, acessorios e URLs/IDs das imagens aprovadas.",
  environmentBible:
    "Ambiente principal: escritorio claro e elegante, plano geral, angulo oposto, lateral, detalhes da mesa, entradas/saidas e URL/ID da imagem mestra.",
  objectBible:
    "Produto e objetos: tela do diagnostico MUSA, celular, notebook, elementos de marca e qualquer texto/logotipo como arquivo separado para composicao.",
  visualStyleGuide:
    "Realista premium, luz suave, pele natural, fundo limpo, composicao vertical 9:16, contraste moderado e paleta elegante sem excesso de efeitos.",
  imageGenerationPlan:
    "Solicitar ao modelo de imagem OpenAI primeiro as imagens mestre de personagem, ambiente, produto e frames-chave; aprovar antes de pedir video.",
  continuityRules:
    "Manter rosto, cabelo, figurino, acessorios, escala, temperatura de cor, posicao de objetos fixos e arquitetura do ambiente em todas as cenas.",
  targetDurationSeconds: "180",
  funnelStage: "AWARENESS",
  primaryMetric: "DIAGNOSTIC_START",
  providerPlan:
    "Comecar com roteiro e storyboard; depois testar narracao, cenas-chave e montagem em jobs auditaveis.",
  voiceoverPlan:
    "Voz proxima, confiante e acolhedora, com ritmo medio e pausas curtas para reforcar pontos de virada.",
  soundtrackPlan:
    "Trilha leve, moderna e aspiracional, sempre abaixo da narracao.",
  captionPlan:
    "Legendas curtas com palavras-chave de dor, mecanismo, prova e CTA.",
  editingNotes:
    "Priorizar cortes limpos, prova visual concreta e CTA sem excesso de texto.",
  qualityGate:
    "Aprovar somente se a historia estiver clara, o mecanismo parecer plausivel, o audio for compreensivel e o CTA estiver conectado ao funil.",
  status: "READY_FOR_SCRIPT",
};

const musaV7Briefing: StudioBriefing = {
  productId: "4",
  campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
  videoCategory: "COMMERCIAL_SHORT",
  contextType: "PDE",
  productionMode: "CINEMATIC_SCENE_BLUEPRINT",
  targetChannel: "PDE_HERO_DIAGNOSTIC",
  format: "VERTICAL_9_16",
  title: "MUSA v7 - O espelho antes de sair",
  story:
    "Antes de sair, uma mulher urbana se olha no espelho e percebe que nao precisa comprar uma vida nova. Ela precisa ajustar pequenos sinais da presenca que ja quer comunicar: limpar ruido visual, escolher uma peca-sinal, alinhar cor, acabamento e postura. O video conduz essa passagem de duvida discreta para clareza elegante e termina no diagnostico gratuito do Plano MUSA de 7 dias.",
  product: "Metodo MUSA - Presenca Elegante em 7 Dias",
  audience:
    "Mulheres urbanas que querem presenca mais elegante, marcante e segura com escolhas acessiveis e sem esforco excessivo",
  pain: "A cliente se arruma, olha no espelho e sente que sua imagem ainda fica comum, apagada ou com ruido visual.",
  promise:
    "Em 7 dias, pequenos ajustes podem deixar a imagem mais intencional, elegante e coerente usando o que ela ja tem.",
  mechanism:
    "Arquitetura de Presenca Elegante Acessivel: ruido visual, peca-sinal, cor, acabamento, postura e repeticao diaria.",
  proof:
    "Mostrar microacoes reais: retirar excesso visual, escolher um acessorio discreto, comparar paleta, ajustar acabamento e iniciar o diagnostico.",
  cta: "Ver meu plano MUSA de 7 dias",
  characterBible:
    "Sofia MUSA ou mulher urbana brasileira adulta, elegante sem ostentacao, roupa simples com acabamento bonito, expressao natural, postura discreta e confiante. Usar imagem-semente aprovada do produto quando disponivel; preservar cabelo, faixa de idade, figurino e energia visual entre as cenas.",
  environmentBible:
    "Quarto claro com espelho, luz natural suave e detalhes editoriais acessiveis; rua urbana elegante e realista; ambiente final claro com celular em maos sem texto legivel de interface.",
  objectBible:
    "Espelho, celular com diagnostico sem textos legiveis, acessorio discreto como peca-sinal, tecido, paleta creme/vinho, bolsa ou colar simples. Nao usar sacolas de luxo, marcas ou UI deformada.",
  visualStyleGuide:
    "Editorial realista, intimo e premium acessivel. Paleta vinho MUSA, creme editorial, blush quente, dourado discreto, grafite suave e oliva. Sem estetica de slide, palestra, banco de imagem ou luxo inacessivel.",
  imageGenerationPlan:
    "Gerar ou selecionar primeiro imagem mestre da personagem e frames-chave de espelho, caminhada, detalhes de mecanismo e gesto no celular. Aprovar antes de pedir cenas Luma/Kling.",
  continuityRules:
    "Manter a mesma personagem, cabelo, roupa base, temperatura de luz, estilo de ambiente e nivel de elegancia. O video deve funcionar sem audio, com legendas adicionadas na montagem final.",
  targetDurationSeconds: "30",
  funnelStage: "AWARENESS_TO_DIAGNOSTIC",
  primaryMetric:
    "CTA_CLICK_TO_DIAGNOSTIC; apoio: VIDEO_PLAY, VIDEO_75, DIAGNOSTIC_COMPLETED, PAYWALL_VIEWED, CHECKOUT_STARTED, PURCHASE",
  providerPlan:
    "Luma Ray como principal para cenas editoriais e movimento; Kling como alternativa de realismo/custo; HeyGen apenas se a decisao mudar para apresentadora/avatar.",
  voiceoverPlan:
    "Opcional. Se usar voz, pt-BR feminina, intima, baixa, segura e sem entusiasmo artificial. A peca precisa vender mesmo com audio desligado.",
  soundtrackPlan:
    "Trilha feminina, leve, editorial, crescente e sofisticada, sempre discreta para nao parecer propaganda agressiva.",
  captionPlan:
    "Legendas obrigatorias adicionadas fora do modelo: 'Voce se arruma... mas sente que ainda falta presenca?', 'Em 7 dias, pequenos ajustes deixam sua imagem mais intencional.', 'Ruido visual. Peca-sinal. Cor. Acabamento. Postura.', 'Faça o diagnostico gratuito e veja seu Plano MUSA de 7 dias.'",
  editingNotes:
    "Montagem 28-34s: dor do espelho, resultado acessivel, mecanismo em cortes sensoriais e CTA no diagnostico. Criar tambem cortes de 15s e 6-8s para Reels/Stories e retargeting.",
  qualityGate:
    "Aprovar somente se completar Dor -> Resultado -> Mecanismo -> CTA, preservar promessa sem garantia absoluta, nao parecer luxo inacessivel, funcionar em mobile sem audio, ter HLS/fallback e revisao humana antes de entrar em heroVideos da v7.",
  status: "READY_FOR_SCRIPT",
};

const studioPresets: StudioPreset[] = [
  {
    key: "musa-v7",
    label: "MUSA v7 hero cinematografico",
    badge: "Caso real",
    description:
      "Blueprint comercial do video hero da Semana dos 7 Sinais de Presenca.",
    briefing: musaV7Briefing,
  },
  {
    key: "musa-manifesto",
    label: "Manifesto MUSA 3 minutos",
    badge: "Modelo longo",
    description:
      "Estrutura longa para historia, mecanismo, prova, oferta e CTA.",
    briefing: defaultBriefing,
  },
];

function buildBriefingFromProject(project: VideoProject): StudioBriefing {
  return {
    productId: project.productId ? String(project.productId) : "",
    campaignKey: project.campaignKey || "",
    videoCategory: project.videoCategory || defaultBriefing.videoCategory,
    contextType: project.contextType || defaultBriefing.contextType,
    productionMode: project.productionMode || defaultBriefing.productionMode,
    targetChannel: project.targetChannel || defaultBriefing.targetChannel,
    format: project.format || defaultBriefing.format,
    title: project.title,
    story: project.storyText || project.objective,
    product: project.contextType || defaultBriefing.product,
    audience:
      getStudioCommercialLabel(project.targetChannel) ||
      defaultBriefing.audience,
    pain: project.hookText || project.objective,
    promise:
      getStudioCommercialLabel(project.primaryMetric) ||
      project.objective ||
      defaultBriefing.promise,
    mechanism:
      getStudioCommercialLabel(project.productionMode) ||
      defaultBriefing.mechanism,
    proof: project.visualReferences || defaultBriefing.proof,
    cta: project.ctaText || defaultBriefing.cta,
    characterBible: project.characterBible || defaultBriefing.characterBible,
    environmentBible:
      project.environmentBible || defaultBriefing.environmentBible,
    objectBible: project.objectBible || defaultBriefing.objectBible,
    visualStyleGuide:
      project.visualStyleGuide || defaultBriefing.visualStyleGuide,
    imageGenerationPlan:
      project.imageGenerationPlan || defaultBriefing.imageGenerationPlan,
    continuityRules: project.continuityRules || defaultBriefing.continuityRules,
    targetDurationSeconds: project.targetDurationSeconds
      ? String(project.targetDurationSeconds)
      : defaultBriefing.targetDurationSeconds,
    funnelStage: project.funnelStage || defaultBriefing.funnelStage,
    primaryMetric: project.primaryMetric || defaultBriefing.primaryMetric,
    providerPlan: project.providerPlan || defaultBriefing.providerPlan,
    voiceoverPlan: project.voiceoverPlan || defaultBriefing.voiceoverPlan,
    soundtrackPlan: project.soundtrackPlan || defaultBriefing.soundtrackPlan,
    captionPlan: project.captionPlan || defaultBriefing.captionPlan,
    editingNotes: project.editingNotes || defaultBriefing.editingNotes,
    qualityGate: project.qualityGate || defaultBriefing.qualityGate,
    status: project.status || defaultBriefing.status,
  };
}

function parsePositiveInteger(value: string) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function parseOptionalNumber(value: string) {
  if (!value.trim()) {
    return undefined;
  }
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function durationValidationMessage(
  videoCategory: string,
  targetDurationSeconds?: number,
) {
  if (!targetDurationSeconds) {
    return "Informe uma duracao alvo valida para o projeto.";
  }
  if (
    videoCategory === "COMMERCIAL_SHORT" &&
    (targetDurationSeconds < 6 || targetDurationSeconds > 60)
  ) {
    return "Video comercial curto deve ter entre 6 e 60 segundos.";
  }
  if (videoCategory === "LONG_FORM" && targetDurationSeconds < 180) {
    return "Video longo ou VSL deve ter 180 segundos ou mais.";
  }
  return "";
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
  const linkedProfileId = selectedProject?.salesVideoProfileId;
  const linkedJobsQuery = useSalesVideoJobs(linkedProfileId ?? undefined);
  const createVideoProject = useCreateVideoProject();
  const updateVideoProject = useUpdateVideoProject();
  const [saveFeedback, setSaveFeedback] = useState("");
  const [briefing, setBriefing] = useState<StudioBriefing>(defaultBriefing);

  const isEditingProject = Boolean(editableProjectId);
  const isSavingProject =
    createVideoProject.isPending || updateVideoProject.isPending;
  const targetDurationSeconds = parsePositiveInteger(
    briefing.targetDurationSeconds,
  );

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

  const selectedTimeline =
    targetDurationSeconds && targetDurationSeconds <= 60
      ? heroScriptBlocks
      : longFormScriptBlocks;
  const selectedScenePrompts =
    briefing.campaignKey === musaV7Briefing.campaignKey
      ? musaV7ScenePrompts
      : defaultScenePrompts;
  const selectedCategory =
    videoCategoryOptions.find(
      (option) => option.value === briefing.videoCategory,
    ) ?? videoCategoryOptions[1];
  const durationIssue = durationValidationMessage(
    briefing.videoCategory,
    targetDurationSeconds,
  );

  const updateBriefing =
    (field: keyof StudioBriefing) =>
    (
      event: ChangeEvent<
        HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
      >,
    ) => {
      setBriefing((current) => ({ ...current, [field]: event.target.value }));
    };

  const applyPreset = (preset: StudioPreset) => {
    setBriefing(preset.briefing);
    setSaveFeedback("");
  };

  const buildProjectPayload = (): VideoProjectPayload => ({
    productId:
      parseOptionalNumber(briefing.productId) ?? selectedProject?.productId,
    experimentId: selectedProject?.experimentId,
    salesVideoProfileId: selectedProject?.salesVideoProfileId,
    campaignKey:
      briefing.campaignKey || selectedProject?.campaignKey || undefined,
    videoCategory: briefing.videoCategory || "LONG_FORM",
    contextType: briefing.contextType || "PDE",
    productionMode: briefing.productionMode || "STORY_FIRST_AUDIO_VIDEO",
    targetChannel: briefing.targetChannel || "PDE_AND_SOCIAL",
    format: briefing.format || "VERTICAL_9_16",
    title: briefing.title,
    objective:
      selectedProject?.objective ||
      "Testar uma narrativa audiovisual para aumentar desejo, confianca e acao no Metodo MUSA.",
    storyText: briefing.story,
    funnelStage: briefing.funnelStage || "AWARENESS",
    primaryMetric: briefing.primaryMetric || "DIAGNOSTIC_START",
    hookText: `${briefing.audience}, se ${briefing.pain.toLowerCase()}, este video mostra um caminho mais simples.`,
    scriptText: scriptDraft.join("\n\n"),
    scenePlan: selectedScenePrompts.join("\n"),
    visualReferences: briefing.proof,
    characterBible: briefing.characterBible,
    environmentBible: briefing.environmentBible,
    objectBible: briefing.objectBible,
    visualStyleGuide: briefing.visualStyleGuide,
    imageGenerationPlan: briefing.imageGenerationPlan,
    continuityRules: briefing.continuityRules,
    voiceoverPlan: briefing.voiceoverPlan,
    soundtrackPlan: briefing.soundtrackPlan,
    captionPlan: briefing.captionPlan,
    ctaText: briefing.cta,
    targetDurationSeconds: targetDurationSeconds ?? null,
    providerPlan: briefing.providerPlan,
    editingNotes: briefing.editingNotes,
    qualityGate: briefing.qualityGate,
    status: briefing.status || selectedProject?.status || "READY_FOR_SCRIPT",
    createdBy: isEditingProject ? undefined : "codex-mkt",
    updatedBy: "codex-mkt",
  });

  const handleSaveProject = async () => {
    setSaveFeedback("");
    if (durationIssue) {
      setSaveFeedback(durationIssue);
      return;
    }
    try {
      if (editableProjectId) {
        const project = await updateVideoProject.mutateAsync({
          projectId: editableProjectId,
          payload: buildProjectPayload(),
        });
        setSaveFeedback(
          `Projeto atualizado: #${project.id} - ${project.title}`,
        );
        return;
      }

      const project = await createVideoProject.mutateAsync(
        buildProjectPayload(),
      );
      setSaveFeedback(`Projeto criado: #${project.id} - ${project.title}`);
    } catch {
      setSaveFeedback(
        "Nao foi possivel salvar o projeto agora. Revise a conexao com o backend e tente novamente.",
      );
    }
  };

  const recentProjects = videoProjectsQuery.data?.slice(0, 4) ?? [];
  const renderedJob = useMemo(
    () =>
      linkedJobsQuery.data
        ?.filter((job) => job.status === "VIDEO_READY" && job.assetId)
        .sort((first, second) => {
          const firstDate =
            first.finishedAt ?? first.updatedAt ?? first.createdAt ?? "";
          const secondDate =
            second.finishedAt ?? second.updatedAt ?? second.createdAt ?? "";
          return secondDate.localeCompare(firstDate);
        })[0],
    [linkedJobsQuery.data],
  );
  const renderedAssetQuery = useAsset(renderedJob?.assetId);
  const renderedAssetUrl =
    renderedJob?.streamPlaybackUrl?.trim() ||
    renderedAssetQuery.data?.publicUrl ||
    renderedAssetQuery.data?.url ||
    "";

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
            : "Todo video comercial nasce de um blueprint: funil, promessa, cenas, audio, provider, aprovacao e metrica antes da renderizacao."
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
            Blueprint comercial
          </p>
          <h2>Padronize o video antes de gerar cenas.</h2>
          <p>
            O Estudio organiza a producao audiovisual como um ativo comercial:
            funil, promessa, roteiro, cenas, voz, trilha, provider, montagem,
            revisao e metrica antes de qualquer renderizacao.
          </p>
        </div>
        <div
          className="audio-video-studio-page__status"
          aria-label="Status do modulo"
        >
          <Timer size={22} aria-hidden="true" />
          <span>Projeto atual</span>
          <strong>
            {targetDurationSeconds ?? 0}s /{" "}
            {getOptionLabel(formatOptions, briefing.format)}
          </strong>
          <small>
            {selectedCategory.label} ·{" "}
            {getOptionLabel(funnelStageOptions, briefing.funnelStage)}
          </small>
        </div>
      </section>

      <section className="audio-video-studio-page__workflow">
        {studioWorkflowSteps.map((step, index) => (
          <article
            className="audio-video-studio-page__workflow-step"
            key={step.title}
          >
            <span>{index + 1}</span>
            <strong>{step.title}</strong>
            <small>{step.description}</small>
          </article>
        ))}
      </section>

      {!isEditingProject ? (
        <section className="audio-video-studio-page__preset-grid">
          {studioPresets.map((preset) => (
            <button
              className="audio-video-studio-page__preset"
              key={preset.key}
              type="button"
              onClick={() => applyPreset(preset)}
            >
              <span>{preset.badge}</span>
              <strong>{preset.label}</strong>
              <small>{preset.description}</small>
            </button>
          ))}
        </section>
      ) : null}

      <section className="audio-video-studio-page__workspace">
        <form
          className="audio-video-studio-page__briefing"
          aria-label="Blueprint operacional de video comercial"
        >
          <div className="audio-video-studio-page__section-heading">
            <h2>
              {isEditingProject ? "Projeto carregado" : "Projeto de exemplo"}
            </h2>
            <p>
              {isEditingProject
                ? "Continue o trabalho a partir dos dados persistidos neste projeto."
                : "Use o preset MUSA v7 como primeiro caso real ou ajuste o blueprint para outro video comercial."}
            </p>
          </div>
          <div className="audio-video-studio-page__briefing-grid">
            <label>
              ID do produto
              <input
                value={briefing.productId}
                onChange={updateBriefing("productId")}
              />
            </label>
            <label>
              Campanha
              <select
                value={briefing.campaignKey}
                onChange={updateBriefing("campaignKey")}
              >
                {optionsWithCurrent(campaignOptions, briefing.campaignKey).map(
                  (option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ),
                )}
              </select>
              <small>
                {getOptionDescription(campaignOptions, briefing.campaignKey)}
              </small>
            </label>
            <label>
              Categoria do video
              <select
                value={briefing.videoCategory}
                onChange={updateBriefing("videoCategory")}
              >
                {videoCategoryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Canal alvo
              <select
                value={briefing.targetChannel}
                onChange={updateBriefing("targetChannel")}
              >
                {optionsWithCurrent(
                  targetChannelOptions,
                  briefing.targetChannel,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(
                  targetChannelOptions,
                  briefing.targetChannel,
                )}
              </small>
            </label>
            <label>
              Duracao alvo
              <input
                type="number"
                min="1"
                value={briefing.targetDurationSeconds}
                onChange={updateBriefing("targetDurationSeconds")}
              />
            </label>
            <label>
              Status operacional
              <select
                value={briefing.status}
                onChange={updateBriefing("status")}
              >
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="audio-video-studio-page__category-note">
            <ListChecks size={18} aria-hidden="true" />
            <div>
              <strong>{selectedCategory.durationRule}</strong>
              <span>{selectedCategory.commercialUse}</span>
            </div>
          </div>
          {durationIssue ? (
            <p className="audio-video-studio-page__duration-block">
              {durationIssue}
            </p>
          ) : null}
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
          <div className="audio-video-studio-page__briefing-grid">
            <label>
              Etapa do funil
              <select
                value={briefing.funnelStage}
                onChange={updateBriefing("funnelStage")}
              >
                {optionsWithCurrent(
                  funnelStageOptions,
                  briefing.funnelStage,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(funnelStageOptions, briefing.funnelStage)}
              </small>
            </label>
            <label>
              Metrica primaria
              <select
                value={briefing.primaryMetric}
                onChange={updateBriefing("primaryMetric")}
              >
                {optionsWithCurrent(
                  primaryMetricOptions,
                  briefing.primaryMetric,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(
                  primaryMetricOptions,
                  briefing.primaryMetric,
                )}
              </small>
            </label>
            <label>
              Modo de producao
              <select
                value={briefing.productionMode}
                onChange={updateBriefing("productionMode")}
              >
                {optionsWithCurrent(
                  productionModeOptions,
                  briefing.productionMode,
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <small>
                {getOptionDescription(
                  productionModeOptions,
                  briefing.productionMode,
                )}
              </small>
            </label>
            <label>
              Formato
              <select
                value={briefing.format}
                onChange={updateBriefing("format")}
              >
                {optionsWithCurrent(formatOptions, briefing.format).map(
                  (option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ),
                )}
              </select>
              <small>
                {getOptionDescription(formatOptions, briefing.format)}
              </small>
            </label>
          </div>
          <label>
            Prova visual
            <input value={briefing.proof} onChange={updateBriefing("proof")} />
          </label>
          <label>
            CTA
            <input value={briefing.cta} onChange={updateBriefing("cta")} />
          </label>
          <div className="audio-video-studio-page__visual-bible">
            <div className="audio-video-studio-page__section-heading">
              <h2>Biblia visual premium</h2>
              <p>
                Defina referencias mestras antes de gerar cenas para preservar
                consistencia entre takes.
              </p>
            </div>
            <label>
              Personagens e imagens mestre
              <textarea
                value={briefing.characterBible}
                onChange={updateBriefing("characterBible")}
                rows={4}
              />
            </label>
            <label>
              Ambientes e imagens mestre
              <textarea
                value={briefing.environmentBible}
                onChange={updateBriefing("environmentBible")}
                rows={4}
              />
            </label>
            <label>
              Objetos, produto e marca
              <textarea
                value={briefing.objectBible}
                onChange={updateBriefing("objectBible")}
                rows={3}
              />
            </label>
            <label>
              Direcao visual e acabamento
              <textarea
                value={briefing.visualStyleGuide}
                onChange={updateBriefing("visualStyleGuide")}
                rows={3}
              />
            </label>
            <label>
              Plano para solicitar imagens via OpenAI
              <textarea
                value={briefing.imageGenerationPlan}
                onChange={updateBriefing("imageGenerationPlan")}
                rows={3}
              />
            </label>
            <label>
              Regras de continuidade
              <textarea
                value={briefing.continuityRules}
                onChange={updateBriefing("continuityRules")}
                rows={3}
              />
            </label>
            <label>
              Provider e renderizacao
              <textarea
                value={briefing.providerPlan}
                onChange={updateBriefing("providerPlan")}
                rows={3}
              />
            </label>
            <label>
              Voz
              <textarea
                value={briefing.voiceoverPlan}
                onChange={updateBriefing("voiceoverPlan")}
                rows={3}
              />
            </label>
            <label>
              Trilha
              <textarea
                value={briefing.soundtrackPlan}
                onChange={updateBriefing("soundtrackPlan")}
                rows={3}
              />
            </label>
            <label>
              Legendas
              <textarea
                value={briefing.captionPlan}
                onChange={updateBriefing("captionPlan")}
                rows={3}
              />
            </label>
            <label>
              Montagem e cortes
              <textarea
                value={briefing.editingNotes}
                onChange={updateBriefing("editingNotes")}
                rows={3}
              />
            </label>
            <label>
              Gate de aprovacao
              <textarea
                value={briefing.qualityGate}
                onChange={updateBriefing("qualityGate")}
                rows={4}
              />
            </label>
          </div>
          <button
            className="audio-video-studio-page__primary-action"
            type="button"
            onClick={handleSaveProject}
            disabled={
              isSavingProject ||
              selectedProjectQuery.isLoading ||
              Boolean(durationIssue)
            }
          >
            <Save size={18} aria-hidden="true" />
            {isSavingProject
              ? "Salvando projeto..."
              : isEditingProject
                ? "Salvar continuidade"
                : "Criar blueprint"}
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
            <Sparkles size={20} aria-hidden="true" />
            <div>
              <strong>Pre-producao visual</strong>
              <span>
                Primeiro aprove imagens de personagem, ambiente, produto e
                frames-chave; depois gere cenas com essas referencias.
              </span>
            </div>
          </div>
        </div>
      </section>

      {isEditingProject ? (
        <section className="audio-video-studio-page__section">
          <div className="audio-video-studio-page__section-heading">
            <h2>MP4 gerado para revisao</h2>
            <p>
              Ultimo arquivo renderizado ligado a este projeto para assistir,
              revisar e decidir se entra no funil.
            </p>
          </div>
          {linkedJobsQuery.isLoading ? (
            <article className="audio-video-studio-page__project-card">
              Buscando renders do projeto...
            </article>
          ) : renderedJob && renderedAssetUrl ? (
            <article className="audio-video-studio-page__render-card">
              <div className="audio-video-studio-page__render-preview">
                <video
                  controls
                  playsInline
                  preload="metadata"
                  src={renderedAssetUrl}
                >
                  Seu navegador nao conseguiu carregar este video.
                </video>
              </div>
              <div className="audio-video-studio-page__render-details">
                <span>Pronto para revisao</span>
                <h3>{briefing.title}</h3>
                <p>
                  Job #{renderedJob.id}
                  {renderedJob.providerName
                    ? ` · ${renderedJob.providerName}`
                    : ""}
                  {renderedJob.assetId
                    ? ` · Asset #${renderedJob.assetId}`
                    : ""}
                </p>
                <a
                  className="audio-video-studio-page__secondary-action"
                  href={renderedAssetUrl}
                  rel="noreferrer"
                  target="_blank"
                >
                  Abrir MP4
                </a>
              </div>
            </article>
          ) : (
            <article className="audio-video-studio-page__project-card">
              Nenhum MP4 pronto foi encontrado para este projeto ainda.
            </article>
          )}
        </section>
      ) : null}

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
                <small>{getStudioCommercialLabel(project.status)}</small>
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
          <h2>Estrutura narrativa</h2>
          <p>
            Sequencia do blueprint atual para testar retencao, desejo e acao sem
            depender de improviso.
          </p>
        </div>
        <div className="audio-video-studio-page__timeline">
          {selectedTimeline.map((block) => (
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
            {selectedScenePrompts.map((prompt) => (
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
