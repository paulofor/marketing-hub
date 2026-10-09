/* Cliente da prova privada: apenas apresenta estado e reporta ações; o backend controla a fila. */
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const base = location.pathname.replace(/\/prototype$/, '');
  let secret = sessionStorage.getItem('private-kit-session-v1') || '';
  let current, timer, displaying = '', imageUrls = [];
  const error = message => { $('error').textContent = message; $('error').hidden = !message; $('reload').hidden = !message || current?.status !== 'READY'; };
  async function api(path, method = 'GET', body, binary = false) {
    const response = await fetch(base + path, { method, headers: { 'X-Kit-Session': secret, ...(body ? { 'Content-Type': 'application/json' } : {}) }, body: body ? JSON.stringify(body) : undefined, cache: 'no-store', credentials: 'omit' });
    if (!response.ok) {
      let detail;
      try { detail = await response.json(); } catch { detail = {}; }
      throw new Error(detail.detail || detail.message || (response.status === 401 ? 'Acesso inválido, expirado ou revogado. Volte ao ciclo para conferir seu acesso.' : 'A ação não foi aceita. Confira a entrada e tente novamente.'));
    }
    return binary ? response.blob() : response.status === 204 ? null : response.json();
  }
  async function event(code) { current = await api('/events', 'POST', { eventId: crypto.randomUUID(), code }); audit(); }
  function audit() {
    $('audit').textContent = JSON.stringify({ productId: current.productId, cycleId: current.cycleId, experimentId: current.experimentId, prototypeVersion: current.prototypeVersion, sessionId: current.id, state: current.status, transfers: current.transfers, events: current.events.map(e => e.code), providerCalls: 0, commercialEvidenceEligible: false }, null, 2);
  }
  async function showResult() {
    if (displaying === current.id) return;
    displaying = current.id;
    for (const url of imageUrls) URL.revokeObjectURL(url);
    imageUrls = [];
    const first = current.firstApplication;
    $('caption').value = first.caption;
    $('message').value = first.message;
    $('calendar').textContent = first.calendar;
    $('files').replaceChildren(...current.manifest.files.map(file => { const li = document.createElement('li'); li.textContent = file.name + ' · ' + file.bytes + ' bytes'; return li; }));
    for (const [id, name] of [['post', first.post], ['story', first.story]]) {
      const blob = await api('/assets/' + name, 'GET', undefined, true);
      const url = URL.createObjectURL(blob); imageUrls.push(url); $(id).src = url;
    }
    await Promise.all(['post', 'story'].map(id => $(id).decode()));
    if (!current.events.some(e => e.code === 'VALUE_MOMENT')) await event('VALUE_MOMENT');
  }
  async function render() {
    clearTimeout(timer);
    error('');
    $('identity').textContent = current.productName + ' · produto #' + current.productId + ' · ciclo #' + current.cycleId + ' · ' + current.prototypeVersion;
    $('status').textContent = current.reason;
    const presentation = current.presentation;
    $('title').textContent = presentation.title;
    $('introduction').textContent = presentation.introduction;
    $('blocked').hidden = !presentation.nextActionPath;
    $('next-step').textContent = presentation.nextStep;
    $('review-cycle').textContent = presentation.nextActionLabel;
    if (presentation.nextActionPath) $('review-cycle').href = new URL(presentation.nextActionPath, 'http://191.252.181.168:5173').href;
    else $('review-cycle').removeAttribute('href');
    $('briefing').hidden = current.status !== 'INPUT';
    $('result').hidden = true;
    $('resume').hidden = true;
    $('prepare').disabled = current.status !== 'INPUT';
    audit();
    if (current.status === 'READY') {
      try { await showResult(); $('result').hidden = false; } catch (ex) { displaying = ''; throw ex; }
    }
    if (['QUEUED', 'RUNNING'].includes(current.status)) timer = setTimeout(load, 1000);
  }
  async function load() {
    try { current = await api('/session'); await render(); } catch (ex) { clearTimeout(timer); error(ex.message); }
  }
  async function action(button, operation) {
    button.disabled = true; error('');
    try { await operation(); } catch (ex) { error(ex.message); } finally { button.disabled = false; }
  }
  $('reload').onclick = () => action($('reload'), load);
  $('form').addEventListener('submit', async e => {
    e.preventDefault();
    await action($('prepare'), async () => {
      const form = new FormData(e.target);
      const input = Object.fromEntries(['professionalName','email','cityRegion','whatsapp','services','visualStyle','weeklyGoal','preferredColors','notes'].map(key => [key, String(form.get(key) || '').trim()]));
      input.consentAccepted = form.has('consentAccepted');
      current = await api('/input', 'PUT', input); await render();
    });
  });
  for (const [id, field] of [['copy-caption','caption'],['copy-message','message']]) $(id).onclick = () => action($(id), async () => {
    await navigator.clipboard.writeText($(field).value);
    await event('READY_RESULT_USED');
    $('download-status').textContent = 'Texto copiado. Você pode usar esta primeira aplicação.';
  });
  $('download').onclick = () => action($('download'), async () => {
    const blob = await api('/download', 'GET', undefined, true);
    const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = 'kit-privado.zip'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    await event('DOWNLOAD_COMPLETED');
    $('download-status').textContent = 'Pacote íntegro recebido para salvar. Nenhuma compra ou envio foi criado.';
  });
  $('prefer').onclick = () => action($('prefer'), async () => { await event('PREFERRED_OVER_FREE'); $('checkout-status').textContent = 'Preferência simulada registrada como teste técnico, sem prova de demanda.'; });
  $('checkout').onclick = () => action($('checkout'), async () => { await event('CHECKOUT_STARTED'); $('checkout-status').textContent = 'Continuidade simulada. Não há cobrança, link de pagamento nem autorização comercial nesta prova.'; });
  $('exit').onclick = () => action($('exit'), async () => { await event('SESSION_EXITED'); $('result').hidden = true; $('resume').hidden = false; $('status').textContent = 'Sua entrada e seu pacote foram preservados.'; });
  $('return').onclick = () => action($('return'), async () => { await event('SESSION_RETURNED'); await load(); });
  window.addEventListener('message', async e => {
    const allowed = e.origin === 'http://191.252.181.168:5173' || (['localhost','127.0.0.1'].includes(location.hostname) && /^http:\/\/(localhost|127\.0\.0\.1):[0-9]+$/.test(e.origin));
    if (!allowed || e.source !== window.opener || e.data?.type !== 'PDE_PRIVATE_KIT_ACCESS_V1' || typeof e.data.sessionToken !== 'string' || !/^[a-f0-9]{64}$/.test(e.data.sessionToken)) return;
    secret = e.data.sessionToken;
    sessionStorage.setItem('private-kit-session-v1', secret);
    displaying = '';
    await load();
  });
  if (window.opener) window.opener.postMessage({ type: 'PDE_PRIVATE_KIT_READY_V1' }, '*');
  if (secret) load();
})();
