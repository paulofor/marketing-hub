import React, { useEffect, useMemo, useState } from 'react';
import {
  BookOpen,
  CalendarDays,
  Check,
  ChevronRight,
  ClipboardCheck,
  Gauge,
  KeyRound,
  Library,
  LogIn,
  Sparkles,
  Target,
  User,
} from 'lucide-react';
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
      action: 'Hoje voce nao vai tentar mudar tudo. Vista ou separe uma combinacao real, olhe cabelo, pele, roupa, perfume e acessorios, escolha o detalhe que mais apaga sua presenca e ajuste apenas esse ponto.',
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
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
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

  async function submitAccess() {
    if (!email.trim()) {
      setErrorMessage(authMode === 'login'
        ? 'Informe o e-mail cadastrado para entrar na sua Area MUSA.'
        : 'Informe seu melhor e-mail para criar o cadastro da Area MUSA.');
      return;
    }
    setLoading(true);
    setErrorMessage('');
    try {
      const response = await fetch(`/api/pde/access/${authMode === 'login' ? 'login' : 'register'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, email }),
      });
      if (!response.ok) {
        throw new Error(authMode === 'login' ? 'Cadastro nao encontrado.' : 'Nao foi possivel criar o cadastro.');
      }
      const access = await response.json();
      setAccessToken(access.token);
      window.history.replaceState(null, '', access.accessUrl);
      await loadWorkspace(access.token);
    } catch {
      setErrorMessage(authMode === 'login'
        ? 'Nao encontramos esse e-mail. Confira o endereco ou crie seu cadastro para iniciar a Area MUSA.'
        : 'Nao conseguimos criar seu cadastro agora. Tente novamente em alguns minutos.');
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
  const firstMission = currentProduct.missions[0];
  const nextMission = currentProduct.missions.find((mission) => !completedMissionIds.has(mission.id)) ?? currentProduct.missions[0];

  if (!workspace) {
    return (
      <main className="app-shell login-shell">
        <section className="login-hero">
          <div className="login-panel">
            <p className="eyebrow">Area exclusiva MUSA</p>
            <h1>Entre na sua experiencia guiada</h1>
            <p className="promise">
              Acesse o diagnostico, o Dia 1 e os materiais premium do Metodo MUSA em um ambiente
              simples para seguir a jornada sem procurar arquivos soltos.
            </p>
            <div className="auth-tabs" aria-label="Tipo de acesso">
              <button
                className={authMode === 'login' ? 'active' : ''}
                onClick={() => {
                  setAuthMode('login');
                  setErrorMessage('');
                }}
                type="button"
              >
                Ja tenho cadastro
              </button>
              <button
                className={authMode === 'register' ? 'active' : ''}
                onClick={() => {
                  setAuthMode('register');
                  setErrorMessage('');
                }}
                type="button"
              >
                Criar cadastro
              </button>
            </div>
            <label className="email-box login-email-box">
              {authMode === 'login' ? 'E-mail cadastrado' : 'E-mail para criar cadastro'}
              <input
                type="email"
                placeholder="seuemail@exemplo.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    submitAccess();
                  }
                }}
              />
            </label>
            {errorMessage && <p className="form-message">{errorMessage}</p>}
            <button className="primary-button login-button" onClick={submitAccess} disabled={loading}>
              <LogIn size={18} />
              {loading
                ? (authMode === 'login' ? 'Entrando...' : 'Criando cadastro...')
                : (authMode === 'login' ? 'Entrar na Area MUSA' : 'Criar cadastro e entrar')}
            </button>
            <p className="access-note">
              Use o mesmo e-mail para manter seu progresso salvo na jornada.
            </p>
          </div>
          <div
            className="experience-card login-cover"
            style={{
              backgroundImage: currentProduct.theme.imageUrl
                ? `linear-gradient(180deg, rgba(45, 32, 36, 0.16), rgba(45, 32, 36, 0.88)), url(${currentProduct.theme.imageUrl})`
                : undefined,
            }}
          >
            <div className="cover-mark">
              <Sparkles size={32} />
            </div>
            <p>Metodo MUSA</p>
            <strong>Diagnostico + 7 missoes + biblioteca premium</strong>
            <span>{currentProduct.audience}</span>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell dashboard-shell">
      <section className="musa-first-fold">
        <div className="musa-hero-copy">
          <p className="eyebrow">Sua Jornada MUSA</p>
          <h1>Sua presenca elegante comeca hoje.</h1>
          <p className="promise">{currentProduct.promise}</p>
          <div className="musa-hero-actions">
            <button className="primary-button" onClick={() => setActiveMissionId(firstMission?.id ?? '')}>
              <Sparkles size={18} />
              Comecar Dia 1
            </button>
            <span>Uma missao curta por dia, com evidencias simples de progresso.</span>
          </div>
        </div>
        <article className="next-mission-hero">
          <div className="next-mission-topline">
            <span>Proxima missao</span>
            <strong>{nextMission ? `Dia ${nextMission.day}` : 'Jornada finalizada'}</strong>
          </div>
          <h2>{nextMission?.title ?? 'Continue sua assinatura MUSA'}</h2>
          <p>
            {nextMission
              ? 'Escolha uma combinacao real, identifique o detalhe que apaga sua presenca e registre a frase que vai guiar seu primeiro ajuste.'
              : currentProduct.completionOffer}
          </p>
          <button
            className="secondary-button next-mission-button"
            onClick={() => setActiveMissionId(nextMission?.id ?? firstMission?.id ?? '')}
          >
            Abrir orientacao
            <ChevronRight size={18} />
          </button>
        </article>
        <aside className="progress-hero-card" aria-label="Progresso da jornada MUSA">
          <Gauge size={24} />
          <span>Progresso</span>
          <strong>{workspace.progressPercent}%</strong>
          <div className="progress-track" aria-label="Progresso da experiencia">
            <span style={{ width: `${workspace.progressPercent}%` }} />
          </div>
          <p>
            {workspace.completedMissions} de {workspace.totalMissions} missoes concluidas.
          </p>
        </aside>
      </section>

      <section className="dashboard-overview dashboard-overview-secondary" aria-label="Resumo da Area MUSA">
        <article className="status-card account-status-card">
          <User size={20} />
          <span>Acesso liberado</span>
          <strong>{workspace.email}</strong>
          <p>Use este e-mail para manter sua jornada salva.</p>
        </article>
        <article className="status-card">
          <ClipboardCheck size={20} />
          <span>Diagnostico</span>
          <strong>Comece pelo espelho</strong>
          <p>Nomeie o que hoje deixa voce arrumada, mas pouco marcante.</p>
        </article>
        <article className="status-card">
          <Library size={20} />
          <span>Biblioteca</span>
          <strong>{currentProduct.supportMaterials.length} materiais</strong>
          <p>E-book, experiencia guiada e arquivos de apoio.</p>
        </article>
        <article className="status-card">
          <KeyRound size={20} />
          <span>Produto ativo</span>
          <strong>{currentProduct.priceLabel}</strong>
          <p>Metodo MUSA liberado para uso.</p>
        </article>
      </section>

      <section className="dashboard-header compact-dashboard-header">
        <div className="dashboard-title">
          <div className="dashboard-icon">
            <CalendarDays size={22} />
          </div>
          <div>
            <p className="eyebrow">Roteiro guiado</p>
            <h2>Diagnostico, missao e materiais de apoio</h2>
            <p>Depois de iniciar o Dia 1, use o diagnostico e a biblioteca apenas como apoio para executar sem se perder.</p>
          </div>
        </div>
      </section>

      <section className="dashboard-main">
        <aside className="customer-sidebar">
          <div
            className="mini-cover"
            style={{
              backgroundImage: currentProduct.theme.imageUrl
                ? `linear-gradient(180deg, rgba(45, 32, 36, 0.06), rgba(45, 32, 36, 0.72)), url(${currentProduct.theme.imageUrl})`
                : undefined,
            }}
          >
            <Sparkles size={24} />
            <span>Metodo MUSA</span>
          </div>
          <div className="diagnostic-panel">
            <Target size={22} />
            <h2>{currentProduct.diagnostic.title}</h2>
            <p>{currentProduct.diagnostic.intro}</p>
            <ul>
              {currentProduct.diagnostic.questions.map((question) => (
                <li key={question}>{question}</li>
              ))}
            </ul>
          </div>
        </aside>

        <section className="mission-panel">
          {firstMission && (
            <article className="start-here-panel">
              <p className="section-kicker">Comece aqui</p>
              <h2>Dia 1: {firstMission.title}</h2>
              <p>
                A primeira missao e escolher uma combinacao real, identificar o detalhe que mais
                apaga sua presenca e escrever a frase de diagnostico. Voce termina o dia sabendo
                exatamente o que ajustar antes de pensar em comprar algo novo.
              </p>
              <button className="inline-action" onClick={() => setActiveMissionId(firstMission.id)}>
                Abrir orientacao do Dia 1
                <ChevronRight size={17} />
              </button>
            </article>
          )}
          <div className="mission-tabs" aria-label="Dias da experiencia">
            {currentProduct.missions.map((mission) => (
              <button
                key={mission.id}
                className={mission.id === activeMission?.id ? 'active' : ''}
                onClick={() => setActiveMissionId(mission.id)}
                title={`Dia ${mission.day}: ${mission.title}`}
              >
                {completedMissionIds.has(mission.id) ? <Check size={16} /> : mission.day}
              </button>
            ))}
          </div>

          {activeMission && (
            <article className="mission-detail">
              <p className="section-kicker">Missao ativa - Dia {activeMission.day}</p>
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
            <p className="section-kicker">Biblioteca da cliente</p>
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
