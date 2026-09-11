import { readFile } from 'node:fs/promises';
const resources=new URL('../../../../main/resources/prompts/adjustment/',import.meta.url);
export async function buildRequest(job,model) {
  const prompt=await readFile(new URL('prompt.md',resources),'utf8');
  const schema=JSON.parse(await readFile(new URL('output-schema.json',resources),'utf8'));
  return {model,service_tier:'flex',store:false,max_output_tokens:2500,reasoning:{effort:'low'},
    input:[{role:'system',content:prompt.replace('{{CONTEXT}}',JSON.stringify({...job.context,cardId:String(job.id)}))}],
    text:{format:{type:'json_schema',name:'vega_adjustment_v1',strict:true,schema}}};
}
export function boundary(context) {
  const input=context.input||{};
  return /\b(comprar|compre|adquirir|emagrecer|emagreça|diagn[oó]stico)\b/iu.test(Object.values(input).join(' '))
    ? 'Este ajuste usa apenas a roupa que você já possui. Não recomenda compras, mudanças no corpo ou avaliações de saúde. Você pode reformular com a ocasião e a combinação disponível.' : null;
}
export function parseResult(raw,job) {
  if(raw.status && raw.status!=='completed') throw new Error(`Modelo terminou sem resultado completo (${raw.status}).`);
  const text=raw.output?.flatMap(i=>i.content||[]).filter(c=>c.type==='output_text').map(c=>c.text).join('');
  const card=JSON.parse(text||'null');
  const keys=['action','application','occasion','selfAssessmentPrompt'];
  if(!card || Object.keys(card).sort().join()!=['cardId','usesOnlyAvailableItems',...keys].sort().join() || card.cardId!==String(job.id) || card.usesOnlyAvailableItems!==true || keys.some(k=>typeof card[k]!=='string'||!card[k].trim()||card[k].length>1000) || card.occasion!==job.context.input.occasion || /\b(comprar|compre|adquirir|adquira|emagrecer|emagreça)\b/iu.test(JSON.stringify(card))) throw new Error('Resposta fora do contrato: o cartão completo e aplicável não foi confirmado.');
  return card;
}
