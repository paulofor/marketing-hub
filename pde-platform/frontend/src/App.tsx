import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  BookOpen,
  CalendarDays,
  Check,
  ChevronRight,
  ClipboardCheck,
  CreditCard,
  Gauge,
  KeyRound,
  Library,
  Lock,
  LogIn,
  Mail,
  Pencil,
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
  accessSource: string;
  subscriptionStatus: 'ACTIVE' | 'TRIAL';
  completedMissions: number;
  totalMissions: number;
  progressPercent: number;
  completedMissionIds: string[];
};

type MagicLinkResponse = {
  productSlug: string;
  email: string;
  deliveryStatus: string;
  accessUrl?: string;
};

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
          }) => void;
          renderButton: (element: HTMLElement, options: Record<string, string | number | boolean>) => void;
        };
      };
    };
  }
}

const fallbackProduct: ProductExperience = {
  slug: 'metodo-musa-7-dias',
  name: 'Método MUSA - Experiência Guiada de 7 Dias',
  promise: 'Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.',
  audience: 'Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.',
  priceLabel: '',
  theme: {
    primary: '#7a2444',
    accent: '#d6a75c',
    background: '#fff8f3',
    imageUrl: '/assets/musa-cover.png',
  },
  diagnostic: {
    title: 'Diagnóstico MUSA',
    intro: 'Comece pelo momento do espelho: quando você está pronta, mas sente que ainda falta presença, acabamento ou intenção.',
    questions: [
      'Quando você se vê pronta, o que faz pensar: está ok, mas ainda não está marcante?',
      'Seu cabelo, pele, roupa, perfume e acessórios parecem conversar entre si?',
      'Qual compra você está quase fazendo para tentar compensar essa sensação?',
    ],
  },
  missions: [
    {
      id: 'dia-1-ruido-visual',
      day: 1,
      title: 'Sair do quase bom',
      principle: 'A presença cresce quando você identifica o detalhe que mais apaga o conjunto.',
      action: 'Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe cabelo, pele, roupa, perfume e acessórios, escolha o detalhe que mais apaga sua presença e ajuste apenas esse ponto.',
      evidence: 'Frase preenchida: eu me sinto arrumada, mas pouco marcante quando...',
      visualCue: 'Compare a sensação antes/depois de remover ou ajustar um detalhe.',
    },
  ],
  supportMaterials: [
    {
      title: 'E-book Método MUSA',
      type: 'PDF',
      description: 'Guia de consulta para entender o método, ver exemplos e revisar sua semana.',
      url: '/materials/metodo-musa-ebook.pdf',
    },
    {
      title: 'Experiência Guiada MUSA',
      type: 'HTML',
      description: 'Versão navegável da experiência para consultar a ordem, o diagnóstico e as missões de 7 dias.',
      url: '/materials/experiencia-guiada-musa.html',
    },
    {
      title: 'Plano, Checklists e Templates',
      type: 'CSV',
      description: 'Planilha com a ordem de aplicação, critérios de conclusão e pontos de atenção de cada material.',
      url: '/materials/plano-checklists-e-templates.csv',
    },
    {
      title: 'Mapa Visual MUSA',
      type: 'Infográfico',
      description: 'Resumo visual do método: coerência, redução de ruído e assinatura pessoal.',
      url: '/materials/mapa-visual-musa.png',
    },
  ],
  completionOffer: 'Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais.',
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
  const [successMessage, setSuccessMessage] = useState('');
  const [devAccessUrl, setDevAccessUrl] = useState('');
  const firstUseTrackedRef = useRef(false);
  const emailInputRef = useRef<HTMLInputElement>(null);
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;
  const checkoutUrl = (import.meta.env.VITE_MUSA_CHECKOUT_URL as string | undefined) ?? '';

  const activeMission = useMemo(() => {
    const missionList = workspace?.product.missions ?? product.missions;
    return missionList.find((mission) => mission.id === activeMissionId) ?? missionList[0];
  }, [activeMissionId, product.missions, workspace]);

  useEffect(() => {
    trackEvent('PED_ENTRY', { source: 'frontend_entry' });
    const tokenFromPath = window.location.pathname.match(/^\/access\/([^/]+)/)?.[1] ?? '';
    if (tokenFromPath) {
      setAccessToken(tokenFromPath);
      loadWorkspace(tokenFromPath).catch(() => setErrorMessage('Não encontramos esse acesso. Confira o link recebido após a compra.'));
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

  useEffect(() => {
    if (!googleClientId || workspace) {
      return;
    }
    const scriptId = 'google-identity-services';
    const renderGoogleButton = () => {
      const container = document.getElementById('google-login-button');
      if (!container || !window.google) {
        return;
      }
      container.innerHTML = '';
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: (response) => {
          if (response.credential) {
            submitGoogleAccess(response.credential);
          }
        },
      });
      window.google.accounts.id.renderButton(container, {
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        width: 320,
      });
    };
    const existingScript = document.getElementById(scriptId);
    if (existingScript) {
      renderGoogleButton();
      return;
    }
    const script = document.createElement('script');
    script.id = scriptId;
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = renderGoogleButton;
    document.head.appendChild(script);
  }, [googleClientId, workspace]);

  useEffect(() => {
    if (workspace?.subscriptionStatus === 'TRIAL') {
      trackEvent('PAYWALL_VIEWED', {
        accessToken,
        email: workspace.email,
        provider: workspace.accessSource,
        metadata: { placement: 'dashboard_paywall' },
      });
    }
  }, [workspace?.subscriptionStatus, accessToken]);

  async function submitAccess() {
    if (!email.trim()) {
      setErrorMessage(authMode === 'login'
        ? 'Informe o e-mail cadastrado para entrar na sua Área MUSA.'
        : 'Informe seu melhor e-mail para criar o cadastro da Área MUSA.');
      return;
    }
    setLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    setDevAccessUrl('');
    try {
      await trackEvent('LOGIN_STARTED', {
        email,
        provider: 'EMAIL_MAGIC_LINK',
        metadata: { authMode },
      });
      const response = await fetch('/api/pde/access/magic-link', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, email }),
      });
      if (!response.ok) {
        throw new Error('Não foi possível enviar o link de acesso.');
      }
      const result: MagicLinkResponse = await response.json();
      setSuccessMessage(result.deliveryStatus === 'SENT'
        ? 'Enviamos o link de acesso para seu e-mail. Abra o link para continuar.'
        : 'Link de teste gerado. Como o envio por e-mail ainda não está configurado neste ambiente, use o botão abaixo para abrir seu acesso.');
      if (result.accessUrl) {
        setDevAccessUrl(result.accessUrl);
      }
    } catch {
      setErrorMessage('Não conseguimos enviar seu link agora. Confira o e-mail e tente novamente.');
    } finally {
      setLoading(false);
    }
  }

  async function submitGoogleAccess(idToken: string) {
    setLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      await trackEvent('LOGIN_STARTED', {
        provider: 'GOOGLE',
        metadata: { authMode: 'google' },
      });
      const response = await fetch('/api/pde/access/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productSlug: product.slug, idToken }),
      });
      if (!response.ok) {
        throw new Error('Google não autorizado.');
      }
      const access = await response.json();
      setAccessToken(access.token);
      window.history.replaceState(null, '', access.accessUrl);
      await loadWorkspace(access.token);
    } catch {
      setErrorMessage('Não conseguimos entrar com Google agora. Use o link por e-mail como alternativa.');
    } finally {
      setLoading(false);
    }
  }

  async function loadWorkspace(token: string) {
    const response = await fetch(`/api/pde/access/${token}/workspace`);
    if (!response.ok) {
      throw new Error('Acesso não encontrado.');
    }
    const data = await response.json();
    setWorkspace(data);
    setProduct(data.product);
    setActiveMissionId(data.product.missions[0]?.id ?? '');
  }

  async function trackEvent(
    eventType: string,
    options: {
      accessToken?: string;
      email?: string;
      provider?: string;
      source?: string;
      metadata?: Record<string, unknown>;
    } = {},
  ) {
    try {
      await fetch('/api/pde/access/events', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          productSlug: product.slug,
          eventType,
          accessToken: options.accessToken,
          email: options.email,
          provider: options.provider,
          source: options.source ?? 'pde-platform-frontend',
          pageUrl: window.location.href,
          metadata: options.metadata,
        }),
      });
    } catch {
      // Eventos de funil não devem bloquear login, compra ou consumo da experiência.
    }
  }

  async function handleSubscriptionClick() {
    if (!workspace) {
      return;
    }
    await trackEvent('SUBSCRIPTION_CLICKED', {
      accessToken,
      email: workspace.email,
      provider: workspace.accessSource,
      metadata: { checkoutConfigured: Boolean(checkoutUrl) },
    });
    if (checkoutUrl) {
      window.open(checkoutUrl, '_blank', 'noopener,noreferrer');
      return;
    }
    setErrorMessage('Checkout de assinatura ainda não configurado para este ambiente.');
  }

  function editAccessEmail() {
    setSuccessMessage('');
    setDevAccessUrl('');
    emailInputRef.current?.focus();
    emailInputRef.current?.select();
  }

  function trackFirstUse(activationType: string, metadata: Record<string, unknown> = {}) {
    if (!workspace || !hasActiveSubscription || firstUseTrackedRef.current) {
      return;
    }
    firstUseTrackedRef.current = true;
    trackEvent('FIRST_USE', {
      accessToken,
      email: workspace.email,
      provider: workspace.accessSource,
      metadata: { activationType, ...metadata },
    });
  }

  function openMission(missionId: string, activationType = 'mission_open') {
    setActiveMissionId(missionId);
    trackFirstUse(activationType, { missionId });
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
  const hasActiveSubscription = workspace?.subscriptionStatus === 'ACTIVE';

  if (!workspace) {
    return (
      <main className="app-shell login-shell">
        <section className="login-hero">
          <div className="login-panel">
            <p className="eyebrow">Clube MUSA</p>
            <h1>Descubra o que hoje apaga a sua presença.</h1>
            <p className="promise">
              Entre para liberar seu diagnóstico inicial e o Dia 1 do Método MUSA: uma experiência
              guiada para construir presença elegante em 7 dias, sem luxo caro nem compra por impulso.
            </p>
            <div className="login-value-strip" aria-label="O que fica disponível ao entrar">
              <span><Check size={16} /> Diagnóstico gratuito</span>
              <span><Sparkles size={16} /> Dia 1 liberado</span>
              <span><Lock size={16} /> Continuação premium</span>
            </div>
            <div className="login-preview-card">
              <div>
                <span>Primeira parte liberada</span>
                <strong>Seu espelho MUSA</strong>
                <p>Nomeie o detalhe que faz você se sentir arrumada, mas ainda pouco marcante.</p>
              </div>
              <ChevronRight size={22} />
            </div>
            <div className="auth-tabs" aria-label="Tipo de acesso">
              <button
                className={authMode === 'login' ? 'active' : ''}
                onClick={() => {
                  setAuthMode('login');
                  setErrorMessage('');
                }}
                type="button"
              >
                Já tenho cadastro
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
            {googleClientId && (
              <div className="social-login-block">
                <div id="google-login-button" aria-label="Entrar com Google" />
                <span>Mais rápido para entrar e salvar sua primeira orientação.</span>
              </div>
            )}
            <div className="auth-divider">
              <span>ou entre com um link seguro por e-mail</span>
            </div>
            <label className="email-box login-email-box">
              {authMode === 'login' ? 'E-mail cadastrado' : 'E-mail para criar cadastro'}
              <input
                ref={emailInputRef}
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
            {successMessage && <p className="form-message success-message">{successMessage}</p>}
            {successMessage && (
              <button className="inline-action edit-email-button" onClick={editAccessEmail} type="button">
                <Pencil size={16} />
                Editar e-mail
              </button>
            )}
            <button className="primary-button login-button" onClick={submitAccess} disabled={loading}>
              <Mail size={18} />
              {loading
                ? 'Enviando link...'
                : (authMode === 'login' ? 'Receber meu link' : 'Liberar meu Dia 1')}
            </button>
            {devAccessUrl && (
              <button
                className="secondary-button dev-access-button"
                onClick={() => {
                  window.history.replaceState(null, '', devAccessUrl);
                  const token = devAccessUrl.split('/access/')[1] ?? '';
                  setAccessToken(token);
                  loadWorkspace(token);
                }}
                type="button"
              >
                <LogIn size={18} />
                Abrir acesso de teste
              </button>
            )}
            <p className="access-note">
              O login libera a primeira parte da experiência. Dias 2 a 7, biblioteca e materiais
              premium aparecem dentro da área e são desbloqueados com o acesso completo.
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
            <div className="login-cover-content">
              <p>Método MUSA</p>
              <strong>Uma jornada de 7 dias para parecer mais elegante com o que você já tem.</strong>
              <span>Entre, veja seu primeiro diagnóstico e descubra o próximo ajuste que pode mudar sua presença.</span>
            </div>
            <div className="login-unlock-list" aria-label="Prévia da experiência MUSA">
              <span><CalendarDays size={16} /> 7 missões guiadas</span>
              <span><BookOpen size={16} /> Biblioteca de apoio</span>
              <span><Target size={16} /> Checklists práticos</span>
            </div>
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
          <h1>Sua presença elegante começa hoje.</h1>
          <p className="promise">{currentProduct.promise}</p>
          <div className="musa-hero-actions">
            <button className="primary-button" onClick={() => openMission(firstMission?.id ?? '', 'primary_start')}>
              <Sparkles size={18} />
              Começar Dia 1
            </button>
            <span>Uma missão curta por dia, com evidências simples de progresso.</span>
          </div>
        </div>
        <article className="next-mission-hero">
          <div className="next-mission-topline">
            <span>Próxima missão</span>
            <strong>{nextMission ? `Dia ${nextMission.day}` : 'Jornada finalizada'}</strong>
          </div>
          <h2>{nextMission?.title ?? 'Continue sua assinatura MUSA'}</h2>
          <p>
            {nextMission
              ? 'Escolha uma combinação real, identifique o detalhe que apaga sua presença e registre a frase que vai guiar seu primeiro ajuste.'
              : currentProduct.completionOffer}
          </p>
          <button
            className="secondary-button next-mission-button"
            onClick={() => openMission(nextMission?.id ?? firstMission?.id ?? '', 'next_mission_open')}
          >
            Abrir orientação
            <ChevronRight size={18} />
          </button>
        </article>
        <aside className="progress-hero-card" aria-label="Progresso da jornada MUSA">
          <Gauge size={24} />
          <span>Progresso</span>
          <strong>{workspace.progressPercent}%</strong>
          <div className="progress-track" aria-label="Progresso da experiência">
            <span style={{ width: `${workspace.progressPercent}%` }} />
          </div>
          <p>
            {workspace.completedMissions} de {workspace.totalMissions} missões concluídas.
          </p>
        </aside>
      </section>

      <section className="dashboard-overview dashboard-overview-secondary" aria-label="Resumo da Área MUSA">
        <article className="status-card account-status-card">
          <User size={20} />
          <span>Acesso liberado</span>
          <strong>{workspace.email}</strong>
          <p>Use este e-mail para manter sua jornada salva.</p>
        </article>
        <article className="status-card">
          <ClipboardCheck size={20} />
          <span>Diagnóstico</span>
          <strong>Comece pelo espelho</strong>
          <p>Nomeie o que hoje deixa você arrumada, mas pouco marcante.</p>
        </article>
        <article className="status-card">
          <Library size={20} />
          <span>Biblioteca</span>
          <strong>{currentProduct.supportMaterials.length} materiais</strong>
          <p>E-book, experiência guiada e arquivos de apoio.</p>
        </article>
        <article className="status-card">
          <KeyRound size={20} />
          <span>{hasActiveSubscription ? 'Produto ativo' : 'Assinatura'}</span>
          <strong>{hasActiveSubscription ? 'Acesso completo' : 'Pendente'}</strong>
          <p>{hasActiveSubscription ? 'Método MUSA liberado para uso.' : 'Assine para liberar todos os recursos.'}</p>
        </article>
      </section>

      {!hasActiveSubscription && (
        <section className="subscription-paywall" aria-label="Oferta de assinatura MUSA">
          <div>
            <p className="section-kicker">Liberar área completa</p>
            <h2>Assine o Clube MUSA para acessar todas as missões, biblioteca e próximos desafios.</h2>
            <p>
              Seu login já salvou a entrada na área. A assinatura remove o bloqueio e permite continuar a experiência completa.
            </p>
          </div>
          <button className="primary-button" onClick={handleSubscriptionClick}>
            <CreditCard size={18} />
            Solicitar acesso pago
          </button>
        </section>
      )}

      <section className="dashboard-header compact-dashboard-header">
        <div className="dashboard-title">
          <div className="dashboard-icon">
            <CalendarDays size={22} />
          </div>
          <div>
            <p className="eyebrow">Roteiro guiado</p>
            <h2>Diagnóstico, missão e materiais de apoio</h2>
            <p>Depois de iniciar o Dia 1, use o diagnóstico e a biblioteca apenas como apoio para executar sem se perder.</p>
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
            <span>Método MUSA</span>
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
                A primeira missão é escolher uma combinação real, identificar o detalhe que mais
                apaga sua presença e escrever a frase de diagnóstico. Você termina o dia sabendo
                exatamente o que ajustar antes de pensar em comprar algo novo.
              </p>
              <button className="inline-action" onClick={() => openMission(firstMission.id, 'start_here_open')}>
                Abrir orientação do Dia 1
                <ChevronRight size={17} />
              </button>
            </article>
          )}
          <div className="mission-tabs" aria-label="Dias da experiência">
            {currentProduct.missions.map((mission) => (
              <button
                key={mission.id}
                className={`${mission.id === activeMission?.id ? 'active' : ''} ${!hasActiveSubscription && mission.id !== firstMission?.id ? 'locked' : ''}`}
                onClick={() => {
                  if (!hasActiveSubscription && mission.id !== firstMission?.id) {
                    handleSubscriptionClick();
                    return;
                  }
                  openMission(mission.id, 'mission_tab_open');
                }}
                title={`Dia ${mission.day}: ${mission.title}`}
              >
                {!hasActiveSubscription && mission.id !== firstMission?.id
                  ? <Lock size={15} />
                  : completedMissionIds.has(mission.id) ? <Check size={16} /> : mission.day}
              </button>
            ))}
          </div>

          {activeMission && (
            <article className="mission-detail">
              <p className="section-kicker">Missão ativa - Dia {activeMission.day}</p>
              <h2>{activeMission.title}</h2>
              <div className="mission-block">
                <strong>Princípio aplicado</strong>
                <p>{activeMission.principle}</p>
              </div>
              <div className="mission-block">
                <strong>Ação de hoje</strong>
                <p>{activeMission.action}</p>
              </div>
              <div className="mission-block">
                <strong>Evidência de progresso</strong>
                <p>{activeMission.evidence}</p>
              </div>
              <div className="visual-cue">
                <ChevronRight size={18} />
                {activeMission.visualCue}
              </div>
              <button
                className="secondary-button"
                disabled={!workspace || completedMissionIds.has(activeMission.id) || !hasActiveSubscription}
                onClick={() => completeMission(activeMission.id)}
              >
                {hasActiveSubscription ? <Check size={18} /> : <Lock size={18} />}
                {hasActiveSubscription
                  ? (completedMissionIds.has(activeMission.id) ? 'Missão concluída' : 'Concluir missão')
                  : 'Assine para salvar progresso'}
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
            <h2>Materiais de apoio do método</h2>
          </div>
        </div>
        <div className="material-grid">
          {currentProduct.supportMaterials.map((material) => (
            <article className="material-card" key={material.title}>
              <BookOpen size={20} />
              <span>{material.type}</span>
              <h3>{material.title}</h3>
              <p>{material.description}</p>
              {hasActiveSubscription ? (
                <a
                  href={material.url}
                  target="_blank"
                  rel="noreferrer"
                  onClick={() => trackFirstUse('material_open', { materialTitle: material.title, materialType: material.type })}
                >
                  Abrir material
                </a>
              ) : (
                <button className="inline-action" onClick={handleSubscriptionClick}>
                  <Lock size={16} />
                  Liberar com assinatura
                </button>
              )}
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
