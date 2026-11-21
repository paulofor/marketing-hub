const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const definition = JSON.parse(fs.readFileSync(path.join(root, 'config/product-types/ai-designed-manufactured-product-v1.json'), 'utf8'));

test('Jaspe request fits the existing catalog DTO without a migration or product creation', () => {
  assert.deepEqual(Object.keys(definition).sort(), ['aliases', 'blueprint', 'code', 'description', 'internalName', 'name', 'status']);
  for (const [field, max] of Object.entries({ code: 64, name: 191, internalName: 191, description: 1000 })) {
    assert.equal(typeof definition[field], 'string');
    assert.ok(definition[field].trim().length > 0 && definition[field].length <= max, field);
  }
  assert.match(definition.code, /^[A-Z][A-Z_]+$/);
  assert.ok(definition.aliases.length <= 20);
  for (const alias of definition.aliases) assert.ok(alias.length > 0 && alias.length <= 191);
  assert.equal(definition.status, 'PROPOSED', 'No production rollout is implied by the type definition');
});

test('construction blueprint uses existing backend fields and their length limits', () => {
  const limits = { version:64, primaryChannel:64, customerJob:5000, valueMechanism:5000,
    experienceFlow:5000, requiredInputs:5000, expectedOutputs:5000, memoryStrategy:5000,
    integrationRequirements:5000, safetyGuardrails:5000, successMetrics:5000,
    backendSdkModule:255, frontendSdkModule:255 };
  assert.deepEqual(Object.keys(definition.blueprint).sort(), Object.keys(limits).sort());
  for (const [field,max] of Object.entries(limits)) {
    assert.equal(typeof definition.blueprint[field], 'string');
    assert.ok(definition.blueprint[field].length <= max, field);
    if (field !== 'frontendSdkModule') assert.ok(definition.blueprint[field].trim(), field);
  }
  assert.equal(definition.blueprint.primaryChannel, 'WEB');
});

test('comparison scores and example contribution are arithmetically consistent', () => {
  const research = fs.readFileSync(path.join(root, 'docs/pesquisas/ia-concepcao-fabricacao-2026-11-21.md'), 'utf8');
  const rows = [...research.matchAll(/\| (\d)\/(\d)\/(\d)\/(\d)\/(\d) \| \*{0,2}(\d+)\*{0,2} \|/g)];
  assert.equal(rows.length,12);
  for (const row of rows) assert.equal([6,5,4,3,2].reduce((s,w,i)=>s+w*Number(row[i+1]),0), Number(row[6]));
  assert.equal(1490 - [450,90,180,60,90,60,180].reduce((a,b)=>a+b),380);
  assert.equal(1290 - [500,140,180,80,120,100,300].reduce((a,b)=>a+b),-130);
  assert.equal(1990 - [500,90,240,60,90,60,180].reduce((a,b)=>a+b),770);
});
