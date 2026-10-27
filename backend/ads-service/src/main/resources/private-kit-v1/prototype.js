/* Cliente da prova privada: apenas apresenta estado e reporta ações; o backend controla a fila. */
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const base = location.pathname.replace(/\/prototype$/, '');
  class KitUiError extends Error {}
  let secret = sessionStorage.getItem('private-kit-session-v1') || '';
  let current, timer, displaying = '', imageUrls = [];
  const error = message => { $('error').textContent = message; $('error').hidden = !message; $('reload').hidden = !message || current?.status !== 'READY'; };
  async function api(path, method = 'GET', body, binary = false) {
    const response = await fetch(base + path, { method, headers: { 'X-Kit-Session': secret, ...(body ? { 'Content-Type': 'application/json' } : {}) }, body: body ? JSON.stringify(body) : undefined, cache: 'no-store', credentials: 'omit' });
    if (!response.ok) {
      throw new KitUiError(response.status === 401 ? 'Seu acesso não está disponível. Abra novamente a demonstração pelo painel.' : 'Não foi possível concluir esta ação. Tente novamente.');
    }
    return binary ? response.blob() : response.status === 204 ? null : response.json();
  }
  async function event(code) { current = await api('/events', 'POST', { eventId: crypto.randomUUID(), code }); }
  // Consome apenas o recibo privado da sessão; leitura e reprodução não aprovam peças ou comércio.
  function renderVideo() {
    const binding = current.videoIntegration;
    const hero = binding?.heroVideo;
    const matches = binding?.evidenceType === 'LEARNING_CYCLE_VIDEO_INTEGRATION_V1'
      && /^[a-f0-9]{64}$/.test(binding.integrationFingerprint)
      && binding.productId === current.productId && binding.cycleId === current.cycleId
      && binding.experimentId === current.experimentId && binding.productVersion === current.prototypeVersion
      && hero?.role === 'LANDING_HERO' && hero.reviewStatus === 'APPROVED';
    $('video-demo').hidden = !matches;
    const video = $('kit-video');
    if (!matches) {
      if (video.hasAttribute('src')) { video.pause(); video.removeAttribute('src'); video.load(); }
      delete video.dataset.fingerprint; $('video-demo').open = false; return;
    }
    if (video.dataset.fingerprint === binding.integrationFingerprint) return;
    let source;
    try { source = new URL(hero.assetUrl); } catch { $('video-demo').hidden = true; return; }
    if (source.protocol !== 'https:' || source.username || source.password) { $('video-demo').hidden = true; return; }
    video.dataset.fingerprint = binding.integrationFingerprint;
    video.src = source.href;
    $('video-captions').textContent = hero.captions;
    $('video-error').hidden = true;
    video.onerror = () => { $('video-error').hidden = false; };
  }
  // A ampliação é exclusiva da nova versão; não altera arquivos nem dispara composição.
  for (const id of ['post', 'story']) $('enlarge-' + id).onclick = () => {
    $('enlarged-art').src = $(id).src;
    $('enlarged-art').alt = $(id).alt + ' ampliado';
    $('art-dialog').showModal();
  };
  $('close-art').onclick = () => $('art-dialog').close();
  async function showResult() {
    if (displaying === current.id) return;
    displaying = current.id;
    for (const url of imageUrls) URL.revokeObjectURL(url);
    imageUrls = [];
    const first = current.firstApplication;
    const references = current.profileCode === 'beauty-v2' || current.profileCode?.endsWith('-v2');
    for (const id of ['post', 'story']) $('enlarge-' + id).hidden = !references;
    $('caption').value = first.caption;
    $('message').value = first.message;
    const fileLabels = { posts: 'Post', stories: 'Story', legendas: 'Legenda', mensagens: 'Mensagem' };
    const friendlyName = path => {
      const [category, name] = path.split('/');
      return category === 'calendario' ? 'Calendário de 7 dias' : (fileLabels[category] || 'Arquivo do kit') + ' ' + Number(name?.match(/\d+/)?.[0] || 1);
    };
    $('calendar').textContent = current.manifest.files.reduce((text, file) => text.replaceAll(file.name, friendlyName(file.name)), first.calendar);
    $('files').replaceChildren(...current.manifest.files.map(file => {
      const li = document.createElement('li');
      li.textContent = friendlyName(file.name);
      return li;
    }));
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
    $('identity').textContent = current.productName;
    const presentation = current.presentation;
    $('status').textContent = presentation.statusMessage || '';
    $('title').textContent = presentation.title;
    $('introduction').textContent = presentation.introduction;
    $('introduction').hidden = current.status === 'READY';
    $('blocked').hidden = !presentation.nextActionPath;
    $('next-step').textContent = presentation.nextStep;
    $('review-cycle').textContent = presentation.nextActionLabel;
    if (presentation.nextActionPath) $('review-cycle').href = new URL(presentation.nextActionPath, 'http://191.252.181.168:5173').href;
    else $('review-cycle').removeAttribute('href');
    $('briefing').hidden = current.status !== 'INPUT';
    const options = current.specialties || [];
    const multiple = options.length > 1;
    $('specialty-field').hidden = !multiple;
    $('specialty').required = multiple;
    if (!$('specialty').options.length && multiple) {
      $('specialty').replaceChildren(...options.map(option => {
        const element = document.createElement('option');
        element.value = option.code; element.textContent = option.label; return element;
      }));
      $('specialty').onchange = () => {
        const selected = options.find(option => option.code === $('specialty').value);
        if (selected) {
          document.querySelector('[name=services]').value = selected.serviceExample;
          $('specialty-sample').hidden = false;
          $('sample-headline').textContent = selected.sampleHeadline;
          $('sample-caption').textContent = selected.sampleCaption;
        }
      };
      $('specialty').onchange();
    }
    $('result').hidden = true;
    $('resume').hidden = true;
    $('prepare').disabled = current.status !== 'INPUT';
    renderVideo();
    if (current.status === 'READY') {
      try { await showResult(); $('result').hidden = false; window.scrollTo(0, 0); } catch (ex) { displaying = ''; throw ex; }
    }
    if (['QUEUED', 'RUNNING'].includes(current.status)) timer = setTimeout(load, 1000);
  }
  async function load() {
    try { current = await api('/session'); await render(); } catch (ex) { clearTimeout(timer); error(ex instanceof KitUiError ? ex.message : 'Não foi possível abrir seu kit agora. Tente novamente.'); }
  }
  async function action(button, operation) {
    button.disabled = true; error('');
    try { await operation(); } catch (ex) { error(ex instanceof KitUiError ? ex.message : 'Não foi possível concluir esta ação. Tente novamente.'); } finally { button.disabled = false; }
  }
  $('reload').onclick = () => action($('reload'), load);
  $('form').addEventListener('submit', async e => {
    e.preventDefault();
    await action($('prepare'), async () => {
      const form = new FormData(e.target);
      const input = Object.fromEntries(['professionalName','email','cityRegion','whatsapp','services','visualStyle','weeklyGoal','preferredColors','notes'].map(key => [key, String(form.get(key) || '').trim()]));
      input.consentAccepted = form.has('consentAccepted');
      if (current.specialties?.length > 1) input.specialtyCode = String(form.get('specialtyCode') || '');
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
    const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = 'kit-de-divulgacao.zip'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    await event('DOWNLOAD_COMPLETED');
    $('download-status').textContent = 'Kit recebido. Guarde seus arquivos para usar quando quiser.';
  });
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
