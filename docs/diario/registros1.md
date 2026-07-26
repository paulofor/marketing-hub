# Diario de registros

## 2026-07-26 - Product Discovery com linguagem real de consumidor

- Pergunta explicita de causa raiz: "por que esse erro aconteceu?". Resposta: os ciclos recentes do Product Discovery concluiam sem falha tecnica, mas com score baixo porque o worker montava consultas agregando tema e publico com termos amplos como "dificuldade", "review" e "como resolver"; isso gerava pesquisa generica, pouco proxima da linguagem real usada por consumidores em dores, reclamacoes, duvidas e arrependimentos.
- Alternativas avaliadas: (1) criar mais ciclos iguais, baixo esforco mas alta chance de repetir score 45; (2) escolher manualmente uma oportunidade e ir para campanha, rapido mas com risco de midia sem evidencia; (3) melhorar o gerador de queries do worker com frases reais de dor e multiplas consultas por recorte, maior aderencia a descoberta comercial e baixo escopo tecnico. Escolhida a alternativa 3.
- Ajuste aplicado em `product-discovery-worker/src/research.js`: `buildSearchQueries` agora combina frases reais por dominio (roupa online/caimento, looks/rotina e estilo/imagem pessoal) com templates genericos em primeira pessoa para PDEs futuros.
- A melhoria preserva a normalizacao de termos como `30+` para `30 anos ou mais` e remove duplicidades antes de consultar o provedor.
- Testes adicionados em `product-discovery-worker/test/research.test.js` validam consultas de arrependimento de compra online, estilo acessivel e looks para rotina real.
