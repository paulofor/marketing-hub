import {createServer} from 'node:http';
const server=createServer(async(req,res)=>{let text='';for await(const data of req)text+=data;
 const body=JSON.parse(text);if(body.service_tier!=='flex' || body.text?.format?.type!=='json_schema'){res.writeHead(400).end('{}');return;}
 const content=body.input[0].content;const data=JSON.parse(content.slice(content.lastIndexOf('Contexto persistido do backend:')+'Contexto persistido do backend:'.length));
 let card={cardId:data.cardId,action:'Faça uma dobra simples nas mangas da camisa.',application:'Dobre a ponta de cada manga uma vez, sem apertar. Observe se o caimento permite movimentar os braços com conforto.',occasion:data.input.occasion,selfAssessmentPrompt:'O ajuste ficou confortável e combina com seu almoço?',usesOnlyAvailableItems:true};
 if(data.input.optionalNote==='TEST_INVALID')delete card.application;
 if(data.input.optionalNote==='TEST_503'){res.writeHead(503,{'Content-Type':'application/json'}).end(JSON.stringify({error:{message:'Indisponibilidade sintética'}}));return;}
 res.writeHead(200,{'Content-Type':'application/json'}).end(JSON.stringify({status:'completed',output:[{type:'message',content:[{type:'output_text',text:JSON.stringify(card)}]}],usage:{input_tokens:210,output_tokens:90}}));});
server.listen(18081,'0.0.0.0',()=>console.log('Provedor sintético em 18081'));
