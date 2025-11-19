import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { environment } from './config/environment'
import { useUiStore } from './state/uiStore'
import { fetchContentCards, fetchHealth } from './services/contentApi'
import type { ContentCard, Role } from './types/content'

const roleOptions: { label: string; value: Role; description: string }[] = [
  { label: 'Visitante (ANON)', value: 'ANON', description: 'Vê cards públicos sem abrir premium' },
  {
    label: 'Lead logado (LEAD)',
    value: 'LEAD',
    description: 'Explora a vitrine, mas precisa pagar para abrir conteúdos premium'
  },
  {
    label: 'Cliente (CLIENTE)',
    value: 'CLIENTE',
    description: 'Possui compra aprovada e abre conteúdos premium'
  },
  { label: 'Admin', value: 'ADMIN', description: 'Pode abrir todos os conteúdos' }
]

const renderAccessBadge = (card: ContentCard) => (
  <span className={`badge ${card.accessType === 'PREMIUM' ? 'premium' : 'free'}`}>
    {card.accessType === 'PREMIUM' ? 'Premium' : 'Free'}
  </span>
)

function App() {
  const navigationOpen = useUiStore((state) => state.navigationOpen)
  const toggleNavigation = useUiStore((state) => state.toggleNavigation)

  const [role, setRole] = useState<Role>('LEAD')

  const { data: healthData, isLoading: healthLoading, isError: healthError } = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    retry: 1
  })

  const {
    data: contents,
    isLoading: contentsLoading,
    isError: contentsError
  } = useQuery({
    queryKey: ['contents', role],
    queryFn: () => fetchContentCards(role),
    retry: 1
  })

  const premiumCount = contents?.filter((card) => card.accessType === 'PREMIUM').length ?? 0

  return (
    <div className="app-shell">
      <aside className={`sidebar ${navigationOpen ? '' : 'collapsed'}`}>
        <button className="brand" onClick={toggleNavigation} aria-label="Alternar navegação">
          <span className="brand-icon">V</span>
          {navigationOpen && <span>Vitrines Lead</span>}
        </button>
        <div className="nav-item">
          <span className="icon">🗂️</span>
          {navigationOpen && <span>Dashboard</span>}
        </div>
        <div className="nav-item">
          <span className="icon">🔒</span>
          {navigationOpen && <span>Regras de acesso</span>}
        </div>
        <div className="nav-item">
          <span className="icon">💳</span>
          {navigationOpen && <span>Checkout</span>}
        </div>
      </aside>

      <main className="content">
        <header className="page-header">
          <div>
            <p className="small">Stack: React 18 + Vite + TypeScript</p>
            <h1 className="page-title">Vitrine pronta para conectar ao backend</h1>
          </div>
          <div className="actions">
            <a className="button" href="/" aria-label="Voltar para o portal Lead">
              ↩️ Portal Lead
            </a>
            <a className="button secondary" href="https://vitejs.dev/guide/" target="_blank" rel="noreferrer">
              📘 Guia Vite
            </a>
          </div>
        </header>

        <section className="status-row">
          <article className="card status-card">
            <div>
              <p className="small">Backend</p>
              <h3>Health check</h3>
              <p className="muted">{environment.apiBaseUrl}/health</p>
            </div>
            <div className={`status ${healthError ? 'error' : ''}`}>
              {healthLoading && <span>⏳ Consultando saúde...</span>}
              {!healthLoading && healthError && <span>❌ Backend indisponível</span>}
              {!healthLoading && healthData && (
                <span>
                  ✅ {healthData.status} — build {healthData.version}
                </span>
              )}
            </div>
          </article>

          <article className="card status-card">
            <div className="status-card__header">
              <div>
                <p className="small">RBAC</p>
                <h3>Simular role do usuário</h3>
              </div>
              <select
                className="role-select"
                value={role}
                onChange={(event) => setRole(event.target.value as Role)}
                aria-label="Selecionar role para simular a vitrine"
              >
                {roleOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <ul className="role-list">
              {roleOptions.map((option) => (
                <li key={option.value} className={role === option.value ? 'active' : ''}>
                  <div className="role-label">{option.label}</div>
                  <p className="small">{option.description}</p>
                </li>
              ))}
            </ul>
          </article>
        </section>

        <section className="grid">
          <article className="card">
            <h3>Listagem e abertura</h3>
            <p>
              A API expõe <code>GET /conteudos</code> e <code>GET /conteudos/&#123;id&#125;</code>, calculando o campo
              <strong> locked</strong> a partir da role e do plano comprado. Conteúdos premium retornam URL assinada para
              storage privado.
            </p>
            <p className="small">Modelos de dados: usuários, planos, conteúdos e compras com status PAID/CANCELLED.</p>
          </article>

          <article className="card">
            <h3>Checkout</h3>
            <p>
              Endpoint <code>POST /checkout</code> gera o link de pagamento para o plano selecionado. É o ponto de entrada para
              integrar Stripe, Mercado Pago ou PagSeguro e receber notificações por webhook.
            </p>
            <p className="small">O backend já valida e-mail e plano enviados no corpo da requisição.</p>
          </article>

          <article className="card">
            <h3>Magic link por e-mail</h3>
            <p>
              O fluxo sugere tokens de uso único, enviados em links do tipo <code>/auth/magic?token=</code>. O backend valida
              assinatura e expiração e retorna sessão sem exigir senha.
            </p>
            <p className="small">Ideal para converter leads rapidamente sem fricção.</p>
          </article>
        </section>

        <section className="content-section">
          <div className="section-header">
            <div>
              <p className="small">Cards da vitrine</p>
              <h2>Conteúdos disponíveis</h2>
              <p className="muted">
                Sinta a experiência Netflix: rótulo premium/gratuito, badge de bloqueio e cálculo de permissões vindo do
                backend.
              </p>
            </div>
            <div className="pill">
              {premiumCount} premium · {contents?.length ?? 0} total
            </div>
          </div>

          {contentsLoading && <p className="muted">Carregando cards...</p>}
          {contentsError && <p className="error">Não foi possível carregar os conteúdos.</p>}

          <div className="content-grid">
            {contents?.map((card) => (
              <article key={card.id} className="content-card">
                <div className="cover" style={{ backgroundImage: `url(${card.coverImageUrl})` }}>
                  <div className="cover-gradient" />
                  <div className="cover-content">
                    {renderAccessBadge(card)}
                    {card.locked && <span className="lock">🔒</span>}
                  </div>
                </div>
                <div className="card-body">
                  <div className="card-title-row">
                    <h3>{card.title}</h3>
                    {card.planId && <span className="chip">{card.planId}</span>}
                  </div>
                  <p className="small">{card.description}</p>
                  <div className="cta-row">
                    <button className="button tertiary" disabled={card.locked}>
                      {card.locked ? 'Assinar para abrir' : 'Abrir conteúdo'}
                    </button>
                    <button className="ghost">Checkout rápido</button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  )
}

export default App
