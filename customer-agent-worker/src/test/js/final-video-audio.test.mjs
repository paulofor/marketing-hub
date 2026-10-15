import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import path from "node:path";
import os from "node:os";
import { createRequire } from "node:module";
import { extractAudio } from "../../main/resources/browser/final-video-review.mjs";
import { videoBrowserOptions } from "../../main/resources/browser/learning-cycle-video-checks.mjs";
const { chromium, devices }=createRequire(import.meta.url)("playwright-core");

// Usa WAV PCM de teste para validar o decodificador real sem inferência ou envio externo.
function fixture(silent=false) {
  const rate=24000, count=rate*6, bytes=Buffer.alloc(44+count*2);
  bytes.write('RIFF',0);bytes.writeUInt32LE(bytes.length-8,4);bytes.write('WAVEfmt ',8);
  bytes.writeUInt32LE(16,16);bytes.writeUInt16LE(1,20);bytes.writeUInt16LE(1,22);
  bytes.writeUInt32LE(rate,24);bytes.writeUInt32LE(rate*2,28);bytes.writeUInt16LE(2,32);bytes.writeUInt16LE(16,34);
  bytes.write('data',36);bytes.writeUInt32LE(count*2,40);
  for(let i=0;i<count;i++)bytes.writeInt16LE(silent?0:Math.round(8000*Math.sin(2*Math.PI*220*i/rate)),44+i*2);
  return bytes;
}

test("extrai mix completo em desktop/iPhone/Pixel e recusa silêncio sem alegar qualidade perceptual", async()=>{
  const dir=await mkdtemp(path.join(os.tmpdir(),'psique-audio-fixture-'));
  const options=videoBrowserOptions();options.args.push('--disable-dev-shm-usage');
  const browser=await chromium.launch(options);
  try {
    for(const [index,options] of [{viewport:{width:1440,height:900}},devices['iPhone 15 Pro'],devices['Pixel 7']].entries()) {
      const context=await browser.newContext(options);const page=await context.newPage();await page.goto('about:blank');
      const media={assetId:9100+index,sha256:'a'.repeat(64),durationSeconds:6};
      const result=await extractAudio(page,fixture(),media,dir);
      assert.ok(Math.abs(result.durationSeconds-6)<.02);assert.equal(result.sampleRate,24000);assert.ok(result.peak>.2&&result.peak<.3);
      assert.equal((await readFile(result.localPath)).subarray(0,4).toString(),'RIFF');
      await assert.rejects(extractAudio(page,fixture(true),media,dir),/silencioso/);
      await assert.rejects(extractAudio(page,Buffer.from('invalid'),media,dir));
      await context.close();
    }
  } finally {await browser.close();await rm(dir,{recursive:true,force:true});}
});
