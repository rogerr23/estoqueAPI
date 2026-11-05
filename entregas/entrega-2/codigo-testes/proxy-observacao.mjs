// Instrumentação exclusiva de QA: não usar em produção.
// Sem registro de headers, cookies, corpos, credenciais ou tokens.
import http from 'node:http';
import fs from 'node:fs';
const path = process.env.QA_LOG || '/private/tmp/estoque-entrega2/proxy.jsonl';
const server = http.createServer((req, res) => {
  const start = new Date().toISOString();
  const chunks=[];
  req.on('data', c=>chunks.push(c));
  req.on('end', ()=> {
    const send = () => {
      const upstream = http.request({host:'127.0.0.1',port:8081,path:req.url,method:req.method,headers:req.headers}, response=>{
        res.writeHead(response.statusCode, response.headers);
        response.pipe(res);
        response.on('end',()=>fs.appendFileSync(path,JSON.stringify({start,end:new Date().toISOString(),method:req.method,path:req.url,status:response.statusCode})+'\n'));
      });
      upstream.on('error',()=>{res.writeHead(502);res.end('Proxy de testes indisponível');});
      upstream.end(Buffer.concat(chunks));
    };
    if(req.method==='POST' && req.url==='/api/transferencias') setTimeout(send,8000);
    else send();
  });
});
server.listen(8082,'127.0.0.1',()=>console.log('Proxy QA em 8082; atraso de 8 s apenas no POST de transferência'));
