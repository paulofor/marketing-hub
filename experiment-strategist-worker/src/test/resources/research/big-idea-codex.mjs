#!/usr/bin/env node
import fs from 'node:fs';
import {fileURLToPath} from 'node:url';

// Simula somente o modelo; argumentos, stdin, schema e recibos percorrem o executor real.
const args=process.argv.slice(2);
const prompt=fs.readFileSync(0,'utf8');
const schema=JSON.parse(fs.readFileSync(args[args.indexOf('--output-schema')+1],'utf8'));
if(schema.properties.contractVersion.const!=='PRODUCT_BIG_IDEA_RESEARCH_V1') process.exit(4);
if(!prompt.includes('alta demanda comprovada')) process.exit(5);
const result=JSON.parse(fs.readFileSync(fileURLToPath(new URL('../../../../../infra/testing/product-big-idea/research-result.json',import.meta.url)),'utf8'));
result.productId=Number(prompt.match(/"productId"\s*:\s*(\d+)/)[1]);
fs.writeFileSync(args[args.indexOf('--output-last-message')+1],JSON.stringify(result));
console.log(JSON.stringify({type:'turn.completed',usage:{input_tokens:240,cached_input_tokens:40,output_tokens:130}}));
