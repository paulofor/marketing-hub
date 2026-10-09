import test from 'node:test';
import assert from 'node:assert/strict';
import { validateFirstKitActions } from '../../main/resources/browser/private-kit-first-actions-contract.mjs';

// Monta posições observadas sem depender de HTML, texto comercial, produto ou versão específicos.
function firstScreen(width = 1440, height = 900) {
  return { viewport: { width, height }, testControlsClosed: true,
    actions: [
      { id: 'copy-caption', label: 'Copiar legenda', x: 24, y: 460, width: 280, height: 48 },
      { id: 'download', label: 'Baixar pacote', x: 24, y: 520, width: 280, height: 48 },
    ] };
}

test('aceita ações inteiras em desktop e celular', () => {
  for (const screen of [firstScreen(), firstScreen(393, 852), firstScreen(412, 915)]) {
    assert.doesNotThrow(() => validateFirstKitActions(screen));
  }
});

test('recusa a fricção original de copiar e baixar depois das prévias altas', () => {
  const screen = firstScreen();
  screen.actions[0].y = 1200;
  screen.actions[1].y = 2000;
  assert.throws(() => validateFirstKitActions(screen), /copy-caption.*primeira tela/);
});

test('recusa controle parcial, escondido, desabilitado ou sem nome em outro viewport', () => {
  for (const change of [{ y: 830 }, { x: 300 }, { hidden: true }, { disabled: true }, { label: '' }]) {
    const screen = firstScreen(393, 852);
    Object.assign(screen.actions[1], change);
    assert.throws(() => validateFirstKitActions(screen), /download.*primeira tela/);
  }
});

test('exige ações completas e controles internos recolhidos na entrada', () => {
  const missing = firstScreen();
  missing.actions.pop();
  assert.throws(() => validateFirstKitActions(missing), /download/);
  const expanded = firstScreen();
  expanded.testControlsClosed = false;
  assert.throws(() => validateFirstKitActions(expanded), /controles de teste/);
});
