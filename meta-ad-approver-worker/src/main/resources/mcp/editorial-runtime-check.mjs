import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { createHash } from 'node:crypto';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

const exec = promisify(execFile);
const sha = bytes => createHash('sha256').update(bytes).digest('hex');

// Homologa o MCP real com backend e mídias locais, sem modelo, provedor ou dados comerciais.
export async function validateEditorialInspection(realFixtureDirectory = null) {
  const directory = await mkdtemp(join(tmpdir(), 'editorial-contract-'));
  const videoPath = join(directory,'fixture.mp4'), pngPath = join(directory,'fixture.png');
  const options = { timeout: 120000, maxBuffer: 1024 * 1024 };
  await exec('/usr/local/bin/ffmpeg', ['-v','error','-f','lavfi','-i','color=c=blue:s=320x180:r=10:d=3',
    '-threads','1','-c:v','libx264','-pix_fmt','yuv420p','-y',videoPath], options);
  await exec('/usr/local/bin/ffmpeg', ['-v','error','-f','lavfi','-i','color=c=white:s=100x100',
    '-frames:v','1','-threads','1','-y',pngPath], options);
  let video = await readFile(videoPath), png = await readFile(pngPath);
  let context, base, requests = [];
  const files = new Map();
  const server = createServer((request,response) => {
    requests.push(request.url);
    if (request.url.startsWith('/api/internal/creatives/') && request.url.includes('/agent-review/context?')) {
      response.writeHead(200,{'content-type':'application/json'});response.end(JSON.stringify(context));return;
    }
    if (request.url === '/video.mp4') {
      response.writeHead(200,{'content-type':'video/mp4'});response.end(video);return;
    }
    let body = files.get(request.url);
    if (!body && ['/art-0.png','/art-1.png'].includes(request.url)) body = png;
    if (!body && context?.mediaGovernanceEvidence?.editorialMediaEvidence?.productProofSources?.some(source =>
      request.url === `/api/internal/creatives/${context.creativeId}/agent-review/product-proofs/${source.taskId}/${source.evidenceId}?experimentId=${context.experimentId}`)) body=png;
    if (!body) { response.writeHead(404); response.end(); return; }
    response.writeHead(200,{'content-type':'image/png'});response.end(body);
  });
  await new Promise(done=>server.listen(0,'127.0.0.1',done));
  base=`http://127.0.0.1:${server.address().port}`;

  // Exercita o protocolo stdio usado pelo Codex, incluindo bytes e metadados recebidos.
  async function inspect(id,experimentId,expectedError=false) {
    const transport = new StdioClientTransport({command:process.execPath,
      args:[fileURLToPath(new URL('./meta-ad-approver.mjs',import.meta.url))],
      env:{PATH:process.env.PATH??'/usr/local/bin:/usr/bin:/bin',
        MCP_MARKETING_HUB_URL:base,MCP_CREATIVE_ID:String(id),MCP_EXPERIMENT_ID:String(experimentId)}});
    const client=new Client({name:'editorial-local-contract',version:'1.0.0'});
    try {
      await client.connect(transport);
      const result=await client.callTool({name:'inspecionar_midia',arguments:{}},undefined,{timeout:180000});
      assert.equal(Boolean(result.isError),expectedError,JSON.stringify(result.content.filter(v=>v.type==='text')).slice(0,700));
      return result;
    } finally { await client.close(); }
  }
  const results=[];
  try {
    for (const [id,experimentId] of [[540,105],[9540,9105]]) {
      requests=[];context=fixture(base,id,experimentId,sha(video),sha(png));
      const result=await inspect(id,experimentId);
      const texts=result.content.filter(v=>v.type==='text').map(v=>JSON.parse(v.text));
      const audit=texts.find(v=>v.inspectionContract==='EDITORIAL_MEDIA_INSPECTION_V1');
      assert.equal(audit.narrationText,'Primeiro exemplo Segundo exemplo');
      assert.equal(audit.originalAudioReceipts.length,2);
      assert.equal(audit.listenedByThisTool,false);
      assert.equal(texts.filter(v=>v.sourceType==='PRODUCT_PROOF_SOURCE').length,2);
      assert.equal(texts.filter(v=>v.sourceType==='DELIVERY_ART_SOURCE').length,2);
      assert.equal(result.content.filter(v=>v.type==='image').length,9);
      assert.equal(texts.find(v=>v.mediaType==='VIDEO').sha256MatchesGovernance,true);
      assert.ok(texts.some(v=>v.finalFrameAtSeconds===0.5));
      assert.equal(requests.filter(v=>v.includes('/product-proofs/')).length,2);
      context.mediaGovernanceEvidence.editorialMediaEvidence.deliverySources[0].sha256='0'.repeat(64);
      await inspect(id,experimentId,true);
      context=fixture(base,id,experimentId,sha(video),sha(png));
      context.mediaGovernanceEvidence.editorialMediaEvidence.experimentId=experimentId+1;
      requests=[];await inspect(id,experimentId,true);
      assert.equal(requests.filter(v=>v.includes('/product-proofs/')).length,0);
      context=fixture(base,id,experimentId,sha(video),sha(png));
      delete context.mediaGovernanceEvidence.editorialMediaEvidence.audio.segments[0].originalReceipt;
      await inspect(id,experimentId,true);
      context=fixture(base,id,experimentId,sha(video),sha(png));
      context.mediaGovernanceEvidence.editorialMediaEvidence.productProofSources[0].crop=[0,0,1000,1000];
      await inspect(id,experimentId,true);
      context={format:'VIDEO',mediaUrl:base+'/video.mp4',mediaGovernanceEvidence:{
        contractVersion:'CREATIVE_MEDIA_GOVERNANCE_V4',finalArtifact:{sha256:sha(video)},
        approvedCreativeSources:[],approvedVideoSources:[]}};
      const legacy=await inspect(id,experimentId);
      assert.equal(legacy.content.filter(v=>v.type==='image').length,3);
      results.push({creativeId:id,experimentId,sourceImages:4,finalFrames:5,negativeCases:4,legacyFrames:3});
    }
    if (realFixtureDirectory) {
      const dir=resolve(realFixtureDirectory);
      const governance=JSON.parse(await readFile(join(dir,'governance.json'),'utf8'));
      video=await readFile(join(dir,'video.mp4'));
      assert.equal(sha(video),governance.finalArtifact.sha256);
      for (const source of governance.editorialMediaEvidence.productProofSources) {
        files.set(`/api/internal/creatives/540/agent-review/product-proofs/${source.taskId}/${source.evidenceId}?experimentId=105`,
          await readFile(join(dir,`proof-${source.taskId}-${source.evidenceId}.png`)));
      }
      for (const source of governance.editorialMediaEvidence.deliverySources) {
        files.set(`/art-${source.visualAssetId}.png`,await readFile(join(dir,`art-${source.visualAssetId}.png`)));
        source.url=`${base}/art-${source.visualAssetId}.png`;
      }
      context={format:'VIDEO',mediaUrl:base+'/video.mp4',mediaGovernanceEvidence:governance};
      const real=await inspect(540,105);
      const texts=real.content.filter(v=>v.type==='text').map(v=>JSON.parse(v.text));
      const audit=texts.find(v=>v.inspectionContract==='EDITORIAL_MEDIA_INSPECTION_V1');
      assert.equal(audit.originalAudioReceipts.length,7);
      assert.equal(texts.filter(v=>v.sourceType==='PRODUCT_PROOF_SOURCE').length,3);
      assert.equal(texts.filter(v=>v.sourceType==='DELIVERY_ART_SOURCE').length,3);
      assert.equal(texts.find(v=>v.mediaType==='VIDEO').sha256MatchesGovernance,true);
      results.push({actualCapturedFixture:true,sourceImages:6,voiceReceipts:7,
        finalFrames:texts.filter(v=>Number.isFinite(v.finalFrameAtSeconds)).length});
    }
    return {status:'PASS',cases:results};
  } finally {
    await new Promise(done=>server.close(done));await rm(directory,{recursive:true,force:true});
  }
}

// Usa identidades e provas sintéticas segregadas, com a mesma estrutura de produção.
function fixture(base,id,experimentId,videoHash,imageHash) {
  const sources=[0,1].map(index=>({taskId:id+index+10,evidenceId:id+index+20,
    sha256:imageHash,crop:[index*10,index*10,50,50],sourceRole:'APPLIED_PRODUCT_PROOF',
    captionCueIndex:index,reviewMoments:[index*2+0.5],appliedIntervals:`between(t,${index},${index+1})`}));
  const arts=[0,1].map(index=>({visualAssetId:id+30+index,url:base+`/art-${index}.png`,sha256:imageHash,
    captionCueIndex:index,startSeconds:index*2,endSeconds:index*2+1,reviewMoment:index*2+0.5,
    origin:'SYNTHETIC_LOCAL_ONLY',rightsStatement:'Fixture local, sem evidência comercial.'}));
  const segments=['Primeiro exemplo','Segundo exemplo'].map((text,index)=>({segmentIndex:index+1,
    text,inputTextSha256:sha(Buffer.from(text)),sha256:imageHash,
    originalReceipt:{operation:'TEXT_TO_SPEECH',provider:'OPENAI',status:'COMPLETED',
      request:{input:text,model:'fixture-model',voice:'fixture-voice'},response:{sha256:imageHash,bytes:100}}}));
  return {creativeId:id,experimentId,format:'VIDEO',mediaUrl:base+'/video.mp4',mediaGovernanceEvidence:{
    contractVersion:'CREATIVE_MEDIA_GOVERNANCE_V5',verificationStatus:'VERIFIED',
    generationStrategy:'DETERMINISTIC_EDITORIAL_MOTION_FROM_APPROVED_PRODUCT_PROOF',
    finalArtifact:{sha256:videoHash},editorialMediaEvidence:{experimentId,productVersion:'local-v1',
      blockers:[],productProofSources:sources,deliverySources:arts,
      audio:{narrationText:'Primeiro exemplo Segundo exemplo',segments,costStatus:'LOCAL_TEST_ONLY'}}}};
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href===import.meta.url)
  console.log(JSON.stringify(await validateEditorialInspection(process.argv[2]??null)));
