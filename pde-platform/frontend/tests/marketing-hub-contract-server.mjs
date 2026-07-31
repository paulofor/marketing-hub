import http from 'node:http';

const port = Number(process.env.MARKETING_HUB_CONTRACT_SERVER_PORT ?? 57181);
const productSlug = 'metodo-musa-7-dias';
const v5ExperienceVersion = 'musa-pde-entry-v5-video-explicativo';
const v6ExperienceVersion = 'musa-pde-entry-v6-video-motivacional';
const v7ExperienceVersion = 'musa-pde-entry-v7-espelho-antes-de-sair';

const baseProduct = {
  slug: productSlug,
  funnelVersion: 'musa-membership-funnel-v1',
  name: 'Método MUSA - Experiência Guiada de 7 Dias',
  promise: 'Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante.',
  audience: 'Mulheres urbanas',
  priceLabel: 'R$67',
  theme: {
    primary: '#7a2444',
    accent: '#d6a75c',
    background: '#fff8f3',
    imageUrl: '/assets/musa-cover.png',
  },
  diagnostic: {
    title: 'Mapa de Presença MUSA',
    intro: 'Entrada publicada pelo Marketing Hub.',
    questions: ['O que minha imagem comunica hoje?'],
  },
  missions: [
    {
      id: 'dia-1-ruido-visual',
      day: 1,
      title: 'Ler o sinal que sua imagem comunica',
      principle: 'A presença cresce quando você identifica o sinal visual.',
      action: 'Escolha uma microação para comunicar mais intenção.',
      evidence: 'Frase preenchida.',
      visualCue: 'Compare a sensação antes/depois.',
    },
  ],
  supportMaterials: [],
  heroVideos: [
    {
      experienceVersion: v6ExperienceVersion,
      placement: 'public_diagnostic_initial_explainer',
      playbackUrl: '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8',
      hlsPlaybackUrl: '/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8',
      autoplay: false,
      muted: false,
      controls: true,
      loop: false,
      playsInline: true,
      source: 'MARKETING_HUB_MANAGED_HLS',
      status: 'READY',
      reviewStatus: 'APPROVED',
    },
  ],
  scientificEvidencePack: {
    version: 'musa-evidence-pack-v1',
    principles: [],
    practicalApplications: [],
    allowedLanguage: [],
    forbiddenClaims: [],
    references: [],
  },
  completionOffer: 'Continuidade',
};

const slots = {
  v5: {
    experienceVersion: v5ExperienceVersion,
    layoutKey: 'video-explicativo',
    publicFirstFold: {
      headline: 'Você se arruma, mas ainda sente que falta presença?',
      supportingText: 'Em poucos minutos, o MUSA identifica o primeiro ajuste de presença.',
      videoCtaLabel: 'Ver meu primeiro ajuste MUSA',
    },
  },
  v6: {
    experienceVersion: v6ExperienceVersion,
    layoutKey: 'video-motivacional',
    publicFirstFold: {
      headline: 'Se o look parece certo, por que você ainda sente que falta presença?',
      supportingText:
        'Em poucos minutos, o MUSA identifica o ruído que enfraquece sua imagem hoje e entrega um ajuste simples para você começar.',
      videoKicker: 'Comece pelo espelho',
      videoHeadline: 'Veja o detalhe que pode estar apagando sua elegância antes de pensar em comprar outra peça.',
      videoSupportingText:
        'O vídeo mostra como roupa, acabamento, cor e postura podem mudar a leitura da sua imagem quando pequenos ruídos saem do caminho.',
      videoExtraText:
        'Depois da prévia, responda 4 escolhas rápidas. O MUSA transforma suas respostas em um Mapa de Presença do Dia 1.',
      videoCtaLabel: 'Revelar meu ajuste MUSA de hoje',
    },
  },
  v7: {
    experienceVersion: v7ExperienceVersion,
    layoutKey: 'espelho-antes-de-sair',
    publicFirstFold: {
      headline: 'Antes de sair, veja por que seu look ainda parece simples demais.',
      supportingText: 'Ajuste o sinal que mais derruba sua presença hoje.',
      videoCtaLabel: 'Revelar meu ajuste antes de sair',
    },
  },
};

function resolveSlot(url) {
  const explicitSlot = url.searchParams.get('slotCode');
  if (explicitSlot && slots[explicitSlot]) {
    return explicitSlot;
  }
  const experienceVersion = url.searchParams.get('experienceVersion');
  return Object.entries(slots).find(([, slot]) => slot.experienceVersion === experienceVersion)?.[0] ?? 'v5';
}

const server = http.createServer((request, response) => {
  const url = new URL(request.url ?? '/', `http://${request.headers.host}`);
  if (request.method !== 'GET' || url.pathname !== `/api/products/public/${productSlug}/pde-experience`) {
    response.writeHead(404, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({ error: 'not found' }));
    return;
  }

  const slot = slots[resolveSlot(url)];
  response.writeHead(200, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
  });
  response.end(JSON.stringify({ ...baseProduct, ...slot }));
});

server.listen(port, '127.0.0.1', () => {
  console.log(`Marketing Hub contract server listening on http://127.0.0.1:${port}`);
});
