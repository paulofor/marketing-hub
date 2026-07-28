import { NavLink } from 'react-router-dom';
import { ReactNode, useEffect, useState } from 'react';
import clsx from 'clsx';
import client from '../api/client';

const links = [
  { to: '/', label: 'Dashboard' },
  { to: '/prompts', label: 'Prompts' },
  { to: '/prompt-hints', label: 'Itens do Prompt' },
  { to: '/prompt-lists', label: 'Lista de Prompts' },
  { to: '/environments', label: 'Ambientes' },
  { to: '/problems', label: 'Problemas' },
  { to: '/logs', label: 'Interpretador de Logs' },
  { to: '/codex', label: 'Codex' },
  { to: '/codex-chatgpt', label: 'Codex ChatGPT' },
  { to: '/codex-chatgpt-mkt', label: 'Codex ChatGPT MKT' },
  { to: '/codex/models', label: 'Modelos Codex' },
  { to: '/audit', label: 'Audit Log' }
];

interface OperationalMetricsPeriod {
  startsAt?: string;
  requestCount?: number;
  interactionCount?: number;
  durationMs?: number;
}

interface OperationalMetrics {
  day?: OperationalMetricsPeriod;
}

const formatOperationalDate = (value?: string) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'America/Sao_Paulo'
  });
};

const formatOperationalDuration = (value?: number) => {
  if (value === undefined || value === null || !Number.isFinite(value) || value < 0) {
    return '0min';
  }

  const totalMinutes = Math.floor(value / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours > 0) {
    return `${hours}h ${minutes}min`;
  }
  return `${minutes}min`;
};

const formatOperationalNumber = (value?: number) => {
  if (value === undefined || value === null || !Number.isFinite(value)) {
    return '—';
  }
  return value.toLocaleString('pt-BR');
};

function OperationalMetricsFloatingCard() {
  const [metrics, setMetrics] = useState<OperationalMetrics | null>(null);

  useEffect(() => {
    let active = true;

    const loadMetrics = () => {
      client.get<OperationalMetrics>('/codex/requests/metrics')
        .then((response) => {
          if (active) {
            setMetrics(response.data);
          }
        })
        .catch(() => {
          if (active) {
            setMetrics(null);
          }
        });
    };

    loadMetrics();
    const intervalId = window.setInterval(loadMetrics, 60_000);
    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, []);

  const day = metrics?.day;

  return (
    <aside className="fixed right-4 top-4 z-40 w-[220px] rounded-lg border border-slate-200 bg-white/95 p-4 text-right shadow-lg backdrop-blur dark:border-slate-800 dark:bg-slate-950/95 sm:right-8 sm:top-6">
      <p className="text-xs font-medium uppercase text-slate-500 dark:text-slate-400">Dia operacional</p>
      <p className="mt-1 text-sm font-semibold text-slate-700 dark:text-slate-200">{formatOperationalDate(day?.startsAt)}</p>
      <p className="mt-2 text-base font-bold text-emerald-700 dark:text-emerald-300">{formatOperationalDuration(day?.durationMs)}</p>
      <div className="mt-4 grid grid-cols-2 gap-2 text-center">
        <div className="rounded-md border border-slate-200 bg-slate-50 px-2 py-2 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-[10px] font-medium uppercase text-slate-500 dark:text-slate-400">Solicitações</p>
          <p className="mt-1 text-sm font-bold text-slate-800 dark:text-slate-100">{formatOperationalNumber(day?.requestCount)}</p>
        </div>
        <div className="rounded-md border border-slate-200 bg-slate-50 px-2 py-2 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-[10px] font-medium uppercase text-slate-500 dark:text-slate-400">Interações</p>
          <p className="mt-1 text-sm font-bold text-slate-800 dark:text-slate-100">{formatOperationalNumber(day?.interactionCount)}</p>
        </div>
      </div>
      <p className="mt-2 text-[11px] text-slate-500 dark:text-slate-400">Corte às 03:00 · São Paulo</p>
    </aside>
  );
}

export default function ShellLayout({ children }: { children: ReactNode }) {
  const [dark, setDark] = useState(false);

  return (
    <div className={clsx('min-h-screen', dark ? 'dark' : '')}>
      <div className="flex min-h-screen bg-slate-100 dark:bg-slate-900">
        <OperationalMetricsFloatingCard />
        <aside className="w-60 bg-white/80 dark:bg-slate-950/40 backdrop-blur border-r border-slate-200 dark:border-slate-800 p-4">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-lg font-bold text-slate-800 dark:text-slate-100">AI Hub 6</h1>
            <button
              onClick={() => setDark((prev) => !prev)}
              className="text-xs px-2 py-1 border rounded-md border-slate-300 dark:border-slate-700"
            >
              {dark ? 'Claro' : 'Escuro'}
            </button>
          </div>
          <nav className="space-y-2">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  clsx(
                    'block rounded-md px-3 py-2 text-sm font-medium',
                    isActive
                      ? 'bg-emerald-600 text-white'
                      : 'text-slate-700 hover:bg-slate-200 dark:text-slate-200 dark:hover:bg-slate-800'
                  )
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <main className="flex-1 p-8">
          <div className="mx-auto max-w-6xl space-y-6">{children}</div>
        </main>
      </div>
    </div>
  );
}
