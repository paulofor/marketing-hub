import {createServer,request} from 'node:http';import {readFile} from 'node:fs/promises';import {resolve,extname} from 'node:path';
const root=resolve('pde-platform/frontend/dist-vega');
createServer(async(req,res)=>{
 if(req.url.startsWith('/api/')){const proxy=request('http://127.0.0.1:18080'+req.url,{method:req.method,headers:req.headers},response=>{res.writeHead(response.statusCode,response.headers);response.pipe(res);});proxy.on('error',()=>res.writeHead(502).end());req.pipe(proxy);return;}
 let pathname=new URL(req.url,'http://localhost').pathname.replace(/^\/vega-private\/?/,'');if(!pathname||!extname(pathname))pathname='index.html';const file=resolve(root,pathname);if(!file.startsWith(root+'/')){res.writeHead(404).end();return;}
 try{const body=await readFile(file);res.writeHead(200,{'Content-Type':{'.html':'text/html','.js':'application/javascript','.css':'text/css'}[extname(file)]||'application/octet-stream','Cache-Control':'no-store','Referrer-Policy':'no-referrer','X-Robots-Tag':'noindex, nofollow'}).end(body);}catch{res.writeHead(404).end();}
}).listen(18083,'0.0.0.0',()=>console.log('Artefato compilado em 18083'));
