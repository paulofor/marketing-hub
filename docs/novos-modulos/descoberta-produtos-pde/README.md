# Modulo de Descoberta de Produtos PDE

## Objetivo

Criar um modulo do Marketing Hub para pesquisar continuamente a internet e encontrar
oportunidades de novos produtos PDE a partir de dores grandes, recorrentes e mal atendidas.

O modulo deve alimentar a fabrica de produtos digitais com oportunidades melhores antes
da etapa de hipotese, evitando que o Marketing Hub crie ofertas a partir de intuicao,
moda passageira ou dor pequena demais para sustentar venda em escala.

## Diagnostico que justifica o modulo

Hoje o repositorio ja possui pecas importantes:

- OPRM pesquisa nichos, rotinas, publico executor, linguagem e contexto operacional.
- MOIS analisa produtos existentes e paginas de venda para entender dor, promessa,
  mecanismo, prova e oferta.
- O pipeline de hipotese transforma uma dor em resultado, mecanismo, prova e oferta.
- A metodologia em `docs/neuron/estrada_do_desconhecimento_ao_desejo_de_compra.md`
  mostra que produtos novos precisam conduzir o cliente da resistencia ate o desejo,
  usando relevancia, analogia, mecanismo, microexperiencia, reducao de risco e oferta.

A lacuna esta antes disso:

```text
Internet -> sinais de dor -> oportunidade PDE -> hipotese -> oferta -> experimento
```

Sem essa camada, o sistema pode descobrir nichos e analisar produtos, mas ainda nao tem
um mecanismo dedicado a responder:

```text
Que dor grande, pouco atendida e adequada a PDE vale virar novo produto?
```

## Alternativas avaliadas

### Alternativa 1 - Estender OPRM

Beneficio: aproveita a pesquisa de rotina e fontes publicas ja existentes.

Risco: OPRM tem limite canonico claro: entender nicho e rotina, sem criar produto, oferta
ou tese comercial profunda. Estender OPRM criaria contaminação de fase.

Esforco: medio.

Adequacao ao objetivo: parcial.

### Alternativa 2 - Estender MOIS

Beneficio: MOIS ja trabalha com paginas de venda, produtos existentes e sinais de mercado.

Risco: MOIS parte de produtos ja materializados. Isso tenderia a copiar mercados atendidos,
em vez de descobrir dores grandes ainda mal atendidas.

Esforco: medio.

Adequacao ao objetivo: parcial.

### Alternativa 3 - Criar modulo novo antes da hipotese

Beneficio: separa claramente descoberta de dor, avaliacao de lacuna e encaixe PDE antes
de criar oferta. Permite usar OPRM e MOIS como fontes auxiliares sem misturar responsabilidades.

Risco: exige novo pipeline, contratos, tela e worker.

Esforco: alto, mas controlado se nascer como v1 simples.

Adequacao ao objetivo: alta.

### Decisao

A melhor escolha e criar um modulo novo: **Descoberta de Produtos PDE**.

Ele deve ser um pipeline anterior a hipotese, focado em oportunidade comercial validada
por sinais publicos. OPRM e MOIS entram como apoio, nao como donos do fluxo.

## Posicionamento no Marketing Hub

Fluxo recomendado:

```text
Descoberta Produtos PDE
-> Hipotese
-> Experimento
-> Criativos / Landing / Video
-> Campanha
-> Metricas
-> Aprendizado
-> Escala
```

O modulo deve produzir oportunidades, nao produtos completos.

Produto, oferta, landing e campanha so devem nascer depois que uma oportunidade passar
pelos gates de escala, desatendimento, encaixe PDE e caminho de compra.

## Nome tecnico recomendado

- Dominio: `descoberta-produtos-pde`
- Pipeline: `productdiscovery.v1`
- Worker futuro: `product-discovery-worker`
- Backend futuro: `com.marketinghub.productdiscovery.v1`
- Executor futuro: `com.marketinghub.productdiscoveryworker.productdiscovery.v1`

## Escopo da v1

A v1 deve ser simples e focada em decisao:

- criar ciclos de pesquisa por tema amplo, publico ou territorio de dor;
- pesquisar fontes publicas da internet;
- agrupar sinais em dores candidatas;
- medir escala e recorrencia por evidencias independentes;
- comparar solucoes atuais e lacunas;
- avaliar encaixe como PDE;
- gerar ranking de oportunidades;
- permitir aprovacao humana antes de enviar para hipotese;
- manter evidencias e motivos de decisao persistidos.

## Implementacao operacional v1

A primeira implementacao operacional foi criada em tres partes:

- backend `com.marketinghub.productdiscovery.v1`, como fonte de verdade dos ciclos,
  oportunidades, pendencias e callbacks;
- tela administrativa em `/product-discovery`, para criar ciclos e revisar o ranking;
- worker `product-discovery-worker`, para consumir pendencias, pesquisar sinais publicos
  e devolver oportunidades com evidencias.

Contratos principais:

- `GET /api/product-discovery/v1/cycles`
- `POST /api/product-discovery/v1/cycles`
- `GET /api/product-discovery/v1/cycles/{cycleId}`
- `POST /api/product-discovery/v1/legacy-artificial-evidence/archive`
- `GET /api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending`
- `POST /api/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/complete`
- `POST /api/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/fail`

O backend nao pesquisa internet, nao executa rotina e nao cria produto automaticamente.
Ele apenas persiste contratos, publica pendencias e recebe resultados. O worker executor
mantem a rotina operacional e os prompts/schemas versionados em `product-discovery-worker`.

A limpeza de resultados legados deve ser lógica e auditável: ciclos compostos somente
por páginas de busca sem resultados são arquivados pela tela, permanecem consultáveis
no histórico e deixam de participar do ranking comercial. Nenhum resultado deve ser
apagado nem tratado como evidência real apenas para completar uma execução.

Uma pesquisa dirigida que encontre sinais públicos, mas não alcance o mínimo de ofertas
reais comparáveis, deve terminar como pesquisa válida com decisão `RESEARCH_MORE` e zero
oportunidades. O worker não pode transformar sinais insuficientes em candidatos genéricos,
e o backend deve manter o gate de dez ofertas para qualquer candidato efetivamente enviado.
Termos comerciais genéricos, ocorrências parciais dentro de outras palavras e snapshots
repetidos do mesmo título e produtor não contam como ofertas distintas ou aderentes.
Anúncios permanecem sinais separados e nunca entram na contagem de ofertas pagas comparáveis.

Fora da v1:

- criar campanha automaticamente;
- criar landing automaticamente;
- publicar produto automaticamente;
- fazer scraping privado ou contornar termos de uso;
- coletar dados pessoais;
- prometer resultado sensivel ou garantido.

## Etapas do pipeline v1

### 1. `research-brief`

Define o territorio da pesquisa:

- tema amplo;
- publico desejado;
- pais/idioma;
- canal provavel de aquisicao;
- restricoes comerciais;
- categorias proibidas;
- objetivo do ciclo.

Saida: brief de pesquisa com limites claros.

### 2. `signal-search`

Pesquisa sinais publicos de dor na internet.

Fontes preferenciais:

- buscas abertas;
- perguntas recorrentes;
- comunidades publicas;
- comentarios em conteudos;
- reviews e reclamacoes;
- marketplaces;
- anuncios publicos;
- relatorios setoriais.

Saida: lista de fontes com URL, tipo, resumo, linguagem do publico e risco de contaminacao.

### 3. `pain-clustering`

Agrupa sinais em dores candidatas.

Cada dor deve conter:

- cena de dor;
- publico afetado;
- linguagem recorrente;
- frequencia percebida;
- impacto pratico;
- impacto emocional;
- esforco atual para resolver.

Saida: clusters de dor com evidencias vinculadas.

### 4. `scale-validation`

Valida se a dor pode atingir quantidade grande de pessoas.

Criterios:

- repeticao em mais de uma fonte;
- termos de busca ou tendencia;
- presenca em canais diferentes;
- indicios de investimento concorrente;
- tamanho estimado do publico.

Saida: score de escala e decisao de continuar, pesquisar mais ou rejeitar.

### 5. `unmetness-review`

Analisa se a dor esta mal atendida.

Perguntas:

- quais solucoes existem?
- o que elas resolvem bem?
- onde elas falham?
- sao caras, complexas, demoradas ou pouco personalizadas?
- que objecoes aparecem em reviews e comentarios?
- existe lacuna para uma experiencia digital simples?

Saida: mapa de lacuna e nivel de desatendimento.

### 6. `pde-fit`

Avalia se a oportunidade cabe em produto digital de experiencia.

Criterios:

- entrada simples do usuario;
- mecanismo observavel;
- microresultado rapido;
- baixo esforco percebido;
- antes/depois concreto;
- continuidade natural para oferta paga;
- entrega escalavel;
- risco regulatorio aceitavel.

Saida: proposta de microexperiencia e score PDE.

### 7. `desire-roadmap`

Aplica a estrada do desconhecimento ao desejo de compra.

Para cada oportunidade, responder:

- como o publico reconhece a dor?
- qual analogia torna o produto compreensivel?
- qual mecanismo torna a promessa plausivel?
- qual microexperiencia mostra valor pessoal?
- qual prova reduz risco?
- qual CTA mantem continuidade mental?

Saida: mapa de comunicacao inicial.

### 8. `opportunity-gate`

Decide se a oportunidade avanca.

Decisoes possiveis:

- `APPROVED_FOR_HYPOTHESIS`;
- `NEEDS_MORE_RESEARCH`;
- `REJECTED_SMALL_MARKET`;
- `REJECTED_WELL_SERVED`;
- `REJECTED_WEAK_PDE_FIT`;
- `HUMAN_REVIEW_REQUIRED`.

Saida: dossie de oportunidade PDE.

## Score comercial

Score recomendado:

| Dimensao | Peso |
|---|---:|
| Escala da dor | 20 |
| Intensidade da dor | 15 |
| Frequencia | 15 |
| Desatendimento | 15 |
| Encaixe PDE | 15 |
| Facilidade de comunicacao | 10 |
| Potencial de compra | 10 |

Oportunidades acima de 75 podem ir para hipotese. Entre 55 e 74 devem pedir mais pesquisa
ou revisao humana. Abaixo de 55 devem ser rejeitadas.

## Dossie de oportunidade

O resultado final deve ser curto, acionavel e auditavel:

- nome da oportunidade;
- publico primario;
- dor raiz;
- dor pratica;
- dor emocional;
- tamanho estimado;
- evidencias de escala;
- lacuna de atendimento;
- alternativas existentes;
- microexperiencia PDE;
- mecanismo plausivel;
- primeiro angulo de campanha;
- promessa proibida ou arriscada;
- riscos;
- score;
- decisao;
- proximos passos.

## Tela administrativa

A tela v1 deve permitir:

- criar ciclo de pesquisa;
- ver ciclos em andamento;
- ver ranking de oportunidades;
- abrir dossie;
- filtrar por score, publico, dor, status e risco;
- aprovar para pipeline de hipotese;
- rejeitar com motivo;
- pedir mais pesquisa.

O usuario nao deve depender de logs tecnicos para entender a decisao.

## Contratos de backend

Endpoints recomendados:

```text
POST /api/product-discovery/v1/research-cycles
GET  /api/product-discovery/v1/research-cycles
GET  /api/product-discovery/v1/research-cycles/{cycleId}
GET  /api/product-discovery/v1/opportunities
GET  /api/product-discovery/v1/opportunities/{opportunityId}
POST /api/product-discovery/v1/opportunities/{opportunityId}/approve-for-hypothesis
POST /api/product-discovery/v1/opportunities/{opportunityId}/reject
POST /api/product-discovery/v1/opportunities/{opportunityId}/request-more-research
```

Endpoints internos para worker:

```text
GET  /api/internal/product-discovery/productdiscovery/v1/{stage}/stage-executions/pending
POST /api/internal/product-discovery/productdiscovery/v1/{stage}/stage-executions/{executionId}/result
POST /api/internal/product-discovery/productdiscovery/v1/{stage}/stage-executions/{executionId}/failure
```

## Dados que precisam ser persistidos

- ciclo de pesquisa;
- etapa atual;
- status;
- fontes pesquisadas;
- snapshots/resumos de fonte;
- sinais extraidos;
- clusters de dor;
- scores intermediarios;
- prompt/request/response quando houver IA;
- custo/tokens quando existirem;
- decisao de cada gate;
- dossie final;
- aprovacao/rejeicao humana;
- vinculo com hipotese criada.

## Regras de seguranca comercial

O modulo deve bloquear:

- dor sem escala;
- dor sem evidencia independente;
- oportunidade baseada apenas em tendencia social superficial;
- produto que dependa de promessa sensivel ou garantida;
- uso de dado pessoal sem necessidade;
- solucao que nao consiga mostrar valor em microexperiencia;
- oportunidade que pule direto para oferta sem passar pela estrada do desejo.

## Primeira implementacao recomendada

Para colocar o modulo em producao sem excesso de escopo:

1. Criar backend de ciclos, oportunidades e decisoes.
2. Criar tela simples de ciclos e ranking.
3. Criar worker com apenas tres etapas iniciais: `research-brief`, `signal-search`,
   `pain-clustering`.
4. Persistir evidencias e dossie parcial.
5. Adicionar gates `scale-validation` e `pde-fit`.
6. So depois conectar aprovacao ao pipeline de hipotese.

## Resultado esperado

O Marketing Hub passa a ter uma esteira anterior a criacao de oferta:

```text
dor grande descoberta
-> lacuna comprovada
-> PDE plausivel
-> oportunidade aprovada
-> hipotese mais forte
-> experimento com maior chance de venda
```

Isso aumenta a probabilidade de criar produtos digitais que nao apenas parecem bons, mas
nascem de uma dor ampla, reconhecivel e comercialmente exploravel.
