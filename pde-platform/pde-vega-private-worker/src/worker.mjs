import {readFile,writeFile,mkdir,readdir,rename,unlink} from 'node:fs/promises';
import {resolve} from 'node:path';
import {pathToFileURL} from 'node:url';
import {buildRequest,boundary,parseResult} from './vega/v1/pipeline/adjustment/processor.mjs';
const pause=ms=>new Promise(r=>setTimeout(r,ms));
export async function runOnce(config) {
  const base=config.backend.replace(/\/$/,'')+'/api/pde/vega/private/v1/internal/adjustment/stage-executions';
  const api=async(path,body)=>{
    const response=await fetch(base+path,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json','X-PDE-Internal-Token':config.token},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(30000)});
    if(!response.ok) throw new Error(`Vega backend ${path}: HTTP ${response.status}`);
    const text=await response.text();return text?JSON.parse(text):null;
  };
  await mkdir(config.outbox,{recursive:true,mode:0o700});
  // Reenvia a resposta salva antes de buscar novo trabalho, sem repetir inferência nem consumo.
  for(const filename of await readdir(config.outbox)) if(/^\d+\.json$/.test(filename)) {
    const message=JSON.parse(await readFile(resolve(config.outbox,filename),'utf8'));
    await api(`/${message.id}/result`,message.result);await unlink(resolve(config.outbox,filename));
  }
  for(const pending of await api('/pending')) {
    let job;
    try {job=await api(`/${pending.id}/claim`,{});} catch(error) {console.error('Vega claim',pending.id,error);continue;}
    if(job.status!=='RUNNING')continue;
    let result;const model=config.model;
    try {
      const blocked=boundary(job.context);
      if(blocked)result={status:'BLOCKED',card:null,rawResponse:{boundary:blocked},model:'boundary-v1',inputTokens:0,outputTokens:0,costUsd:0,error:blocked};
      else {
        const request=await buildRequest(job,model);
        await api(`/${job.id}/request`,{request,model});
        console.info(JSON.stringify({operation:'openai.request',executionId:job.id,url:config.openaiUrl,request}));
        const response=await fetch(config.openaiUrl,{method:'POST',headers:{Authorization:`Bearer ${config.key}`,'Content-Type':'application/json'},body:JSON.stringify(request),signal:AbortSignal.timeout(config.timeoutMs||300000)});
        const rawText=await response.text();let raw;
        try {raw=JSON.parse(rawText);}catch{raw={unparseableBody:rawText.slice(0,100000)};}
        console.info(JSON.stringify({operation:'openai.response',executionId:job.id,url:config.openaiUrl,status:response.status,response:raw}));
        if(!response.ok) result={status:'FAILED',card:null,rawResponse:raw,model,error:`O serviço de geração respondeu HTTP ${response.status}. Tente novamente em uma nova tentativa.`,inputTokens:raw.usage?.input_tokens??null,outputTokens:raw.usage?.output_tokens??null,costUsd:null};
        else {
          try {result={status:'COMPLETED',card:parseResult(raw,job),rawResponse:raw,model,inputTokens:raw.usage?.input_tokens??null,outputTokens:raw.usage?.output_tokens??null,costUsd:null,error:null};}
          catch(error) {console.error('Vega resposta inválida',job.id,error);result={status:'FAILED',card:null,rawResponse:raw,model,inputTokens:raw.usage?.input_tokens??null,outputTokens:raw.usage?.output_tokens??null,costUsd:null,error:error.message};}
        }
      }
    } catch(error) {
      console.error('Vega geração falhou',job.id,error);
      result={status:'FAILED',card:null,rawResponse:{error:error.message},model,error:'Não foi possível concluir seu ajuste agora. O contexto foi salvo; tente novamente.',inputTokens:null,outputTokens:null,costUsd:null};
    }
    const path=resolve(config.outbox,`${job.id}.json`);
    await writeFile(path+'.tmp',JSON.stringify({id:job.id,result}),{mode:0o600});await rename(path+'.tmp',path);
    await api(`/${job.id}/result`,result);await unlink(path);
  }
}
async function main(){
 const key=process.env.OPENAI_API_KEY_FILE?(await readFile(process.env.OPENAI_API_KEY_FILE,'utf8')).trim():process.env.OPENAI_API_KEY;
 const config={backend:process.env.VEGA_BACKEND_URL,token:process.env.PDE_INTERNAL_API_TOKEN,key,model:process.env.VEGA_OPENAI_MODEL||'gpt-5-mini',openaiUrl:process.env.OPENAI_RESPONSES_URL||'https://api.openai.com/v1/responses',outbox:process.env.VEGA_OUTBOX||'/data/outbox'};
 if(!config.backend||!config.token||!config.key)throw new Error('Configure backend, credencial interna e credencial OpenAI.');
 while(true){try{await runOnce(config);}catch(error){console.error('Vega worker',error);}await pause(3000);}
}
if(process.argv[1] && import.meta.url===pathToFileURL(resolve(process.argv[1])).href)main().catch(error=>{console.error(error);process.exit(1);});
