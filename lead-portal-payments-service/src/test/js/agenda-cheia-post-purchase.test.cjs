const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/agenda-cheia/obrigado.js'), 'utf8');
const tick = () => new Promise(resolve => setImmediate(resolve));
function setup(fetch, search = '?payment_id=synthetic-payment') {
  const classes = new Set(['hidden']);
  const button = { disabled: false, textContent: '' };
  const form = { classList: { add: v => classes.add(v), remove: v => classes.delete(v) },
    querySelector: () => button, setAttribute() {}, addEventListener(_, callback) { this.submit = callback; } };
  const message = { textContent: '' }, status = { textContent: 'Agenda Cheia Nail Design' };
  const nodes = { '#briefing': form, '#message': message, '#payment-status': status,
    '#kit-title': { textContent: '' }, '#kit-intro': { textContent: '' } };
  vm.runInNewContext(script, { URLSearchParams, location: { search }, fetch,
    document: { querySelector: selector => nodes[selector] },
    FormData: class { *[Symbol.iterator]() { yield ['buyerEmail', 'teste+policy@sandbox.local']; } } });
  return { form, message, status, button, classes };
}
const response = status => ({ ok: true, json: async () => ({ status }) });
for (const state of ['AGUARDANDO_BRIEFING', 'BRIEFING_RECEBIDO', 'ENTREGUE']) {
  test(`consulta respeita o estado oficial ${state}`, async () => {
    const ui = setup(async () => response(state)); await tick();
    assert.equal(ui.classes.has('hidden'), state !== 'AGUARDANDO_BRIEFING');
    assert.match(ui.status.textContent, /confirmado/);
    if (state === 'ENTREGUE') assert.match(ui.message.textContent, /foi entregue/);
  });
}
test('ausência ou recusa de pagamento nunca apresenta aprovação', async () => {
  for (const search of ['', '?payment_id=invalid']) {
    const ui = setup(async () => ({ ok: false }), search); await tick();
    assert.equal(ui.status.textContent, 'Agenda Cheia Nail Design');
    assert.ok(ui.classes.has('hidden'));
  }
});
test('estado desconhecido não libera briefing nem inventa sucesso', async () => {
  const ui = setup(async () => response('UNKNOWN')); await tick();
  assert.ok(ui.classes.has('hidden')); assert.doesNotMatch(ui.status.textContent, /confirmado/);
});
test('envio aguarda, impede duplicação e mostra entrega concluída', async () => {
  let release, posts = 0;
  const ui = setup(async (_, options) => {
    if (!options) return response('AGUARDANDO_BRIEFING');
    posts++; return new Promise(resolve => { release = resolve; });
  }); await tick();
  const sending = ui.form.submit({ preventDefault() {} });
  await ui.form.submit({ preventDefault() {} });
  assert.equal(posts, 1); assert.equal(ui.button.disabled, true);
  release(response('ENTREGUE')); await sending;
  assert.match(ui.message.textContent, /foi entregue/); assert.ok(ui.classes.has('hidden'));
  assert.equal(ui.button.disabled, false);
});
test('falha de rede ou HTTP mantém dados e não anuncia entrega', async () => {
  for (const failure of ['network', 'http']) {
    const ui = setup(async (_, options) => {
      if (!options) return response('AGUARDANDO_BRIEFING');
      if (failure === 'network') throw new Error('offline');
      return { ok: false };
    }); await tick();
    await ui.form.submit({ preventDefault() {} });
    assert.equal(ui.classes.has('hidden'), false); assert.equal(ui.button.disabled, false);
    assert.match(ui.message.textContent, /dados continuam/); assert.doesNotMatch(ui.message.textContent, /foi entregue/);
  }
});
