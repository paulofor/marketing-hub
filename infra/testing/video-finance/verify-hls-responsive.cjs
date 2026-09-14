// Executa HLS real por HTTP, com segmentos gerados pelo uploader versionado e dados sintéticos.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const http = require('node:http');
const { chromium, devices } = require('@playwright/test');

(async () => {
  const output = path.resolve(process.argv[2]);
  const requests = [];
  let mediaOrigin;
  let applicationOrigin;
  const handler = async (req, res) => {
    if (req.headers.origin === applicationOrigin) {
      res.setHeader('Access-Control-Allow-Origin', applicationOrigin);
      res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD');
      res.setHeader('Vary', 'Origin');
    }
    try {
      if (req.url === '/') {
        res.setHeader('Content-Type', 'text/html');
        res.end(`<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0;text-align:center}video{max-width:100%;height:85vh}</style><video controls playsinline></video><script src="/hls.js"></script><script>const v=document.querySelector("video");const h=new Hls();h.loadSource("${mediaOrigin}/playback.m3u8");h.attachMedia(v);h.on(Hls.Events.ERROR,(_,e)=>{if(e.fatal)window.hlsError=e.details});</script>`);
        return;
      }
      const name = path.basename(req.url);
      const file = name === 'hls.js' ? path.resolve('frontend/node_modules/hls.js/dist/hls.min.js') : path.join(output, 'uploaded', name);
      const bytes = await fs.readFile(file);
      res.setHeader('Content-Type', name.endsWith('.m3u8') ? 'application/vnd.apple.mpegurl' : name.endsWith('.ts') ? 'video/mp2t' : 'text/javascript');
      requests.push(name); res.end(bytes);
    } catch (error) { res.statusCode = 404; res.end('Fixture inexistente'); }
  };
  const server = http.createServer(handler);
  const mediaServer = http.createServer(handler);
  await new Promise(resolve => mediaServer.listen(0, '127.0.0.1', resolve));
  mediaOrigin = `http://127.0.0.1:${mediaServer.address().port}`;
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  applicationOrigin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ executablePath:'/usr/bin/chromium', args:['--no-sandbox'] });
  const results=[];
  try {
    for (const [device, options] of [['desktop',{viewport:{width:1440,height:1000}}],['iphone',devices['iPhone 15 Pro']],['pixel',devices['Pixel 7']]]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      await page.goto(`http://127.0.0.1:${server.address().port}`);
      await page.waitForFunction(() => document.querySelector('video').readyState >= 2, null, {timeout:30000});
      const result = await page.evaluate(async () => {
        const v=document.querySelector('video');v.muted=true;await v.play();await new Promise(r=>setTimeout(r,250));
        const advanced=v.currentTime>0;v.pause();v.currentTime=6;
        await new Promise(r=>v.addEventListener('seeked',r,{once:true}));
        return {advanced,duration:v.duration,width:v.videoWidth,height:v.videoHeight,error:window.hlsError??v.error?.code??null,fits:v.getBoundingClientRect().right<=innerWidth};
      });
      assert.equal(result.advanced,true);assert.equal(result.error,null);assert.equal(result.fits,true);
      assert.equal(result.width,1080);assert.equal(result.height,1920);assert.ok(Math.abs(result.duration-15)<.3);
      await page.screenshot({path:path.join(output,`hls-${device}.png`)});results.push({device,...result});await context.close();
    }
    assert.ok(requests.includes('playback.m3u8'));assert.ok(requests.filter(n=>n.endsWith('.ts')).length>=3);
    await fs.writeFile(path.join(output,'hls-browser-results.json'),JSON.stringify({results,requests,applicationOrigin,mediaOrigin,crossOrigin:true},null,2));
    console.log(JSON.stringify(results));
  } finally {await browser.close();await new Promise(resolve=>server.close(resolve));await new Promise(resolve=>mediaServer.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1});
