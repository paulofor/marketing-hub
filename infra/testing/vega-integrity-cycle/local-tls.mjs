// Expõe o artefato e a API locais em TLS de teste para atravessar o gate sem relaxar o HTTPS.
import {createServer} from 'node:https';
import {request} from 'node:http';
import {readFileSync} from 'node:fs';
const root = process.env.LOCAL_TLS_DIRECTORY || process.env.VEGA_TLS_DIRECTORY;
if (!root) throw new Error('Diretório do certificado sintético obrigatório.');
const upstreamPort = Number(process.env.LOCAL_TLS_UPSTREAM_PORT || 18083);
const listenPort = Number(process.env.LOCAL_TLS_LISTEN_PORT || 18084);
if (![upstreamPort, listenPort].every(port => Number.isInteger(port) && port > 0 && port <= 65535))
  throw new Error('Portas locais inválidas.');
createServer({key:readFileSync(root+'/test.key'),cert:readFileSync(root+'/test.crt')},(req,res)=>{
  const upstream=request('http://127.0.0.1:'+upstreamPort+req.url,{method:req.method,headers:req.headers},reply=>{
    res.writeHead(reply.statusCode,reply.headers); reply.pipe(res);
  });
  upstream.on('error',()=>res.writeHead(502).end()); req.pipe(upstream);
}).listen(listenPort,'127.0.0.1',()=>console.log('TLS sintético local pronto'));
