import test from 'node:test';
import assert from 'node:assert/strict';
import { buildVisualRequest } from '../src/visual-personalization-v1.js';

const entry={jobId:'pde-visual-v1-00000000-0000-4000-8000-000000000001',productName:'Outro produto',productVersion:'commercial-v1',
 contractVersion:'PDE_PAID_VISUAL_V1',trafficClass:'CUSTOMER',model:'gpt-5.6',inputHash:'a'.repeat(64),
 input:{occasion:'Jantar',pieces:['camisa','calça'],preferences:'Conforto',constraints:''}};
test('entrega paga preserva entrada real, limites e transparência sem alegar homologação privada',async()=>{
 const request=await buildVisualRequest(entry);
 assert.equal(request.max_tool_calls,1);assert.equal(request.service_tier,'flex');
 assert.deepEqual(JSON.parse(request.input[2].content),entry.input);
 assert.match(request.input[0].content,/Visualização ilustrativa gerada por IA/);
 assert.doesNotMatch(request.input[0].content,/Entrada simulada/);
 assert.match(request.input[1].content,/PDE_PAID_VISUAL_V1/);
 await assert.rejects(buildVisualRequest({...entry,trafficClass:'AGENT_VALIDATION'}));
 await assert.rejects(buildVisualRequest({...entry,contractVersion:'PDE_VISUAL_PERSONALIZATION_V1'}));
});
