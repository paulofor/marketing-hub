const { chromium, devices } = require('playwright');
const fs = require('fs');
const assert = require('node:assert/strict');
const output = process.argv[2];
const base = process.env.VEGA_ACCESS_TEST_URL || 'http://127.0.0.1:18083';
const host = new URL(base).hostname;
if (!['127.0.0.1', 'sandbox-docker'].includes(host) &&
    !(host === process.env.VEGA_TEST_DOCKER_HOST && /^172\.(1[6-9]|2\d|3[01])\./.test(host))) throw Error('Somente sandbox local');
const api = 'http://127.0.0.1:18080/api/pde/vega/private/v1';
async function internal(path, body, method) {
  const response = await fetch(api + path, {method: method || 'POST',
    headers: {'Content-Type': 'application/json', 'X-PDE-Internal-Token': 'vega-local-internal-only'},
    body: body === undefined ? undefined : JSON.stringify(body)});
  assert.ok(response.ok, `${path}: HTTP ${response.status}`);
  const text = await response.text(); return text ? JSON.parse(text) : null;
}
(async () => {
  fs.mkdirSync(output, {recursive:true});
  const browser = await chromium.launch({executablePath: '/usr/bin/chromium'});
  try {
    for (const [name, device] of Object.entries({desktop: {viewport:{width:1440,height:900}}, iphone: devices['iPhone 15 Pro'], pixel: devices['Pixel 7']})) {
      const context = await browser.newContext({...device, defaultBrowserType:'chromium'});
      const page = await context.newPage(); const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      const anonymousSessionRequests = [];
      page.on('request', request => {
        if (request.url().endsWith('/private/v1/session') &&
            !request.headers()['x-vega-session']) anonymousSessionRequests.push(request.url());
      });
      page.on('response', response => {
        if (response.status() >= 500) errors.push(`HTTP ${response.status()} ${new URL(response.url()).pathname}`);
      });
      const session = await internal('/internal/sessions', {cycleId:91002, prototypeVersion:'musa-pde-entry-v11-primeiro-ajuste-aplicavel', origin:'HUMAN', readingNumber:2});
      try {
        await page.goto(base + '/vega-private/');
        const field = page.getByLabel('Seu convite privado');
        await field.waitFor();
        assert.equal(await field.getAttribute('type'), 'password');
        assert.equal(await page.getByRole('button',{name:'Continuar com meu convite'}).isDisabled(), true);
        await field.fill('https://outra-origem.invalid/vega-private/#access=invalid');
        await page.getByRole('button',{name:'Continuar com meu convite'}).click();
        await page.getByRole('alert').filter({hasText:'Não reconhecemos'}).waitFor();
        await field.fill(base+'/vega-private/#access=invalid');
        await page.getByRole('button',{name:'Continuar com meu convite'}).click();
        const access = page.getByRole('button',{name:'Acessar meu ajuste'});
        assert.equal(await access.isDisabled(),true);
        await page.getByRole('checkbox').check(); await access.click();
        await page.getByRole('alert').filter({hasText:'Acesso inválido'}).waitFor();
        await page.getByRole('button',{name:'Usar outro convite'}).click();
        await field.fill(base+'/vega-private/#access='+session.accessToken);
        await page.getByRole('button',{name:'Continuar com meu convite'}).click();
        assert.equal(await page.getByRole('checkbox').isChecked(),false);
        await page.getByRole('checkbox').check(); await access.click();
        await page.getByRole('button',{name:'Começar',exact:true}).waitFor();
        assert.ok(!page.url().includes('#'));
        assert.ok(!(await page.locator('body').innerText()).includes(session.accessToken));
        await page.getByRole('button',{name:'Começar',exact:true}).click();
        await page.getByLabel('Ocasião').waitFor();
        await page.reload(); await page.getByLabel('Ocasião').waitFor();
        await page.screenshot({path:`${output}/${name}-recovered.png`,fullPage:true});
        // A revogação é uma falha real de acesso e não pode apagar a possibilidade de recuperação.
        await internal('/internal/sessions/'+session.id,undefined,'DELETE');
        await page.reload(); await page.getByLabel('Seu convite privado').waitFor();
        await page.getByRole('alert').waitFor();
        assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
        await page.screenshot({path:`${output}/${name}-expired.png`,fullPage:true});
        assert.deepEqual(errors,[]);
        assert.deepEqual(anonymousSessionRequests,[], 'Sem sessão, a entrada não consulta o endpoint protegido');
        console.log('PASS',name,'convite, consentimento, isolamento, acesso, retomada e revogação');
      } finally {
        await internal('/internal/sessions/'+session.id,undefined,'DELETE');
        await context.close();
      }
    }
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
