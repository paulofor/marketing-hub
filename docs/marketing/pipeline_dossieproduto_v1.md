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

### Recomendações de melhoria

- Tratar o `intake` como triagem comercial obrigatória, não apenas abertura técnica do job.
- Capturar e preservar, quando existirem, headline, subheadline, promessa principal, preço, bônus, garantia, CTA, produtor, domínio, redes sociais, pixels/scripts públicos, depoimentos, comentários, imagens de prova e links externos da página.
- Classificar a qualidade da matéria-prima em `RICA`, `PARCIAL` ou `INSUFICIENTE`, com motivo objetivo.
- Criar um resumo inicial da hipótese comercial da página em linguagem simples: "este produto parece vender porque promete X para Y usando Z".
- Sinalizar desde o início se a página é página de venda completa, checkout, presell, artigo, página de captura, área de membros, marketplace ou página quebrada.
- Guardar evidências brutas suficientes para as etapas seguintes não dependerem de inferência solta do modelo.

### Exemplo real observado no banco

No produto MOIS `400` — `Resina LAB`, a etapa `intake` registrou `OBJECTIVE_FULFILLED` e decisão de validar se a página tinha contexto mínimo, preservando texto da página, produto, produtor, promessa, marca e sinais públicos. O mesmo registro mostrou que as chaves disponíveis eram principalmente operacionais (`jobId`, `pageId`, `productKey`, `stageCode`, `status`, `nextStageCode`).

Leitura de negócio: o `intake` conseguiu abrir o fluxo, mas o exemplo deixa claro que só iniciar a execução não significa que o dossiê terá matéria-prima comercial suficiente. Quando o contexto chega pobre, a etapa deve sinalizar essa limitação para não contaminar os capítulos seguintes.

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

### Recomendações de melhoria

- Expandir a leitura para além de produto/público/dor/promessa, incluindo nível de consciência do público, maturidade da dor, urgência, desejo dominante e transformação percebida.
- Separar claramente:
  - dor de superfície;
  - dor raiz;
  - dor emocional/social;
  - ganho desejado;
  - objeção principal;
  - custo de não agir.
- Identificar o ângulo central da oferta: emagrecimento, renda, habilidade, saúde, relacionamento, produtividade, status, segurança, economia, pertencimento ou outro.
- Mapear a arquitetura da oferta: promessa, mecanismo, módulos, bônus, garantia, preço, escassez, urgência, CTA e redução de risco.
- Apontar o "porquê agora": qual tensão, tendência, medo, oportunidade ou mudança de mercado torna a compra mais provável.
- Gerar uma hipótese de sucesso do produto em formato direto: "o produto ganha força porque combina [dor forte] + [resultado desejado] + [mecanismo crível] + [prova/autoridade] + [oferta fácil de comprar]".

### Exemplo real observado no banco

No produto MOIS `400` — `Resina LAB`, a resposta da etapa `product-understanding` foi concluída, mas o próprio conteúdo indicou lacunas: público, dor, promessa, mecanismo e formato apareceram como “não informado no contexto” ou “pendente de investigação”.

Leitura de negócio pelo eixo do Marketing Hub:

| Eixo | O que o banco mostrou | Decisão para o dossiê |
|---|---|---|
| Dor | Não informada no contexto. | Não criar hipótese forte sem nova evidência. |
| Resultado | Não consolidado. | Evitar promessa comercial ainda. |
| Mecanismo | Não informado. | Exigir fonte ou descrição real do método. |
| Prova | Não disponível nesta etapa. | Depender das etapas externas antes de recomendar avanço. |
| Oferta | Produto identificado, mas entendimento incompleto. | Prosseguir com investigação, não com experimento. |

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

### Recomendações de melhoria

- Gerar âncoras por intenção de investigação, não apenas por palavra-chave.
- Separar buscas para:
  - produto exato;
  - produtor;
  - marca;
  - afiliados;
  - reviews;
  - reclamações;
  - aulas/lives/webinars;
  - comunidades;
  - anúncios;
  - redes sociais;
  - páginas de captura;
  - termos proprietários do mecanismo.
- Criar combinações de busca que revelem aquecimento, como `produto + depoimento`, `produto + reclamação`, `produtor + live`, `marca + resultados`, `mecanismo + antes e depois`, `produto + afiliado`.
- Definir perguntas obrigatórias da investigação: onde o público conhece o produto, quem explica a promessa, quem dá autoridade, onde aparecem provas, quais objeções surgem e qual canal empurra a decisão.
- Atribuir prioridade a cada âncora: alta quando identifica ecossistema real do produto; média quando identifica nicho; baixa quando é genérica.
- Registrar hipóteses concorrentes para evitar confirmação automática de uma narrativa fraca.

### Exemplo real observado no banco

No produto MOIS `400` — `Resina LAB`, a etapa `investigation-anchor-builder` concluiu com a decisão de gerar âncoras a partir de produto, produtor, domínio, marca, promessa e termos proprietários. Porém, como a etapa anterior não trouxe esses elementos com riqueza comercial, as âncoras ficaram dependentes de identificadores básicos do produto.

Leitura de negócio: quando o banco mostra pouca informação de produto/produtor/marca, a etapa deve explicitar a lacuna. Uma âncora fraca tende a buscar palavras genéricas e aumenta o risco de trazer fontes sem relação real com a oferta.

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

### Recomendações de melhoria

- Tratar a descoberta de recursos como mapeamento de canais de aquecimento, não como simples busca de links.
- Classificar cada recurso por papel comercial:
  - descoberta;
  - educação;
  - autoridade;
  - comunidade;
  - prova social;
  - comparação/review;
  - objeção/reputação;
  - demonstração;
  - captura de lead;
  - oferta direta;
  - remarketing provável.
- Buscar evidências em canais que normalmente aquecem produtos digitais: YouTube, Instagram, TikTok, Facebook, Google, blogs, páginas de afiliados, marketplaces, páginas de captura, webinars, lives, grupos, Reclame Aqui, reviews, comentários e bibliotecas de anúncios quando disponíveis.
- Diferenciar recurso próprio, recurso de afiliado, recurso de creator, recurso de mídia espontânea e recurso de marketplace.
- Priorizar recursos que expliquem o "como convence": aula, vídeo longo, sequência de posts, depoimento, estudo de caso, review comparativo, comunidade ou página de objeções.
- Registrar lacunas por canal. Exemplo: "há marketplace, mas não há evidência de canal educacional"; "há review, mas não há prova social direta"; "há promessa, mas não há mecanismo demonstrado".

### Exemplo real observado no banco

No produto MOIS `401` — `BLACK MAGRA`, a investigação externa encontrou fontes com maior utilidade comercial: marketplace Hotmart, página de aluno do Programa Monteze, Reclame Aqui e outro produto/rede associado à marca. Já no produto MOIS `179` — `PACOTE COMPLETO - Vitalício - TUDO LIBERADO + Tripé`, o dossiê antigo encontrou fontes web genéricas, como páginas sem relação comercial forte com a oferta.

Leitura de negócio: esta etapa precisa separar “fonte que aquece a decisão” de “resultado de busca que só contém palavra parecida”. O caso `401` pode alimentar capítulos de prova, autoridade e objeções; o caso `179` deve gerar alerta de evidência fraca.

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

### Recomendações de melhoria

- Usar uma matriz de vínculo fonte-produto com critérios explícitos: nome exato, domínio, produtor, marca, identidade visual, CTA, checkout, depoimento, link afiliado, menção direta, mecanismo proprietário ou promessa específica.
- Atribuir `matchStrength` por fonte: `DIRETO`, `PROVAVEL`, `INDIRETO`, `NICHO_APENAS` ou `DESCARTADO`.
- Separar evidência de aquecimento real de evidência de existência. Uma página em marketplace prova que a oferta existe; não prova sozinha que o mercado foi aquecido.
- Marcar conflitos de identidade: produto com mesmo nome, produtor diferente, marca genérica, domínio suspeito, página clonada, review sem relação ou conteúdo reaproveitado.
- Preservar fontes rejeitadas com motivo, pois elas ajudam a explicar por que o dossiê não deve concluir com confiança excessiva.
- Criar um score de confiança por fonte combinando relação com produto, atualidade, papel no funil, profundidade do conteúdo e independência da evidência.

### Exemplo real observado no banco

No produto MOIS `401` — `BLACK MAGRA`, as fontes externas tinham relação comercial mais plausível com marca, marketplace, reputação e área de membros. No produto MOIS `179`, parte das fontes retornadas era genérica e não explicava o sucesso da oferta.

Leitura de negócio: a etapa `source-product-match` deve aprovar fontes como evidência apenas quando houver vínculo claro com produto, produtor, marca, promessa ou mecanismo. Se a relação for só por termo solto, a fonte deve ir para rejeitadas ou para evidência fraca, nunca para a conclusão final.

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

### Recomendações de melhoria

- Extrair sinais por mecanismo de persuasão, não apenas por presença de termos.
- Organizar sinais em grupos:
  - dor explorada;
  - promessa repetida;
  - mecanismo explicado;
  - prova social;
  - autoridade;
  - demonstração;
  - comunidade;
  - objeções;
  - risco percebido;
  - urgência/escassez;
  - distribuição;
  - linguagem do público.
- Identificar como o consumidor é preparado emocionalmente: medo de continuar igual, desejo de facilidade, desejo de prazer, pertencimento, status, economia de esforço ou esperança de transformação rápida.
- Capturar frases, temas e padrões recorrentes, sem depender de uma única fonte isolada.
- Diferenciar sinais de venda forte de sinais de alerta. Reclamações podem revelar objeções críticas; comentários positivos podem revelar promessa mais valorizada; afiliados podem revelar canais de escala.
- Produzir uma leitura de intensidade: sinal forte quando aparece em vários canais ou em fonte de alta confiança; sinal fraco quando aparece apenas em uma fonte indireta.

### Exemplo real observado no banco

No produto MOIS `401` — `BLACK MAGRA`, os resumos de aquecimento registraram sinais de autoridade ou marca pública, canal de audiência, oferta em marketplace e presença de reviews/objeções. O score ficou entre `55` e `57`, com temperatura `WARM`.

Leitura de negócio: a etapa deve transformar fontes em sinais úteis, por exemplo: marketplace indica distribuição/oferta; Reclame Aqui indica reputação e objeções; página de aluno indica área de membros ou entrega. Esses sinais ajudam a decidir se vale aprofundar, mas ainda precisam ser conectados às fontes específicas.

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

### Recomendações de melhoria

- Transformar os sinais em uma narrativa de jornada: como o consumidor descobre a dor, entende a promessa, acredita no mecanismo, vê prova, supera objeções e chega à oferta.
- Mapear os canais por função no funil:
  - topo: descoberta da dor e captura de atenção;
  - meio: educação, autoridade e demonstração;
  - fundo: prova, objeções, urgência, preço e CTA;
  - pós-compra/reputação: comunidade, suporte, reclamações e resultados.
- Indicar o provável motor de crescimento do produto: autoridade do produtor, afiliados, creators, tráfego pago, SEO, marketplace, comunidade, lançamentos, perpétuo, presell ou reputação orgânica.
- Avaliar a coerência entre promessa, canal e público. Um produto pode ter boa promessa, mas canal fraco; ou bom canal, mas mecanismo pouco crível.
- Criar uma matriz de força comercial com notas separadas para demanda, clareza da promessa, credibilidade do mecanismo, prova, distribuição, objeções e facilidade de compra.
- Apontar lacunas que impedem adaptação pelo Marketing Hub: falta de prova, falta de mecanismo claro, canal desconhecido, promessa regulatória arriscada, dependência excessiva de autoridade pessoal ou ausência de sinais recentes.

### Exemplo real observado no banco

No produto MOIS `401` — `BLACK MAGRA`, o resumo de aquecimento classificou o mercado como `WARM`, com ecossistema `CREATORS_HEATED`/`SPECIALISTS_HEATED`, canais `MARKETPLACE`, `WEB` e `REVIEW_SITE`, além de risco de saturação baixo. No produto MOIS `179`, a temperatura ficou `COLD`, mesmo havendo hipótese de oferta/afiliados/marketplace.

Leitura de negócio: o mapa deve mostrar o caminho de aquecimento. Para `401`, há indícios de autoridade, distribuição e reputação que podem preparar a compra. Para `179`, falta identificar o canal real que educa ou convence o público; por isso o próximo passo é investigar produtor, especialista, creator ou marca.

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

### Recomendações de melhoria

- Estruturar o dossiê final como relatório executivo de marketing, não como resumo técnico das etapas.
- A síntese deve responder obrigatoriamente:
  - o que o produto vende;
  - para quem vende;
  - qual dor/desejo movimenta a compra;
  - qual promessa central aparece;
  - qual mecanismo torna a promessa crível;
  - quais provas sustentam a crença;
  - quais canais aquecem o consumidor;
  - como o consumidor é educado antes da compra;
  - quais objeções aparecem;
  - qual parece ser o motor de sucesso;
  - o que pode ser adaptado pelo Marketing Hub;
  - o que não deve ser copiado por risco, baixa evidência ou dependência externa.
- Entregar uma conclusão em três níveis:
  - `FORTE`: evidências múltiplas e canais claros de aquecimento;
  - `PROMISSOR_COM_LACUNAS`: há sinais úteis, mas faltam provas ou canais;
  - `FRACO_OU_INSUFICIENTE`: não há base confiável para explicar sucesso.
- Incluir um quadro final de oportunidades acionáveis: ângulos de campanha, canais prioritários, criativos sugeridos, provas necessárias, riscos e próximos testes.
- Separar explicitamente "o que explica o sucesso observado" de "hipóteses que ainda precisam ser validadas".
- Evitar recomendação genérica. Toda recomendação precisa apontar evidência, impacto comercial e próximo passo.

### Exemplo real observado no banco

No produto MOIS `400` — `Resina LAB`, a etapa `dossier-synthesis` falhou com o motivo: as etapas anteriores não entregaram evidências comerciais suficientes para gerar conclusão, recomendação e próximos passos úteis. Em contraste, o produto MOIS `401` — `BLACK MAGRA` tinha conclusão antiga indicando evidência promissora de autoridade ou marca pública e canal de audiência, mas ainda recomendava cruzar sinais com análise do modelo e fontes listadas.

Leitura de negócio: a síntese final deve escolher entre três caminhos claros: aprovar com evidência forte, aprovar parcialmente com investigação complementar ou bloquear por falta de evidência. O bloqueio do `400` é correto porque evita transformar uma execução técnica em recomendação comercial falsa.

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
