import {mkdirSync,readFileSync,writeFileSync,copyFileSync,chmodSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {dirname,resolve} from 'node:path';
const root=resolve('artifacts/vega380');
mkdirSync(root+'/proxy-certs',{recursive:true});
execFileSync('openssl',['req','-x509','-newkey','rsa:2048','-nodes','-keyout',root+'/proxy-certs/test.key','-out',root+'/proxy-certs/test.crt','-days','2','-subj','/CN=v7.clubemusa.com.br','-addext','subjectAltName=DNS:v7.clubemusa.com.br'],{stdio:'ignore'});
const config=readFileSync('lead-portal-payments-service/nginx.conf','utf8');
for(const match of config.matchAll(/ssl_certificate(_key)?\s+\/etc\/nginx\/certs\/([^;]+);/g)){
 const dest=root+'/proxy-certs/'+match[2];mkdirSync(dirname(dest),{recursive:true});copyFileSync(root+'/proxy-certs/test.'+(match[1]?'key':'crt'),dest);
}
const directory=root+'/image-harness';mkdirSync(directory,{recursive:true});chmodSync(directory,0o777);
writeFileSync(directory+'/input.json',JSON.stringify({mode:'TECHNICAL',captureSessionId:'local-image-vega380',sourceUrl:'http://vega-frontend/vega-private/',sourceReference:'experiment:91092',productId:91004,productSlug:'metodo-musa-7-dias',prototypeVersion:process.env.VEGA_TEST_VERSION||'musa-pde-entry-v9-primeiro-ajuste-aplicavel',cycleId:91002}));
