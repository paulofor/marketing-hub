import { useQuery } from '@tanstack/react-query'
import { environment } from './config/environment'
import { useUiStore } from './state/uiStore'

const fetchHealth = async () => {
  const response = await fetch(`${environment.apiBaseUrl}/health`)
  if (!response.ok) {
    throw new Error('Falha ao consultar o backend das vitrines')
  }

  return response.json() as Promise<{ status: string; version: string }>
}

function App() {
  const navigationOpen = useUiStore((state) => state.navigationOpen)
  const toggleNavigation = useUiStore((state) => state.toggleNavigation)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    retry: 1
  })

  return (
    <div className="app-shell">
      <aside className={`sidebar ${navigationOpen ? '' : 'collapsed'}`}>
        <button className="brand" onClick={toggleNavigation} aria-label="Alternar navegação">
          <span className="brand-icon">V</span>
          {navigationOpen && <span>Vitrines Lead</span>}
        </button>
        <div className="nav-item">
          <span className="icon">🗂️</span>
          {navigationOpen && <span>Dashboard inicial</span>}
        </div>
        <div className="nav-item">
          <span className="icon">🚀</span>
          {navigationOpen && <span>Publicações no host do portal</span>}
        </div>
      </aside>

      <main className="content">
        <header className="page-header">
          <div>
            <p className="small">Stack: React 18 + Vite + TypeScript</p>
            <h1 className="page-title">Vitrines</h1>
          </div>
          <div className="actions">
            <a className="button" href="/" aria-label="Voltar para o portal Lead">
              ↩️ Portal Lead
            </a>
            <a
              className="button secondary"
              href="https://vitejs.dev/guide/"
              target="_blank"
              rel="noreferrer"
            >
              📘 Guia Vite
            </a>
          </div>
        </header>

        <section className="grid">
          <article className="card">
            <h3>Backend Spring Boot</h3>
            <p>
              API dedicada das vitrines exposta no mesmo host do Portal Lead. Utilize a variável{' '}
              <code>VITE_API_BASE_URL</code> para apontar as chamadas.
            </p>
            <div className={`status ${isError ? 'error' : ''}`}>
              {isLoading && <span>⏳ Consultando saúde...</span>}
              {!isLoading && isError && <span>❌ Backend indisponível</span>}
              {!isLoading && data && (
                <span>
                  ✅ {data.status} — build {data.version}
                </span>
              )}
            </div>
            <p className="small">Endpoint padrão: {environment.apiBaseUrl}/health</p>
          </article>

          <article className="card">
            <h3>Frontend</h3>
            <p>
              Interface leve para configurar vitrines e publicar containers no mesmo host do portal. Esta estrutura já inclui
              TanStack Query para chamadas HTTP e Zustand para estado global.
            </p>
            <p className="small">
              Comandos: <code>npm run dev</code> para desenvolvimento e <code>npm run build</code> para produção.
            </p>
          </article>

          <article className="card">
            <h3>Imagens e containers</h3>
            <p>
              O repositório traz Dockerfiles separados para frontend e backend. Publique as imagens no host do portal Lead para
              garantir que a API e a UI fiquem lado a lado.
            </p>
            <p className="small">Consulte o README para o fluxo completo de build e push.</p>
          </article>
        </section>
      </main>
    </div>
  )
}

export default App
