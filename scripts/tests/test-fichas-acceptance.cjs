const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const path = require('node:path');
const fs = require('node:fs');
const root = path.resolve(__dirname, '../..');
test('C1 fichas v1: contrato, API isolada, revisões e navegação homologadas sem agentes', () => {
  const commands = [
    ['schema', 'python3', ['scripts/tests/test-fichas-schema.py']],
    ['backend', 'mvn', ['-f','backend/ads-service/pom.xml','-Dtest=FichasTest','test']],
    ['interface', 'npm', ['--prefix','frontend','test','--','--run','src/pages/product/ProductFichasPage.test.tsx']],
    ['tipos', 'npm', ['--prefix','frontend','run','typecheck']],
    ['navegador', 'node', ['--test','--test-reporter=tap','scripts/tests/test-fichas-browser.cjs']],
  ];
  fs.mkdirSync(path.join(root,'.local/fichas-evidence'),{recursive:true});
  for(const [label,cmd,args] of commands) {
    const result=spawnSync(cmd,args,{cwd:root,encoding:'utf8',timeout:600000,maxBuffer:30*1024*1024,env:{...process.env,PYTHONPATH:path.join(root,'.local/fichas-python')}});
    fs.writeFileSync(path.join(root,`.local/fichas-evidence/${label}.log`),`${result.stdout || ''}\n${result.stderr || ''}`);
    assert.equal(result.status,0,`${label}: ${result.error || result.stderr || result.stdout}`);
  }
});
