import React, { useEffect, useMemo, useState } from 'react';
import { BookOpen, Check, ChevronRight, KeyRound, Library, Sparkles, Target } from 'lucide-react';
import { createRoot } from 'react-dom/client';
import './styles.css';

type Theme = {
  primary: string;
  accent: string;
  background: string;
  imageUrl: string;
};

type Diagnostic = {
  title: string;
  intro: string;
  questions: string[];
};

type Mission = {
  id: string;
  day: number;
  title: string;
  principle: string;
  action: string;
  evidence: string;
  visualCue: string;
};

type SupportMaterial = {
  title: string;
  type: string;
  description: string;
  url: string;
};

type ProductExperience = {
  slug: string;
  name: string;
  promise: string;
  audience: string;
  priceLabel: string;
  theme: Theme;
  diagnostic: Diagnostic;
  missions: Mission[];
  supportMaterials: SupportMaterial[];
  completionOffer: string;
};

type Workspace = {
  product: ProductExperience;
  email: string;
  completedMissions: number;
  totalMissions: number;
  progressPercent: number;
  completedMissionIds: string[];
};

const fallbackProduct: ProductExperience = {
  slug: 'metodo-musa-7-dias',
  name: 'Metodo MUSA - Experiencia Guiada de 7 Dias',
  promise: 'Monte em 7 dias uma presenca mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformacao radical.',
  audience: 'Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessiveis.',
  priceLabel: 'R$47',
  theme: {
    primary: '#7a2444',
    accent: '#d6a75c',
    background: '#fff8f3',
    imageUrl: '/assets/musa-cover.png',
  },
  diagnostic: {
    title: 'Diagnostico MUSA',
    intro: 'Comece pelo momento do espelho: quando voce esta pronta, mas sente que ainda falta presenca, acabamento ou intencao.',
    questions: [
      'Quando voce se ve pronta, o que faz pensar: esta ok, mas ainda nao esta marcante?',
      'Seu cabelo, pele, roupa, perfume e acessorios parecem conversar entre si?',
      'Qual compra voce esta quase fazendo para tentar compensar essa sensacao?',
    ],
  },
  missions: [
    {
      id: 'dia-1-ruido-visual',
      day: 1,
      title: 'Sair do quase bom',
      principle: 'A presenca cresce quando voce identifica o detalhe que mais apaga o conjunto.',
      action: 'Escolha uma combinacao real e marque o que hoje gera ruido: excesso, falta de acabamento, cor solta, peca sem intencao ou acessorio perdido.',
      evidence: 'Frase preenchida: eu me sinto arrumada, mas pouco marcante quando...',
      visualCue: 'Compare a sensacao antes/depois de remover ou ajustar um detalhe.',
    },
  ],
  supportMaterials: [
    {
      title: 'E-book Metodo MUSA',
      type: 'PDF',
      description: 'Guia de consulta para entender o metodo, ver exemplos e revisar sua semana.',
      url: '/materials/metodo-musa-ebook.pdf',
    },
    {
      title: 'Experiencia Guiada MUSA',
      type: 'HTML',
      description: 'Versao navegavel da experiencia para consultar a ordem, o diagnostico e as missoes de 7 dias.',
      url: '/materials/experiencia-guiada-musa.html',
    },
    {
      title: 'Plano, Checklists e Templates',
      type: 'CSV',
      description: 'Planilha com a ordem de aplicacao, criterios de conclusao e pontos de atencao de cada material.',
      url: '/materials/plano-checklists-e-templates.csv',
    },
    {
      title: 'Mapa Visual MUSA',
      type: 'Infografico',
      description: 'Resumo visual do metodo: coerencia, reducao de ruido e assinatura pessoal.',
      url: '/materials/mapa-visual-musa.png',
    },
  ],
  completionOffer: 'Ao concluir os 7 dias, voce pode continuar no Clube MUSA com novos desafios mensais.',
};

function App() {
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [product, setProduct] = useState<ProductExperience>(fallbackProduct);
  const [email, setEmail] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [activeMissionId, setActiveMissionId] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const activeMission = useMemo(() => {
    const missionList = workspace?.product.missions ?? product.missions;
    return missionList.find((mission) => mission.id === activeMissionId) ?? missionList[0];
  }, [activeMissionId, product.missions, workspace]);

  useEffect(() => {
    const tokenFromPath = window.location.pathname.match(/^\/access\/([^/]+)/)?.[1] ?? '';
    if (tokenFromPath) {
      setAccessToken(tokenFromPath);
      loadWorkspace(tokenFromPath).catch(() => setErrorMessage('Nao encontramos esse acesso. Confira o link recebido apos a compra.'));
      return;
    }
    fetch('/api/pde/products/metodo-musa-7-dias')
      .then((response) => response.ok ? response.json() : fallbackProduct)
      .then((data: ProductExperience) => {
        setProduct(data);
        setActiveMissionId(data.missions[0]?.id ?? '');
      })
      .catch(() => {
        setProduct(fallbackProduct);
        setActiveMissionId(fallbackProduct.missions[0]?.id ?? '');
      });
  }, []);

  async function createAccess() {
    if (!email.trim()) {
      setErrorMessage('Informe o e-mail usado na compra para liberar sua Area MUSA.');
      return;
    }
    setLoading(true);
    setErrorMessage('');
    try {
      const response = await fetch('/api/pde/access/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, email }),
      });
      if (!response.ok) {
        throw new Error('Nao foi possivel liberar o acesso.');
      }
      const access = await response.json();
      setAccessToken(access.token);
      window.history.replaceState(null, '', access.accessUrl);
      await loadWorkspace(access.token);
    } catch {
      setErrorMessage('Nao conseguimos liberar a Area MUSA agora. Use o ZIP da pagina de obrigado e tente novamente em alguns minutos.');
    } finally {
      setLoading(false);
    }
  }

  async function loadWorkspace(token: string) {
    const response = await fetch(`/api/pde/access/${token}/workspace`);
    if (!response.ok) {
      throw new Error('Acesso nao encontrado.');
    }
    const data = await response.json();
    setWorkspace(data);
    setProduct(data.product);
    setActiveMissionId(data.product.missions[0]?.id ?? '');
  }

  async function completeMission(missionId: string) {
    if (!accessToken) {
      return;
    }
    const response = await fetch(`/api/pde/access/${accessToken}/missions/${missionId}/complete`, { method: 'POST' });
    const data = await response.json();
    setWorkspace(data);
  }

  const currentProduct = workspace?.product ?? product;
  const completedMissionIds = new Set(workspace?.completedMissionIds ?? []);

  return (
    <main className="app-shell">
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Produto Digital Experiencial</p>
          <h1>{currentProduct.name}</h1>
          <p className="promise">{currentProduct.promise}</p>
          <div className="hero-actions">
            <button className="primary-button" onClick={createAccess} disabled={loading}>
              <KeyRound size={18} />
              {workspace ? 'Area liberada' : 'Liberar minha Area MUSA'}
            </button>
            <span className="price-pill">{currentProduct.priceLabel}</span>
          </div>
          <label className="email-box">
            E-mail usado na compra
            <input
              type="email"
              placeholder="seuemail@exemplo.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          {errorMessage && <p className="form-message">{errorMessage}</p>}
        </div>
        <div
          className="experience-card"
          style={{
            backgroundImage: currentProduct.theme.imageUrl
              ? `linear-gradient(180deg, rgba(45, 32, 36, 0.18), rgba(45, 32, 36, 0.9)), url(${currentProduct.theme.imageUrl})`
              : undefined,
          }}
        >
          <div className="cover-mark">
            <Sparkles size={32} />
          </div>
          <p>Experiencia guiada</p>
          <strong>Diagnostico + 7 missoes + biblioteca premium</strong>
          <span>{currentProduct.audience}</span>
        </div>
      </section>

      <section className="progress-band">
        <div>
          <p className="section-kicker">Progresso</p>
          <h2>{workspace ? `${workspace.progressPercent}% concluido` : 'Aguardando liberacao de acesso'}</h2>
        </div>
        <div className="progress-track" aria-label="Progresso da experiencia">
          <span style={{ width: `${workspace?.progressPercent ?? 0}%` }} />
        </div>
      </section>

      <section className="workspace-grid">
        <aside className="diagnostic-panel">
          <Target size={22} />
          <h2>{currentProduct.diagnostic.title}</h2>
          <p>{currentProduct.diagnostic.intro}</p>
          <ul>
            {currentProduct.diagnostic.questions.map((question) => (
              <li key={question}>{question}</li>
            ))}
          </ul>
        </aside>

        <section className="mission-panel">
          <div className="mission-tabs">
            {currentProduct.missions.map((mission) => (
              <button
                key={mission.id}
                className={mission.id === activeMission?.id ? 'active' : ''}
                onClick={() => setActiveMissionId(mission.id)}
              >
                {completedMissionIds.has(mission.id) ? <Check size={16} /> : mission.day}
              </button>
            ))}
          </div>

          {activeMission && (
            <article className="mission-detail">
              <p className="section-kicker">Dia {activeMission.day}</p>
              <h2>{activeMission.title}</h2>
              <div className="mission-block">
                <strong>Principio aplicado</strong>
                <p>{activeMission.principle}</p>
              </div>
              <div className="mission-block">
                <strong>Acao de hoje</strong>
                <p>{activeMission.action}</p>
              </div>
              <div className="mission-block">
                <strong>Evidencia de progresso</strong>
                <p>{activeMission.evidence}</p>
              </div>
              <div className="visual-cue">
                <ChevronRight size={18} />
                {activeMission.visualCue}
              </div>
              <button
                className="secondary-button"
                disabled={!workspace || completedMissionIds.has(activeMission.id)}
                onClick={() => completeMission(activeMission.id)}
              >
                <Check size={18} />
                {completedMissionIds.has(activeMission.id) ? 'Missao concluida' : 'Concluir missao'}
              </button>
            </article>
          )}
        </section>
      </section>

      <section className="library-section">
        <div className="section-heading">
          <Library size={22} />
          <div>
            <p className="section-kicker">Biblioteca</p>
            <h2>Materiais de apoio do metodo</h2>
          </div>
        </div>
        <div className="material-grid">
          {currentProduct.supportMaterials.map((material) => (
            <article className="material-card" key={material.title}>
              <BookOpen size={20} />
              <span>{material.type}</span>
              <h3>{material.title}</h3>
              <p>{material.description}</p>
              <a href={material.url} target="_blank" rel="noreferrer">
                Abrir material
              </a>
            </article>
          ))}
        </div>
      </section>

      <section className="completion-band">
        <Sparkles size={22} />
        <p>{currentProduct.completionOffer}</p>
      </section>
    </main>
  );
}

const root = createRoot(document.getElementById('root') as HTMLElement);
root.render(<App />);
