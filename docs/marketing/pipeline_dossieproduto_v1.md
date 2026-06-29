# Pipeline de geração de dossiê de produto — MOIS dossieproduto.v1

## 1. Objetivo geral

O pipeline `dossieproduto.v1` transforma uma página de venda da biblioteca MOIS em um dossiê comercial auditável sobre o produto. O objetivo é entender por que aquele produto pode vender, quais dores e promessas aparecem, quais evidências externas sustentam a oferta e quais próximos passos comerciais devem ser tomados antes de criar, adaptar ou escalar uma oferta no Marketing Hub.

O dossiê deve apoiar decisões de venda seguindo o eixo central do Marketing Hub:

> **Dor → Resultado → Mecanismo → Prova → Oferta**

Na prática, o pipeline deve responder:

- qual produto está sendo analisado;
- para qual público ele parece vender;
- qual dor ou desejo central movimenta a compra;
- qual resultado é prometido;
- qual mecanismo plausível sustenta a transformação;
- quais provas, recursos, fontes e sinais externos reforçam ou enfraquecem a oferta;
- se existem lacunas, riscos ou inconsistências antes de usar esse produto como inspiração comercial;
- qual recomendação final deve ser exibida ao usuário.

## 2. Princípios operacionais

- **Backend como fonte de verdade:** o backend controla fila, status, auditoria, avanço entre etapas, callbacks e exposição dos dados para a tela.
- **Worker como executor:** o `mois-sales-library-worker` consome o endpoint `pending`, executa a etapa e reporta request, response, saída funcional, status e artefatos ao backend.
- **Pipeline versionado:** a versão atual é `dossieproduto.v1`; mudanças incompatíveis devem nascer como nova versão, sem sobrescrever silenciosamente a versão atual.
- **Evidência antes de conclusão:** a síntese final só deve ser considerada útil quando as etapas anteriores entregarem evidências comerciais suficientes.
- **Auditabilidade:** cada etapa deve preservar entradas, saídas, decisões, payloads de integração, resposta funcional e erros, para que o frontend consiga explicar o andamento sem depender de logs técnicos.
- **Sem orquestração pelo frontend:** a tela apenas solicita início/reprocessamento e consulta o relatório; a decisão de próxima etapa fica no backend.

## 3. Visão macro do fluxo

1. O usuário solicita o dossiê de uma página de venda na biblioteca MOIS.
2. O backend cria ou atualiza a execução da etapa `intake` e expõe a pendência no endpoint canônico da etapa.
3. O worker consulta `/api/internal/moissaleslibraryworker/dossieproduto/v1/{etapa}/stage-executions/pending`.
4. O worker processa a etapa recebida.
5. O worker envia ao backend:
   - request operacional enviado à etapa ou ao modelo;
   - response bruto quando houver integração externa;
   - saída funcional estruturada;
   - artefatos;
   - status final da etapa.
6. O backend registra auditoria e, quando a etapa conclui com sucesso, avança para a próxima etapa canônica.
7. A tela de dossiê consulta a situação das etapas e exibe auditoria, status, request, response, resposta final e conclusão.

## 4. Ordem canônica das etapas

| Ordem | Código | Nome de negócio | Próxima etapa |
|---:|---|---|---|
| 1 | `intake` | Entrada inicial | `product-understanding` |
| 2 | `product-understanding` | Entendimento do produto | `investigation-anchor-builder` |
| 3 | `investigation-anchor-builder` | Âncoras de investigação | `warmup-resource-discovery` |
| 4 | `warmup-resource-discovery` | Descoberta de recursos | `source-product-match` |
| 5 | `source-product-match` | Relação fonte-produto | `warmup-signal-extraction` |
| 6 | `warmup-signal-extraction` | Extração de sinais | `warmup-map-builder` |
| 7 | `warmup-map-builder` | Mapa de aquecimento | `dossier-synthesis` |
| 8 | `dossier-synthesis` | Síntese final | fim do pipeline |

## 5. Contrato comum de execução

### 5.1 Entrada comum recebida pelo worker

Cada etapa recebe um `StageContext` montado a partir do job pendente entregue pelo backend. A entrada mínima operacional contém:

- `jobId`: identificador da execução;
- `productKey`: identificador externo do produto/página, normalmente derivado do `pageId`;
- `pageId`: identificador da página na biblioteca MOIS;
- `stageCode`: código da etapa atual;
- `status`: status operacional entregue ao worker;
- `nextStageCode`: próxima etapa esperada quando a atual concluir;
- dados funcionais adicionais específicos da etapa, quando o backend os disponibilizar.

Para a síntese final, a entrada deve incluir também as respostas anteriores:

- `previousStages`: mapa das auditorias por etapa;
- `previousStageResponses`: lista de respostas funcionais anteriores.

### 5.2 Saída comum enviada ao backend

Cada etapa deve retornar ao backend:

- status da etapa (`CONCLUIDO`, falha ou bloqueio, conforme contrato do backend);
- saída funcional estruturada da etapa;
- artefatos auditáveis;
- request enviado ao modelo ou à integração, quando houver;
- response bruto recebido, quando houver;
- texto final extraído da resposta, quando aplicável;
- tokens, modelo e custo, quando disponíveis;
- erro operacional com contexto, quando houver falha.

### 5.3 Artefatos esperados

Os artefatos devem separar:

- conteúdo funcional útil para o usuário;
- payload bruto de integração;
- metadados técnicos;
- evidências usadas;
- motivo de bloqueio ou decisão de avanço.

Não deve haver JSON serializado dentro de campos textuais quando existir contrato estruturado para o dado.

## 6. Detalhamento das etapas

## 6.1 Etapa 1 — `intake` — Entrada inicial

### Objetivo

Abrir o dossiê e confirmar que existe contexto mínimo para iniciar a análise do produto. A etapa deve preservar os dados base necessários para que as etapas seguintes não precisem inferir informações sem matéria-prima.

### Entradas

- `jobId`;
- `productKey`;
- `pageId`;
- `stageCode = intake`;
- `nextStageCode = product-understanding`;
- contexto disponível da página de venda;
- idealmente: texto da página, nome do produto, produtor, marca, promessa, sinais públicos e dados capturados anteriormente.

### Processamento

- Verifica se a página existe e pode iniciar o pipeline.
- Registra o início do dossiê e a etapa corrente.
- Confirma a disponibilidade dos dados mínimos.
- Gera evidência auditável com as chaves de entrada recebidas.
- Encaminha a execução para o entendimento do produto quando o status é concluído.

### Saídas

- status funcional da etapa;
- decisão de negócio sobre suficiência inicial do contexto;
- evidências do que foi recebido;
- artefato de objetivo executado;
- próxima etapa: `product-understanding`.

### Critério de qualidade

A etapa não deve ser tratada apenas como marcador técnico. Ela precisa garantir que o pipeline recebeu dados úteis para o dossiê. Se a entrada contiver apenas metadados operacionais, as próximas etapas tendem a produzir análise fraca ou bloqueada.

## 6.2 Etapa 2 — `product-understanding` — Entendimento do produto

### Objetivo

Estruturar o entendimento comercial inicial do produto antes da pesquisa externa. A etapa deve organizar produto, público, dor, promessa, mecanismo, formato, oferta e prova.

### Entradas

- dados operacionais comuns (`jobId`, `productKey`, `pageId`, `stageCode`, `nextStageCode`);
- contexto funcional preservado pelo `intake`;
- texto, promessa, oferta e sinais da página, quando disponíveis;
- saída da etapa anterior, quando exposta pelo backend.

### Processamento

- Monta o request para o modelo de IA quando a OpenAI está configurada.
- Instrui o modelo a responder em JSON válido.
- Usa o eixo **Dor → Resultado → Mecanismo → Prova → Oferta** para organizar a leitura comercial.
- Impede invenção de dados externos quando o modelo só deve usar o contexto recebido.
- Extrai o texto final da resposta da OpenAI.
- Preserva request bruto, response bruto, modelo e tokens para auditoria.
- Em ambiente sem OpenAI configurada, mantém saída local de fallback com objetivo funcional da etapa.

### Saídas

- `produto`;
- `publico`;
- `dor`;
- `promessa`;
- `mecanismo`;
- `formato`;
- `oferta`;
- `prova`;
- `resumo_final`;
- evidências do contexto usado;
- request/response OpenAI quando houver IA;
- próxima etapa: `investigation-anchor-builder`.

### Critério de qualidade

A etapa só é comercialmente forte se o contexto recebido tiver dados reais da página. Se receber apenas `pageId`, `jobId`, `status` e `stageCode`, o modelo tende a devolver JSON formalmente válido, mas sem entendimento de negócio confiável.

## 6.3 Etapa 3 — `investigation-anchor-builder` — Âncoras de investigação

### Objetivo

Gerar âncoras confiáveis para orientar a investigação pública do produto. Essas âncoras definem o que procurar, validar e comparar fora da página original.

### Entradas

- dados operacionais comuns;
- entendimento do produto gerado na etapa 2;
- produto, produtor, domínio, marca, promessa e termos proprietários;
- sinais de dor, resultado, mecanismo, prova e oferta identificados anteriormente.

### Processamento

- Converte o entendimento do produto em termos de busca e validação.
- Separa âncoras por tipo:
  - produto;
  - produtor;
  - marca;
  - domínio;
  - promessa;
  - termos proprietários;
  - mecanismo anunciado;
  - prova social ou autoridade.
- Define quais perguntas externas precisam ser respondidas.
- Evita âncoras genéricas demais que geram fontes irrelevantes.

### Saídas

- lista de âncoras de investigação;
- termos recomendados para busca pública;
- critérios para aceitar ou rejeitar fontes;
- riscos e lacunas da investigação;
- evidências do contexto usado;
- próxima etapa: `warmup-resource-discovery`.

### Critério de qualidade

As âncoras precisam ser específicas o bastante para encontrar o ecossistema real do produto, não apenas o nicho genérico.

## 6.4 Etapa 4 — `warmup-resource-discovery` — Descoberta de recursos

### Objetivo

Planejar e localizar recursos externos que possam aquecer o público antes da compra ou explicar por que o produto vende.

### Entradas

- dados operacionais comuns;
- âncoras de investigação da etapa 3;
- entendimento comercial da etapa 2;
- critérios de busca e validação.

### Processamento

- Identifica candidatos a fontes externas, como:
  - canais;
  - comunidades;
  - aulas;
  - lives;
  - reviews;
  - afiliados;
  - matérias;
  - páginas auxiliares;
  - provas sociais;
  - conteúdos de pré-venda.
- Organiza as fontes candidatas por finalidade comercial.
- Registra origem, tipo de recurso e hipótese de utilidade.
- Mantém rastreabilidade para posterior validação.

### Saídas

- fontes candidatas;
- tipo e papel de cada recurso;
- relação esperada com produto, produtor ou promessa;
- lacunas de fonte;
- próxima etapa: `source-product-match`.

### Critério de qualidade

A etapa deve priorizar fontes que tenham relação verificável com o produto, produtor, marca ou mecanismo comercial. Fontes genéricas do nicho devem ser marcadas como fracas ou descartadas.

## 6.5 Etapa 5 — `source-product-match` — Relação fonte-produto

### Objetivo

Classificar se cada fonte externa descoberta realmente pertence ao produto, produtor, marca ou recurso de aquecimento antes de virar evidência do dossiê.

### Entradas

- dados operacionais comuns;
- fontes candidatas da etapa 4;
- âncoras da etapa 3;
- entendimento comercial da etapa 2;
- critérios de aceitação e rejeição.

### Processamento

- Avalia cada fonte candidata.
- Classifica a relação com o produto:
  - relação direta;
  - relação indireta;
  - relação com produtor/marca;
  - relação apenas com nicho;
  - sem relação confiável.
- Justifica aprovações e rejeições.
- Remove fontes que podem contaminar o dossiê com evidência falsa.

### Saídas

- fontes aprovadas;
- fontes rejeitadas;
- justificativa de cada decisão;
- grau de confiança por fonte;
- evidências preservadas;
- próxima etapa: `warmup-signal-extraction`.

### Critério de qualidade

Nenhuma fonte deve virar evidência final apenas por conter palavra-chave semelhante. A etapa precisa proteger o dossiê contra falso positivo.

## 6.6 Etapa 6 — `warmup-signal-extraction` — Extração de sinais

### Objetivo

Extrair sinais comerciais das fontes qualificadas, especialmente sinais de autoridade, prova social, educação pré-venda, comunidade, distribuição, objeções e intensidade de aquecimento.

### Entradas

- dados operacionais comuns;
- fontes aprovadas na etapa 5;
- entendimento do produto;
- objetivo comercial da investigação;
- critérios de sinal relevante.

### Processamento

- Lê ou consolida informações das fontes aprovadas.
- Extrai sinais como:
  - autoridade do produtor;
  - prova social;
  - volume de distribuição;
  - comentários recorrentes;
  - objeções;
  - linguagem usada pelo público;
  - recursos educacionais antes da oferta;
  - promessa repetida em diferentes canais;
  - mecanismo explicado ou demonstrado.
- Diferencia sinal forte, sinal fraco e ruído.
- Mantém evidência e origem de cada sinal.

### Saídas

- sinais comerciais organizados;
- evidências por fonte;
- objeções e riscos percebidos;
- indícios de demanda ou aquecimento;
- lacunas de validação;
- próxima etapa: `warmup-map-builder`.

### Critério de qualidade

A saída deve conectar cada sinal à fonte que o sustenta. Sinal sem evidência não deve sustentar recomendação final.

## 6.7 Etapa 7 — `warmup-map-builder` — Mapa de aquecimento

### Objetivo

Organizar os recursos externos por papel no aquecimento do público e indicar lacunas que precisam de nova evidência.

### Entradas

- dados operacionais comuns;
- sinais extraídos na etapa 6;
- fontes aprovadas na etapa 5;
- entendimento do produto;
- âncoras e critérios de investigação.

### Processamento

- Agrupa os sinais por papel no funil comercial:
  - descoberta da dor;
  - educação do público;
  - construção de autoridade;
  - prova social;
  - quebra de objeções;
  - demonstração do mecanismo;
  - reforço da promessa;
  - chamada para oferta.
- Monta um mapa de aquecimento do público.
- Aponta riscos comerciais e lacunas.
- Define próximos passos de investigação, validação ou oferta.

### Saídas

- mapa de aquecimento;
- forças comerciais identificadas;
- riscos e lacunas;
- próximos passos recomendados;
- evidências agrupadas;
- próxima etapa: `dossier-synthesis`.

### Critério de qualidade

O mapa deve mostrar como o público é preparado para comprar, não apenas listar links ou sinais soltos.

## 6.8 Etapa 8 — `dossier-synthesis` — Síntese final

### Objetivo

Consolidar a conclusão de negócio, evidências, recursos de aquecimento, recomendação final e próximos passos comerciais para exibição na tela.

### Entradas

- dados operacionais comuns;
- `previousStages` com auditorias e respostas por etapa;
- `previousStageResponses` com respostas funcionais anteriores;
- evidências comerciais das etapas 2 a 7;
- mapa de aquecimento e riscos.

### Processamento

- Lê as respostas anteriores entregues pelo backend.
- Verifica se há evidência comercial suficiente.
- Bloqueia a conclusão quando as etapas anteriores entregaram apenas metadados operacionais.
- Quando há evidência suficiente, consolida:
  - entendimento do produto;
  - hipótese de dor e resultado;
  - mecanismo provável;
  - provas e sinais de mercado;
  - recursos de aquecimento;
  - riscos;
  - recomendação final;
  - próximos passos.
- Gera artefato final separado dos metadados técnicos.

### Saídas

- conclusão final do dossiê;
- status funcional (`APPROVED` ou bloqueio por contexto insuficiente);
- evidências usadas;
- respostas anteriores consideradas;
- próximos passos comerciais;
- motivo de bloqueio, quando houver;
- fim do pipeline.

### Critério de qualidade

A etapa final não deve encerrar como sucesso se não houver evidência comercial suficiente. O bloqueio é desejável quando evita que o usuário tome decisão com base em dossiê vazio.

## 7. Relatório esperado para o usuário

A tela de dossiê deve permitir que o usuário entenda:

- em qual etapa o pipeline está;
- quais etapas já concluíram;
- quais etapas falharam ou foram bloqueadas;
- qual request foi enviado para integrações de IA;
- qual response bruto foi recebido;
- qual texto final foi extraído;
- quais evidências sustentam a conclusão;
- qual é a recomendação comercial final;
- quais próximos passos devem ser tomados.

O usuário não deve precisar ler logs técnicos para compreender o andamento ou a conclusão do dossiê.

## 8. Pontos de atenção atuais

- A etapa `product-understanding` depende fortemente da riqueza do contexto entregue pelo backend. Se a entrada tiver apenas metadados operacionais, o modelo não terá base suficiente para entender o produto.
- O `intake` precisa garantir que os dados comerciais mínimos sejam preservados e repassados às etapas seguintes.
- A síntese final já deve funcionar como gate de qualidade, bloqueando conclusão quando as respostas anteriores não tiverem evidência comercial.
- As etapas intermediárias devem evoluir de respostas declarativas de objetivo para saídas funcionais ricas, com contratos claros de fonte, evidência, classificação e decisão.
- Prompts e schemas de IA devem permanecer versionados no módulo executor responsável, evitando contratos longos hardcoded em classe.

## 9. Resultado de negócio esperado

Ao final, o dossiê de produto deve entregar uma visão prática para decidir se vale a pena:

- criar uma oferta inspirada no produto;
- aprofundar investigação sobre o nicho;
- buscar novas provas;
- usar a promessa como hipótese de campanha;
- adaptar o mecanismo para produto próprio;
- descartar o produto por falta de evidência, baixa força comercial ou risco de falso positivo.

O dossiê não é apenas uma ficha técnica: ele é uma ferramenta de decisão comercial para aumentar a chance de criar produtos digitais que vendem e entregam valor real.
