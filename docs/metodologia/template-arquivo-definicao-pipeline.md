# Template para arquivo-definicao de pipeline

## Objetivo deste documento

Este template orienta a criacao do `arquivo-definicao` usado pelo catalogo de prompts de pipeline do Marketing Hub.

O `arquivo-definicao` e a fonte principal para o modelo entender um pipeline quando cada prompt sera enviado isoladamente. Como o modelo recebera apenas um item por vez, este arquivo precisa conter todo o contrato funcional necessario para impedir inferencias soltas, etapas inventadas, endpoints paralelos ou mistura entre dado de negocio e envelope tecnico.

O documento deve ser escrito antes da implementacao do pipeline ou antes de qualquer ajuste estrutural relevante.

## Regra central

Cada etapa do pipeline deve transformar uma entrada clara em uma saida funcional util para venda, decisao comercial, auditoria ou proxima etapa.

Se a etapa nao tiver objetivo, entrada, saida, criterio de conclusao, criterio de bloqueio e proxima etapa permitida, ela ainda nao esta pronta para implementacao.

## 1. Identificacao do pipeline

Preencha no inicio do arquivo:

| Campo | Orientacao |
|---|---|
| Nome do pipeline | Nome estavel, em minusculo e sem ambiguidade. Ex.: `dossieproduto`. |
| Versao | Sempre explicita. Ex.: `v1`, `v2`, `v3`. |
| Modulo executor | Worker/coletor/modulo que executa as etapas. |
| Pacote backend | Pacote onde o backend publica pendencias, recebe callbacks e persiste estado. |
| Pacote executor | Pacote onde o modulo executor implementa o pipeline. |
| Objeto associado | Entidade principal do dominio. Ex.: CNAE, produto, pagina de venda, hipotese. |
| Chave do objeto | Campo usado para iniciar e correlacionar execucoes. |
| Tabela de auditoria | Tabela que registra os passos do pipeline. |
| Endpoint interno base | Base dos endpoints consumidos pelo executor. |
| Endpoint administrativo base | Base dos endpoints usados por tela ou operacao. |
| Objetivo do pipeline | Transformacao de negocio que o pipeline realiza. |

Exemplo:

```text
Pipeline: dossieproduto
Versao: v1
Modulo executor: mois-sales-library-worker
Objeto associado: pagina de venda
Chave do objeto: productKey
Objetivo: transformar uma pagina de venda em dossie comercial auditavel para orientar decisoes de venda
```

## 2. Contexto de negocio

Explique por que o pipeline existe.

Responda objetivamente:

- qual dor, risco ou esforco ele reduz;
- qual decisao comercial ele melhora;
- como ele ajuda o Marketing Hub a vender mais ou evitar perda de tempo;
- qual resultado final o usuario deve conseguir entender;
- quais limites de dominio o pipeline nao deve ultrapassar.

Use o eixo do Marketing Hub quando fizer sentido:

```text
Dor -> Resultado -> Mecanismo -> Prova -> Oferta
```

Exemplo de limite:

```text
Este pipeline cria um dossie comercial auditavel. Ele nao publica campanha, nao cria oferta automaticamente e nao inicia gasto de midia sem gate posterior.
```

## 3. Visao macro do fluxo

Descreva o fluxo em linguagem simples, do inicio ao fim.

Formato sugerido:

1. O usuario ou sistema solicita o pipeline para o objeto associado.
2. O backend cria o job e a primeira etapa pendente.
3. O modulo executor busca trabalho pelo endpoint `pending`.
4. O executor processa a etapa e registra request/response quando houver integracao externa ou IA.
5. O backend valida o resultado funcional, persiste auditoria e decide o avanco.
6. A tela exibe status, saidas funcionais, evidencias, bloqueios e proxima acao.

Declare explicitamente que:

- o backend decide o avanco entre etapas;
- o executor nao acessa banco diretamente;
- o frontend nao chama endpoints internos do worker;
- logs tecnicos nao substituem dados persistidos de relatorio.

## 4. Ordem oficial das etapas

Crie uma tabela com a ordem canonica.

| Ordem | Codigo da etapa | Nome de negocio | Executor | Proxima etapa | Avanco |
|---:|---|---|---|---|---|
| 1 | `codigo-estavel` | Nome claro para usuario | modulo executor | `proxima-etapa` | automatico/manual/fim |

Regras:

- o codigo da etapa deve ser estavel e usado em endpoint, auditoria, processor e tela;
- nao inclua etapa futura sem contrato completo;
- nao declare `nextStageCode` para etapa sem backend, endpoint `pending`, processor e persistencia;
- etapas removiveis ou substituiveis devem continuar independentes, sem chamada direta entre processors concretos.

## 5. Contrato comum do pipeline

Defina quais dados todas as etapas recebem e retornam.

### Entrada operacional comum

Declare os campos de controle que podem aparecer em todas as etapas:

- `jobId`;
- `stageExecutionId`;
- chave do objeto associado;
- `stageCode`;
- `status`;
- `versaoPipeline`;
- contexto funcional entregue pelo backend;
- artefatos anteriores necessarios para a etapa.

Esses campos sao envelope operacional. Eles ajudam a executar e rastrear, mas nao sao por si so evidencia comercial.

### Saida comum para o backend

Declare o que toda etapa deve devolver:

- status final da etapa;
- saida funcional estruturada;
- artefatos auditaveis;
- bloqueio funcional, quando houver;
- causa-raiz do bloqueio ou erro, quando houver;
- request bruto, response bruto, prompt, schema, modelo, tokens e custo, quando houver IA ou integracao externa.

## 6. Separacao obrigatoria entre dado de negocio e envelope tecnico

Esta secao e obrigatoria.

Defina claramente:

### Dado funcional de negocio

E o conteudo que ajuda o usuario, a proxima etapa ou a decisao comercial.

Exemplos:

- diagnostico;
- evidencias comerciais;
- dores identificadas;
- publico;
- mecanismo plausivel;
- resumo de negocio;
- artefato funcional;
- decisao recomendada;
- causa-raiz funcional;
- acao recomendada;
- criterio de aprovacao ou bloqueio.

### Envelope tecnico e auditoria

E o conteudo usado para rastreabilidade, suporte, custo, execucao e depuracao.

Exemplos:

- `stage`;
- `status`;
- `jobId`;
- `stageExecutionId`;
- request bruto;
- response bruto;
- prompt;
- schema;
- tokens;
- custo;
- modelo;
- plataforma;
- erro tecnico;
- stack trace;
- URL chamada;
- headers tecnicos.

Regra obrigatoria:

```text
resposta_final, saida funcional e artefato final nao podem conter request, response, prompt, schema, tokens, custo, modelo, plataforma, erro tecnico, stack trace, jobId ou stageExecutionId.
```

## 7. Detalhamento de cada etapa

Repita este bloco para cada etapa oficial.

### Etapa N — `codigo-da-etapa` — Nome de negocio

#### Objetivo

Explique a transformacao da etapa em linguagem de negocio.

Boa pergunta de validacao:

```text
Como esta etapa ajuda a vender, validar uma venda, reduzir risco comercial ou preparar uma decisao melhor?
```

#### Entradas obrigatorias

Liste somente entradas reais e necessarias.

Separe:

- dados do objeto associado;
- artefatos anteriores;
- contexto funcional;
- parametros operacionais;
- evidencias obrigatorias.

#### Processamento esperado

Descreva o que a etapa faz.

Quando houver IA ou integracao externa, indique:

- qual prompt/schema deve existir no executor;
- qual request deve ser registrado;
- qual response bruto deve ser preservado;
- qual parte vira saida funcional limpa;
- qual validacao bloqueia resposta invalida.

#### Saida funcional estruturada

Declare campos esperados da resposta de negocio.

Exemplo:

```text
- resumoExecutivo
- publicoPrioritario
- dorPrincipal
- mecanismoIdentificado
- evidenciasUsadas
- riscos
- decisao
- proximaAcaoRecomendada
```

Evite saida vaga como "texto gerado". Se for texto, explique a estrutura minima desse texto.

#### Artefatos auditaveis

Liste artefatos produzidos ou preservados:

- payload bruto;
- fonte consultada;
- arquivo gerado;
- snapshot;
- evidencia;
- resposta validada;
- versao do prompt/schema.

#### Criterio de conclusao

Defina quando a etapa pode ser marcada como concluida.

Exemplo:

```text
A etapa conclui quando houver saida funcional valida no schema, pelo menos uma evidencia usada e nenhuma contaminacao de envelope tecnico no artefato final.
```

#### Criterio de bloqueio

Defina quando a etapa deve parar.

Exemplos:

- entrada obrigatoria ausente;
- resposta fora do schema;
- falta de evidencia;
- risco comercial alto;
- tentativa de executar responsabilidade de outro pipeline;
- erro de integracao;
- saida funcional contaminada por metadados tecnicos.

Cada bloqueio deve indicar:

- causa-raiz;
- impacto comercial ou operacional;
- acao recomendada.

#### Proxima etapa permitida

Declare uma unica proxima etapa automatica, uma decisao humana ou fim do pipeline.

Formato:

```text
Proxima etapa automatica: `nome-da-etapa`
Condicao: somente quando a saida funcional estiver valida e sem bloqueios.
```

ou:

```text
Proxima acao humana: aprovar/revisar/descartar antes de continuar.
```

#### Dados que precisam aparecer na tela

Liste o que o usuario precisa ver:

- status em linguagem simples;
- decisao da etapa;
- resumo funcional;
- evidencias;
- bloqueio e causa;
- proxima acao;
- custo, quando houver;
- detalhes tecnicos em area colapsada.

## 8. Endpoints esperados

Defina os endpoints por etapa.

### Endpoints internos

Use o padrao:

```text
GET  {{endpoint-interno-base}}/<codigo-etapa>/stage-executions/pending
POST {{endpoint-interno-base}}/<codigo-etapa>/stage-executions/{stageExecutionId}/recebeRequest
POST {{endpoint-interno-base}}/<codigo-etapa>/stage-executions/{stageExecutionId}/recebeResponse
```

Inclua `start`, `complete`, `fail` ou nomes equivalentes somente quando o fluxo exigir e o contrato estiver claro.

### Endpoints administrativos

Declare os endpoints que a tela usa para:

- iniciar o pipeline;
- consultar status geral;
- listar etapas;
- detalhar uma etapa;
- consultar resultado consolidado;
- reprocessar ou cancelar quando aplicavel.

O frontend deve consumir apenas endpoints administrativos/publicos do backend.

## 9. Persistencia e auditoria

Defina a tabela de auditoria e os campos minimos.

Campos recomendados:

- `id`;
- `id_externo`;
- `job_id`;
- `job_id_externo`;
- `codigo_etapa`;
- `status`;
- `request`;
- `input_extraido_request`;
- `response`;
- `resposta_final`;
- `prompt`;
- `schema_json`;
- `plataforma`;
- `modelo`;
- `quantidade_token_entrada`;
- `quantidade_token_saida`;
- `custo`;
- `descricao_erro`;
- `versao_pipeline`;
- `data_hora`;
- `created_at`;
- `updated_at`.

Declare a funcao de cada grupo:

- `request`, `response`, `prompt`, `schema_json`, modelo, tokens, custo, status e erro sao auditoria/envelope tecnico;
- `input_extraido_request` e `resposta_final` sao dados funcionais limpos;
- o objeto associado guarda apenas resumo operacional e referencia do resultado, nao payload bruto.

## 10. Gates e regras de avanco

Defina os gates do pipeline.

Todo gate deve responder:

- o que ele protege;
- qual risco evita;
- qual evidencia exige;
- quando bloqueia;
- qual causa-raiz registra;
- qual acao destrava.

Exemplos de gates:

- evidencia minima antes de IA;
- resposta dentro do schema;
- qualidade comercial minima;
- aprovacao humana antes de gasto de midia;
- separacao entre dado funcional e envelope tecnico;
- limite de dominio do pipeline.

Regra:

```text
O backend so enfileira a proxima etapa quando o sucesso funcional estiver comprovado.
```

## 11. Contrato do executor

Descreva como o modulo executor deve trabalhar:

- buscar trabalho somente pelo endpoint `pending`;
- nao acessar banco diretamente;
- nao decidir avanco de negocio;
- implementar processors independentes por etapa;
- nao importar uma etapa concreta dentro de outra;
- manter prompt e schema versionados em arquivo quando houver IA;
- registrar `recebeRequest` antes da chamada externa;
- registrar `recebeResponse` apos resposta ou erro;
- devolver saida funcional limpa separada de auditoria.

Quando houver OpenAI, declarar:

- modelo configuravel;
- `service_tier: "flex"` por padrao;
- prompt em arquivo versionado;
- schema JSON em arquivo versionado;
- request e response brutos preservados;
- validacao contra schema antes de concluir.

## 12. Contrato de tela e relatorio

Defina o que a tela precisa mostrar sem depender de logs.

Itens minimos:

- contexto do objeto associado;
- objetivo do pipeline;
- status geral;
- etapa atual;
- cards das etapas em ordem;
- entrada resumida;
- saida funcional;
- evidencias;
- bloqueios;
- causa-raiz;
- proxima acao;
- custo quando houver;
- resultado consolidado;
- historico de execucoes.

Detalhes tecnicos como request, response, prompt, schema, tokens, stack trace e ids de execucao devem ficar em area tecnica colapsada.

## 13. Testes esperados

Declare quais testes precisam existir ou ser ajustados.

### Backend

- start da primeira etapa;
- pending por etapa;
- callbacks de request/response;
- persistencia de auditoria;
- calculo de custo quando houver IA;
- bloqueio por contrato invalido;
- avanco automatico permitido;
- impedimento de contaminacao da `resposta_final` por envelope tecnico.

### Executor

- consumo exclusivo do `pending`;
- processor registrado no catalogo;
- etapa sem dependencia direta de outra etapa concreta;
- prompt/schema carregados de arquivo;
- request enviado antes da chamada externa;
- response registrado apos retorno;
- schema validado;
- saida funcional limpa.

### Frontend

- chamada do endpoint administrativo correto;
- cards em ordem real;
- status traduzido para linguagem operacional;
- resultado funcional separado de detalhes tecnicos;
- request/response bruto em area colapsada.

## 14. Checklist antes de considerar o arquivo pronto

Use esta lista antes de usar o arquivo no `script-prompt.md`.

- [ ] O objetivo do pipeline esta claro e ligado a venda, decisao comercial ou reducao de risco.
- [ ] O limite de dominio esta declarado.
- [ ] Todas as etapas oficiais estao listadas em ordem.
- [ ] Cada etapa tem objetivo, entrada, processamento, saida, artefatos, conclusao, bloqueio e proxima etapa.
- [ ] Nenhuma etapa futura foi citada sem contrato completo.
- [ ] O backend e declarado como fonte de verdade do avanco.
- [ ] O modulo executor real esta identificado.
- [ ] O endpoint `pending` de cada etapa executada fora do backend esta definido.
- [ ] Callbacks de request/response estao definidos quando houver IA ou integracao externa.
- [ ] A tabela de auditoria e os campos principais estao definidos.
- [ ] `resposta_final` e artefato final estao protegidos contra metadados tecnicos.
- [ ] Gates funcionais foram definidos.
- [ ] O que aparece na tela esta claro.
- [ ] Os testes esperados foram declarados.
- [ ] As lacunas conhecidas foram marcadas como bloqueantes, nao resolvidas por inferencia.

## 15. Modelo resumido para copiar

```markdown
# Pipeline <nome> <versao>

## Objetivo geral

<Explique a transformacao de negocio e por que ela ajuda venda, decisao comercial ou reducao de risco.>

## Identificacao

| Campo | Valor |
|---|---|
| Pipeline | `<nome>` |
| Versao | `<v1>` |
| Modulo executor | `<modulo>` |
| Pacote backend | `<pacote>` |
| Pacote executor | `<pacote>` |
| Objeto associado | `<objeto>` |
| Chave do objeto | `<chave>` |
| Tabela de auditoria | `<tabela>` |
| Endpoint interno base | `<endpoint>` |
| Endpoint administrativo base | `<endpoint>` |

## Limite de dominio

<Declare o que este pipeline faz e o que ele nao deve fazer.>

## Visao macro

1. <Inicio>
2. <Backend cria job>
3. <Executor consome pending>
4. <Executor reporta request/response/resultado>
5. <Backend valida e decide avanco>
6. <Tela mostra relatorio persistido>

## Ordem oficial das etapas

| Ordem | Codigo | Nome de negocio | Executor | Proxima etapa | Avanco |
|---:|---|---|---|---|---|
| 1 | `<codigo>` | `<nome>` | `<executor>` | `<proxima>` | `<automatico/manual/fim>` |

## Separacao entre negocio e auditoria

### Dado funcional

- <campos e artefatos de negocio>

### Envelope tecnico/auditoria

- jobId
- stageExecutionId
- status
- request
- response
- prompt
- schema
- modelo
- tokens
- custo
- erro tecnico

Regra: `resposta_final`, saida funcional e artefato final nao podem conter envelope tecnico.

## Etapas

### Etapa 1 — `<codigo>` — <nome>

#### Objetivo

<Objetivo de negocio da etapa.>

#### Entradas obrigatorias

- <entrada 1>
- <entrada 2>

#### Processamento esperado

- <acao 1>
- <acao 2>

#### Saida funcional estruturada

- <campo funcional 1>
- <campo funcional 2>

#### Artefatos auditaveis

- <artefato 1>

#### Criterio de conclusao

<Quando conclui.>

#### Criterio de bloqueio

<Quando bloqueia, causa-raiz, impacto e acao recomendada.>

#### Proxima etapa permitida

<Proxima etapa ou acao humana.>

#### Dados de tela

<O que o usuario precisa ver.>

## Endpoints

### Internos

- `GET <base>/<etapa>/stage-executions/pending`
- `POST <base>/<etapa>/stage-executions/{stageExecutionId}/recebeRequest`
- `POST <base>/<etapa>/stage-executions/{stageExecutionId}/recebeResponse`

### Administrativos

- `<endpoints de start, status, detalhe e resultado>`

## Persistencia e auditoria

<Tabela, campos, separacao entre auditoria e resposta funcional.>

## Gates

<Gates funcionais e regras de avanco.>

## Executor

<Como o modulo executor consome pending, processa, registra request/response e devolve saida funcional.>

## Tela e relatorio

<Dados persistidos que a tela precisa mostrar.>

## Testes esperados

<Backend, executor e frontend.>

## Lacunas bloqueantes

- <Lacuna que impede implementacao por inferencia>
```
