import {createServer} from 'node:https';
import {request} from 'node:http';
import {readFileSync,createReadStream,statSync} from 'node:fs';
import {resolve,extname,basename} from 'node:path';

// Serve somente artefatos compilados e mídia sintética; proxy restrito ao backend da sandbox.
const media=resolve('.codex/vega-approved/media');
const web=resolve('pde-platform/frontend/dist-vega');
createServer({key:readFileSync(media+'/local.key'),cert:readFileSync(media+'/local.crt')},(req,res)=>{
  if(req.url.startsWith('/api/')){
    const upstream=request('http://127.0.0.1:18091'+req.url,{method:req.method,headers:req.headers},reply=>{
      res.writeHead(reply.statusCode,reply.headers);reply.pipe(res);
    });upstream.on('error',()=>res.writeHead(502).end());req.pipe(upstream);return;
  }
  const url=new URL(req.url,'https://localhost');
  const file=url.pathname.startsWith('/media/')?resolve(media,basename(url.pathname)):
    resolve(web,extname(url.pathname)?url.pathname.replace(/^\/vega-private\//,''):'index.html');
  if(!file.startsWith(media+'/')&&!file.startsWith(web+'/')){res.writeHead(404).end();return;}
  try{
    const size=statSync(file).size;
    const headers={'Content-Type':{'.html':'text/html','.js':'application/javascript','.css':'text/css','.mp4':'video/mp4'}[extname(file)]||'application/octet-stream',
      'Cache-Control':'no-store','Accept-Ranges':'bytes','Access-Control-Allow-Origin':'*'};
    const range=/^bytes=(\d+)-(\d*)$/.exec(req.headers.range||'');
    const start=range?Number(range[1]):0,end=range&&range[2]?Math.min(Number(range[2]),size-1):size-1;
    if(start>=size){res.writeHead(416).end();return;}
    res.writeHead(range?206:200,{...headers,'Content-Length':end-start+1,...(range?{'Content-Range':`bytes ${start}-${end}/${size}`}:{})});
    createReadStream(file,{start,end}).pipe(res);
  }catch{res.writeHead(404).end();}
}).listen(18443,'127.0.0.1',()=>console.log('HTTPS local em 18443'));
