import React, { useMemo, useRef, useState } from 'react';

type Session = {
  sessionId: string;
  variant: 'A' | 'B';
  durationSeconds: number;
  steps: string[];
  exitInstruction: string;
};

const participantId = crypto.randomUUID();

/** Renderiza o experimento consentido da Pausa de Transição sem oferta ou tráfego pago. */
export function TransitionPauseExperiment() {
  const [task, setTask] = useState('');
  const [effortBefore, setEffortBefore] = useState(5);
  const [effortAfter, setEffortAfter] = useState(5);
  const [consent, setConsent] = useState(false);
  const [safety, setSafety] = useState(false);
  const [voluntary, setVoluntary] = useState(false);
  const [session, setSession] = useState<Session | null>(null);
  const [status, setStatus] = useState<'idle' | 'starting' | 'running' | 'done' | 'stopped'>('idle');
  const [secondsLeft, setSecondsLeft] = useState(0);
  const sessionId = useMemo(() => crypto.randomUUID(), []);
  const sessionStartedAt = useRef<number | null>(null);

  async function start() {
    setStatus('starting');
    const response = await fetch('/api/pde/transition-pause/v1/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ participantId, sessionId, taskDescription: task, consentAccepted: consent, safetyAcknowledged: safety, voluntaryParticipation: voluntary }),
    });
    if (!response.ok) {
      setStatus('idle');
      return;
    }
    const started = await response.json() as Session;
    setSession(started);
    sessionStartedAt.current = Date.now();
    setSecondsLeft(started.durationSeconds);
    setStatus('running');
    const timer = window.setInterval(() => {
      setSecondsLeft((current) => {
        if (current <= 1) {
          window.clearInterval(timer);
          return 0;
        }
        return current - 1;
      });
    }, 1000);
  }

  async function record(eventType: string, firstStepCompleted = false) {
    await fetch('/api/pde/transition-pause/v1/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ participantId, sessionId, eventType, effortBefore, effortAfter, secondsUntilTaskStarted: eventType === 'TASK_STARTED' && sessionStartedAt.current ? Math.min(600, Math.round((Date.now() - sessionStartedAt.current) / 1000)) : undefined, firstStepCompleted }),
    });
    setStatus(eventType === 'SAFETY_STOPPED' ? 'stopped' : 'done');
  }

  if (session) {
    return <main className="transition-pause-shell">
      <p className="transition-kicker">Experimento supervisionado · variante {session.variant}</p>
      <h1>{status === 'stopped' ? 'Experiência encerrada' : 'Pausa de Transição'}</h1>
      {status === 'running' && <>
        <p className="transition-timer" aria-live="polite">{Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, '0')}</p>
        <ol>{session.steps.map((step) => <li key={step}>{step}</li>)}</ol>
        <p className="transition-safety">{session.exitInstruction}</p>
        <button className="secondary-button" onClick={() => void record('SAFETY_STOPPED')}>Parar agora</button>
        <button className="primary-button" onClick={() => void record('EXPERIENCE_COMPLETED')}>Concluir a pausa</button>
      </>}
      {status === 'done' && <section>
        <h2>O que aconteceu depois?</h2>
        <label>Esforço percebido agora: {effortAfter}/10<input type="range" min="0" max="10" value={effortAfter} onChange={(event) => setEffortAfter(Number(event.target.value))} /></label>
        <button className="primary-button" onClick={() => void record('TASK_STARTED')}>Comecei a tarefa</button>
        <button className="secondary-button" onClick={() => void record('FIRST_STEP_COMPLETED', true)}>Concluí o primeiro passo</button>
      </section>}
      {status === 'stopped' && <p>Você fez certo em parar. Não continue se houver ansiedade, tontura, dissociação ou qualquer desconforto.</p>}
    </main>;
  }

  return <main className="transition-pause-shell">
    <p className="transition-kicker">Validação voluntária · sem oferta comercial</p>
    <h1>Deixe o primeiro passo parecer um pouco mais leve</h1>
    <p>Esta é uma experiência breve para tarefas cotidianas. Não é hipnose, terapia ou tratamento e não garante resultado.</p>
    <label>Qual tarefa cotidiana você quer começar?<textarea maxLength={300} value={task} onChange={(event) => setTask(event.target.value)} /></label>
    <label>Quanto esforço ela parece exigir agora? {effortBefore}/10<input type="range" min="0" max="10" value={effortBefore} onChange={(event) => setEffortBefore(Number(event.target.value))} /></label>
    <label><input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} /> Aceito participar e registrar minhas respostas para avaliar esta experiência.</label>
    <label><input type="checkbox" checked={safety} onChange={(event) => setSafety(event.target.checked)} /> Estou em local seguro, sem dirigir ou operar máquinas, e vou parar se sentir desconforto.</label>
    <label><input type="checkbox" checked={voluntary} onChange={(event) => setVoluntary(event.target.checked)} /> Sei que participar é voluntário e posso sair a qualquer momento.</label>
    <button className="primary-button" disabled={!task.trim() || !consent || !safety || !voluntary || status === 'starting'} onClick={() => void start()}>{status === 'starting' ? 'Preparando…' : 'Começar experiência'}</button>
  </main>;
}
