# Pipeline operacional — padrão canônico de implementação e interface v1

## 1. Objetivo

Este documento define o padrão canônico para criar, implementar, operar e apresentar pipelines no Marketing Hub, usando o GeraLanding como referência principal de arquitetura e experiência de usuário.

Um pipeline no Marketing Hub não é apenas uma sequência técnica de jobs. Ele é um mecanismo de transformação comercial: recebe um contexto de mercado, dor ou experimento; executa etapas especializadas; materializa artefatos verificáveis; e conduz o usuário para a próxima decisão necessária para gerar vendas.

Todo pipeline deve preservar o eixo central do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

## 2. Princípios obrigatórios

1. **Pipeline existe para avançar uma venda ou uma decisão comercial.** Etapas sem impacto claro na criação, validação, publicação, mensuração ou escala de uma oferta não devem entrar no fluxo.
2. **Cada etapa deve ter uma responsabilidade única.** A etapa recebe entradas claras, executa uma transformação específica e entrega um artefato persistido e auditável.
3. **A tela deve orientar o usuário pelo fluxo, não expor complexidade técnica.** Informações técnicas ficam disponíveis apenas quando ajudam diagnóstico, configuração segura ou suporte.
4. **O usuário deve ver estado, causa e próxima ação.** Toda etapa precisa comunicar se está pronta, em execução, concluída, bloqueada ou com erro, e qual ação destrava o avanço.
5. **O backend é a fonte de verdade.** Frontend, workers e módulos auxiliares consomem e gravam estado por endpoints do backend; nenhum módulo acessa banco diretamente fora do backend principal.
5.1. **Backend não executa OpenAI.** Em etapas que usam IA, o backend não chama OpenAI nem espera resposta de modelo em requisição de tela. Ele registra a solicitação, persiste estado/auditoria, publica pendências por `pending` e recebe callbacks de execução do AI Worker.
6. **Artefatos finais não podem ser contaminados por metadados técnicos.** Comentários operacionais, flags internas, placeholders e textos de debug devem ficar fora do conteúdo publicado.
7. **Automação deve reduzir esforço, não esconder falhas.** Encadeamentos automáticos são obrigatórios quando a próxima etapa depende apenas de sucesso da etapa anterior; bloqueios devem mostrar causa-raiz e ação recomendada.

## 3. Regras universais e limites de reutilização

Esta seção separa o que é regra obrigatória para qualquer pipeline do que é característica específica do GeraLanding. O GeraLanding continua sendo o exemplo principal, mas novos pipelines devem adaptar nomes, artefatos e executores ao seu próprio domínio.

### 3.1 Regras universais para qualquer pipeline

Estas regras são obrigatórias para qualquer pipeline operacional do Marketing Hub:

1. **Backend como fonte de verdade:** o backend principal mantém contrato, estado, validação, auditoria e persistência consolidada; frontend, workers e serviços auxiliares devem operar por APIs do backend.
2. **Etapa com responsabilidade única:** cada etapa executa uma transformação clara, com objetivo comercial próprio, sem acumular responsabilidades de outras etapas.
3. **Entrada, saída e auditoria explícitas:** toda etapa declara o que recebe, o que produz e quais evidências permitem explicar a execução, a falha e a decisão seguinte.
4. **Estados mínimos:** toda execução deve ser traduzível para estados operacionais mínimos como fila, execução, conclusão, falha, bloqueio, cancelamento ou substituição.
5. **Contrato de tela:** a UI deve mostrar contexto, status, artefato, bloqueio e próxima ação em linguagem de negócio, deixando diagnóstico técnico em nível secundário.
6. **Avanço automático ou manual declarado:** cada transição precisa informar se o backend avança automaticamente após sucesso ou se exige decisão humana explícita.
7. **Bloqueio por falha ou contrato inválido:** falha técnica, ausência de entrada, resposta inválida, artefato incompleto ou contrato divergente devem impedir avanço e publicação até a causa-raiz ser corrigida.

### 3.2 Regras específicas do GeraLanding, não copiáveis automaticamente

As regras abaixo pertencem ao domínio de geração de landing pages. Elas podem servir como referência, mas não devem ser copiadas automaticamente para pipelines de outros domínios sem adaptação explícita:

1. **HTML provisório:** é um artefato intermediário útil no GeraLanding para visualizar evolução de landing page, mas outros pipelines devem definir o artefato intermediário adequado ao seu domínio.
2. **`html_geralanding`:** é um campo consolidado específico do GeraLanding e não deve virar nome padrão de outros pipelines.
3. **`landing_page_html`:** não é responsabilidade do GeraLanding; representa o HTML publicável da landing page em uma etapa/módulo de publicação ou consolidação final, e outros domínios devem usar nomes que expressem seu próprio artefato final.
4. **Quality review visual:** é gate específico para avaliar qualidade visual/comercial de landing page; outros pipelines devem definir gates compatíveis com o risco e o resultado do seu artefato.
5. **Presets de design:** pertencem ao fluxo de composição visual do GeraLanding; novos pipelines só devem ter presets quando isso fizer sentido para o domínio.
6. **Renderização e screenshots:** são necessários para validar landing pages renderizadas; outros pipelines devem substituir por evidências apropriadas, como prévia textual, simulação, validação de contrato, arquivo final ou métrica operacional.


## 3.3 Pontos de sucesso do GeraLanding obrigatórios para novos pipelines

Os pontos abaixo deixam de ser apenas aprendizado do GeraLanding e passam a ser regra operacional para qualquer pipeline, adaptando nomes e artefatos ao domínio:

1. **Contrato fechado antes da execução:** toda etapa precisa ter entrada, saída, artefatos, critérios de bloqueio, critérios de conclusão e próxima etapa permitida documentados antes de ser enfileirada.
2. **Backend decide avanço:** o executor nunca decide sozinho a transição de negócio; ele executa, valida e reporta resultado. O backend persiste o resultado e enfileira a próxima etapa somente quando o sucesso funcional estiver comprovado.
3. **Endpoint interno por etapa:** cada etapa consumida por worker deve ter `pending` próprio e callbacks próprios para registrar request/prompt quando houver, resposta/resultado, erro, custo e evidências.
4. **Executor por etapa plugável:** o módulo executor deve manter núcleo genérico separado das etapas concretas; etapa concreta não importa outra etapa concreta e consome resultados anteriores por contrato persistido ou artefato auditável.
5. **Prompt/schema versionados quando houver IA:** prompt operacional, schema de saída e validação ficam versionados no executor, não hardcoded em service de backend nem dispersos na UI.
6. **Auditoria bruta separada do artefato funcional:** request bruto, response bruto, metadados, logs técnicos e flags internas não podem contaminar o artefato final consumível/publicável.
7. **Gate funcional antes de risco comercial:** antes de publicação, materialização, gasto de mídia, oferta ou encerramento como sucesso, deve existir gate compatível com o domínio, apontando causa-raiz, impacto comercial e etapa recomendada de correção.
8. **Relatório por dados persistidos:** a tela deve explicar andamento, decisão, bloqueio, evidências, custo e próxima ação por dados persistidos, não por leitura de logs técnicos.
9. **Catálogo de etapas consistente:** é proibido retornar ou persistir `nextStageCode` para etapa sem contrato completo, endpoint `pending`, processor/handler registrado e persistência de execução.
10. **UI simples e orientada à decisão:** a tela deve mostrar o essencial para o usuário decidir o próximo passo, mantendo detalhes técnicos em nível secundário.

## 4. Modelo conceitual obrigatório

Todo pipeline deve ser descrito por quatro camadas:

| Camada | Responsabilidade | Exemplo no GeraLanding |
|---|---|---|
| Contrato canônico | Define ordem, etapas obrigatórias, aliases, responsabilidades e campos estruturais. | `landing-page-wireframe` → `landing-page-copy` → `landing-page-image-planning` → `landing-page-image-generation` → `landing-page-design-preset` → `landing-page-deliverables`. |
| Configuração operacional | Permite ajuste seguro sem quebrar contrato. | Modelo OpenAI por etapa, descrição operacional, ativação controlada. |
| Execução | Registra jobs, status, prompt, resposta, erro, evidência e artefatos intermediários. | `gera_landing_stage_execution` com prompt, resposta, HTML provisório e auditoria. |
| Artefato consolidado | Estado funcional atual usado pela próxima etapa, tela ou publicação. | `experiment.landing_page_wireframe`, `experiment.landing_page_copy`, `experiment.landing_page_image_assets`, `experiment.html_geralanding`. |

## 5. Desenho de uma etapa

Cada etapa canônica deve declarar, no mínimo:

| Campo | Obrigatório | Regra |
|---|---:|---|
| Código operacional | Sim | Deve ser estável, legível e versionável. Ex.: `landing-page-copy`. |
| Nome de negócio | Sim | Deve explicar a transformação para o operador. Ex.: “Copy da Landing”. |
| Posição | Sim | Deve representar a ordem real de execução. |
| Objetivo comercial | Sim | Deve responder: “como esta etapa ajuda a vender ou validar a venda?”. |
| Entrada principal | Sim | Artefatos, dados e contexto necessários para execução. |
| Saída principal | Sim | Artefato persistido que a próxima etapa consome. |
| Critério de conclusão | Sim | Condição objetiva para marcar sucesso. |
| Critério de bloqueio | Sim | Situações em que não deve avançar. |
| Próxima etapa | Quando aplicável | Pode ser automática ou exigir decisão humana explícita. |
| Executor | Sim | Backend principal, Worker AI, coletor, serviço externo ou ação humana assistida. |
| Endpoint público/administrativo | Quando houver UI | Endpoint usado pela tela para iniciar, listar e detalhar. |
| Endpoint interno | Quando houver worker | Endpoints de `pending`, recebimento de prompt e recebimento de resposta. |
| Persistência | Sim | Tabelas e campos consolidados. |
| Auditoria | Sim | Dados mínimos para explicar o que aconteceu. |

## 6. Padrão de arquitetura inspirado no GeraLanding

### 6.1 Backend

O backend deve concentrar:

1. contrato da etapa;
2. validação de pré-condições;
3. criação da execução;
4. persistência de status;
5. aplicação do resultado no domínio;
6. decisão de enfileirar a próxima etapa;
7. exposição de endpoints administrativos;
8. exposição de endpoints internos para workers.

Para pipelines por etapa, o pacote deve seguir a separação funcional do GeraLanding:

```text
com.marketinghub.<dominio>.<etapa>.web
com.marketinghub.<dominio>.<etapa>.service
com.marketinghub.<dominio>.<etapa>.model
com.marketinghub.<dominio>.<etapa>.repository
```

Quando a etapa usa Worker AI, o backend deve expor o contrato mínimo:

```text
POST /api/<contexto>/<id>/<dominio>/<etapa>/start
GET  /api/<contexto>/<id>/<dominio>/<etapa>/stage-executions
GET  /api/<contexto>/<id>/<dominio>/<etapa>/stage-executions/{idJob}
GET  /api/internal/<dominio>/<etapa>/stage-executions/pending
POST /api/internal/<dominio>/<etapa>/stage-executions/{idJob}/recebe-prompt
POST /api/internal/<dominio>/<etapa>/stage-executions/{idJob}/recebe-resposta
```

A nomenclatura pode variar por domínio, mas o padrão funcional não pode variar: iniciar, listar, detalhar, buscar pendentes, registrar prompt e registrar resposta.

### 6.2 Worker AI

Quando uma etapa usa OpenAI, o Worker AI deve seguir o padrão do núcleo por etapa:

```text
com.marketinghub.worker.openai.core.<etapa>
```

Cada etapa deve possuir configuração própria, adapter de backend, construtor de prompt, validador de resposta e handler de aplicação. O core genérico não deve depender de etapas concretas.

O worker não acessa banco. Ele busca pendências no backend pelo endpoint `pending`, faz `claim` quando o contrato da etapa exigir reserva explícita, monta prompt, chama OpenAI, valida resposta e devolve o resultado ao backend. O backend não pode substituir o worker fazendo chamada direta à OpenAI para “atalhar” uma tela ou reduzir latência; se a UI precisar de resposta rápida, o contrato deve continuar assíncrono e mostrar estado de solicitação/processamento.

Regra canônica de segredo OpenAI em módulos executores: toda etapa concreta de worker/coletor que chama OpenAI deve declarar configuração própria da etapa, aceitar variável específica de `api-key`/`api-key-file` quando necessário e reutilizar obrigatoriamente os fallbacks globais `OPENAI_API_KEY` e `OPENAI_API_KEY_FILE`. O fallback global deve ser coberto por teste de configuração ou contrato para impedir regressão em novas etapas do protocolo padrão módulo.

### 6.3 Persistência

A persistência deve separar:

1. **definição canônica**: o que o pipeline é;
2. **configuração operacional**: como a operação ajusta a execução sem quebrar o contrato;
3. **execução**: o que aconteceu em cada tentativa;
4. **artefato consolidado**: o estado atual usado pelo domínio.

Nunca misturar resposta bruta, artefato funcional e metadado técnico no mesmo campo final publicável.


### 6.4 Contrato mínimo para etapas de IA

Toda etapa ou ação administrativa que precise de IA deve seguir este contrato mínimo:

1. **Start/solicitação no backend:** a tela ou serviço chama o backend, e o backend grava uma execução `PENDING` com contexto suficiente para auditoria e relatório.
2. **Pending canônico:** o backend expõe `GET .../stage-executions/pending` ou variação canônica documentada para o AI Worker buscar trabalho.
3. **Claim/status:** o worker registra que assumiu a execução antes de chamar OpenAI quando houver risco de concorrência.
4. **Execução no worker:** somente o worker monta ou finaliza o payload runtime, chama OpenAI, controla timeout/retry operacional e valida resposta bruta.
5. **Callback de resultado:** o worker devolve resposta estruturada, payload bruto, uso/custo quando houver e erro funcional/técnico ao backend.
6. **Consolidação no backend:** o backend valida o contrato de domínio, persiste artefato/auditoria e decide a próxima transição do pipeline.

É proibido criar endpoint de backend que, ao receber uma ação de tela, chame OpenAI de forma síncrona e retorne a resposta do modelo diretamente ao frontend.

## 7. Encadeamento de etapas

O encadeamento deve seguir uma regra simples:

- se a próxima etapa depende apenas do sucesso técnico e funcional da etapa atual, o backend deve enfileirar automaticamente;
- se a próxima etapa depende de julgamento humano, aprovação comercial, publicação ou risco de gasto, a tela deve exigir ação explícita;
- falha, erro de contrato, resposta inválida ou artefato incompleto não podem iniciar próxima etapa.

No GeraLanding, a regra canônica é:

1. wireframe concluído cria automaticamente copy;
2. image planning concluído cria automaticamente image generation;
3. image generation concluído cria automaticamente design preset;
4. publicação final exige gate de qualidade/aprovação conforme o fluxo vigente.

Novos pipelines devem declarar explicitamente quais transições são automáticas e quais são manuais.

## 8. Estados mínimos de execução

Todo pipeline deve mapear os estados para linguagem operacional simples:

| Estado técnico | Estado exibido | Significado para o usuário |
|---|---|---|
| `PENDING` | Na fila | A etapa foi solicitada e aguarda processamento. |
| `RUNNING` | Em execução | O executor está trabalhando. |
| `COMPLETED` | Concluída | O artefato foi gerado e persistido. |
| `FAILED` | Falhou | Houve erro técnico ou de integração. |
| `BLOCKED` | Bloqueada | Falta entrada, aprovação ou correção de causa-raiz. |
| `CANCELLED` | Cancelada | A execução não deve mais ser usada. |
| `SUPERSEDED` | Substituída | Existe execução mais recente válida. |

Quando a base ainda não possuir todos os estados, a tela deve traduzir os estados existentes para essa semântica sem inventar progresso falso.

## 9. Formato canônico das telas de pipeline

Toda tela administrativa de pipeline deve ter cinco áreas, nesta ordem:

### 9.1 Cabeçalho de contexto

Deve responder rapidamente:

- qual pipeline está sendo operado;
- qual objeto de negócio está em foco;
- qual objetivo comercial do fluxo;
- status geral;
- principal próxima ação.

Formato recomendado:

1. título curto;
2. subtítulo orientado a negócio;
3. badges de módulo, código, status e quantidade de etapas;
4. botão principal apenas quando existir uma ação prioritária inequívoca.

### 9.2 Card de contrato operacional

Deve mostrar se o pipeline está aderente ao contrato canônico.

Conteúdo mínimo:

- status do contrato: `OK`, `ATENÇÃO` ou `BLOQUEADO`;
- etapas esperadas versus configuradas;
- divergências com causa-raiz;
- ação recomendada;
- botão de sincronização/reparo apenas quando o backend possuir operação segura e idempotente.

Esse card evita tratar sintomas na tela quando a causa-raiz é contrato divergente.

### 9.3 Visão do fluxo

Deve mostrar as etapas em ordem real, preferencialmente em cards responsivos.

A visão do fluxo deve priorizar:

1. posição;
2. nome da etapa;
3. objetivo comercial;
4. status atual;
5. principal artefato gerado;
6. próxima ação;
7. indicação de IA, humano, backend ou integração externa.

### 9.4 Detalhe da etapa

Ao abrir uma etapa, a tela deve mostrar:

- entradas usadas;
- artefato produzido;
- histórico de execuções;
- prompt/resposta quando for necessário para auditoria;
- erro com contexto operacional;
- botão de reprocessar, invalidar ou aprovar somente quando fizer sentido.

Detalhes técnicos devem ficar colapsados por padrão para não poluir a operação.

### 9.5 Área de publicação ou decisão final

Quando o pipeline gera um ativo publicável ou decisão de negócio, deve existir uma área final explícita:

- prévia do artefato final;
- checklist de qualidade;
- bloqueios de publicação;
- botão de aprovar/publicar;
- link para o resultado publicado;
- status de mensuração pós-publicação.

## 10. Padrão canônico de cards de etapa

Cada card de etapa deve ser uma unidade de decisão. O usuário deve conseguir olhar o card e saber o que aconteceu e o que fazer.

### 10.1 Estrutura visual obrigatória

Um card de etapa deve conter:

1. **Número da etapa** em destaque.
2. **Nome de negócio** da etapa.
3. **Badge de executor**: IA, Backend, Humano, Coletor ou Integração.
4. **Badge de status** com cor semântica.
5. **Objetivo comercial** em uma frase curta.
6. **Entrada principal** resumida.
7. **Saída/artefato principal** resumido.
8. **Última execução** com data/hora e resultado.
9. **Próxima ação** em botão único prioritário, quando houver.
10. **Ações secundárias** discretas: detalhes, histórico, logs, reprocessar.

### 10.2 Hierarquia visual

A hierarquia deve seguir:

1. número + nome;
2. status;
3. próxima ação;
4. artefato;
5. detalhes técnicos.

Não colocar JSON bruto, stack trace, payload completo ou logs longos no primeiro nível do card.

### 10.3 Cores semânticas

| Situação | Uso visual recomendado |
|---|---|
| Concluída | Verde, com foco em artefato disponível. |
| Em execução | Azul/índigo, com indicação de processamento. |
| Na fila | Cinza/azul claro, com expectativa de espera. |
| Bloqueada | Âmbar, com causa e ação. |
| Falhou | Vermelho, com erro resumido e caminho de correção. |
| IA | Indicador visual com brilho/ícone, sem exagero decorativo. |
| Manual/humano | Âmbar ou neutro, destacando decisão necessária. |

### 10.4 Ações no card

Cada card deve ter no máximo uma ação primária visível. Exemplos:

- “Iniciar etapa”;
- “Ver resultado”;
- “Corrigir bloqueio”;
- “Aprovar e avançar”;
- “Publicar”.

Ações destrutivas, reprocessamento e sincronização devem exigir confirmação quando puderem substituir artefato, recriar etapa, consumir crédito, publicar conteúdo ou alterar contrato.

### 10.5 Cards de configuração de pipeline

Para telas administrativas de configuração, como a tela de Pipelines, o card de etapa deve conter:

- posição;
- nome;
- código;
- pacote raiz;
- módulo executor;
- modelo OpenAI quando a etapa usa IA;
- indicação clara de etapa oficial/protegida;
- bloqueio visual de campos estruturais em pipelines oficiais.

Esse formato já é adotado na tela administrativa de pipelines e deve ser usado como base para novas telas.

## 11. Padrão de informação para cards

A informação exibida deve seguir a regra: **decisão primeiro, diagnóstico depois**.

| Nível | Informação | Deve aparecer |
|---|---|---|
| 1 | Status, nome, posição, próxima ação | Sempre visível. |
| 2 | Objetivo, entrada, saída, última execução | Sempre visível quando houver dado. |
| 3 | Modelo, endpoint, pacote, identificadores técnicos | Visível em configuração ou detalhe. |
| 4 | Prompt, resposta, payload, logs, stack trace | Colapsado, sob demanda. |

## 12. Padrão de tela mobile

Toda tela de pipeline deve ser mobile-first:

1. cards em uma coluna em telas pequenas;
2. botões grandes e claros;
3. badges quebrando linha sem sobreposição;
4. tabelas convertidas em cards quando a leitura horizontal prejudicar a decisão;
5. preview de artefato com altura controlada e opção de abrir em nova aba;
6. nenhuma ação crítica escondida apenas em hover.

## 13. Padrão de copy das telas

A linguagem da tela deve ser operacional e orientada a negócio.

Evitar:

- “payload enviado com sucesso” como mensagem principal;
- nomes internos sem explicação;
- múltiplas ações equivalentes;
- alertas genéricos como “erro inesperado”.

Preferir:

- “Copy gerada e pronta para revisar”;
- “Bloqueado porque ainda não há imagens finais”;
- “Reprocesse o planejamento de imagens antes de publicar”;
- “Contrato divergente: há etapa extra fora do cânone”.

## 14. Validações antes de avançar

Antes de permitir avanço, a etapa deve validar:

1. entradas obrigatórias presentes;
2. contrato do artefato gerado;
3. ausência de placeholders proibidos;
4. ausência de metadado técnico em artefato final;
5. consistência com a etapa anterior;
6. persistência do artefato consolidado;
7. registro de auditoria suficiente;
8. regra de próxima etapa.

No caso de HTML, JSON final, anúncio, landing, checkout ou conteúdo publicado, a validação deve bloquear contaminação por comentário técnico, debug, placeholder e texto de instrução interna.

## 15. Diagnóstico e causa-raiz

Quando uma etapa falhar, a tela e a API devem distinguir:

| Tipo de falha | Exemplo | Correção esperada |
|---|---|---|
| Falta de entrada | Não há wireframe para gerar copy. | Executar etapa anterior. |
| Contrato inválido | Resposta da IA sem campo obrigatório. | Ajustar prompt/schema/validador e reprocessar. |
| Falha de integração | Erro OpenAI, storage ou API externa. | Reprocessar com log e contexto. |
| Divergência canônica | Etapa configurada fora do contrato. | Sincronização/reparo oficial. |
| Artefato contaminado | HTML final com marcador técnico. | Corrigir origem da contaminação e bloquear publicação. |
| Qualidade comercial baixa | Oferta confusa, CTA fraco, prova ausente. | Reexecutar etapa causadora, não remendar só o final. |

A correção deve atacar a causa-raiz, não apenas o sintoma visível.

## 16. Checklist para criar novo pipeline

Antes de implementar um novo pipeline, responder e documentar:

- [ ] Qual decisão comercial ou avanço de venda esse pipeline produz?
- [ ] Quais são as etapas e por que essa ordem é obrigatória?
- [ ] Qual etapa é automática e qual exige decisão humana?
- [ ] Quais artefatos cada etapa recebe e entrega?
- [ ] Onde cada artefato consolidado é persistido?
- [ ] Qual executor processa cada etapa?
- [ ] Quais endpoints administrativos e internos existem?
- [ ] Qual tela permite iniciar, acompanhar, diagnosticar e aprovar?
- [ ] Quais cards mostram status, artefato e próxima ação?
- [ ] Quais validações bloqueiam avanço e publicação?
- [ ] Quais logs e auditorias permitem investigar causa-raiz?
- [ ] Quais testes garantem contrato, ordem e ausência de contaminação?

## 17. Checklist de PR para pipelines

Um PR que cria ou altera pipeline só está completo quando:

- [ ] documento canônico do domínio foi atualizado;
- [ ] Swagger/contrato HTTP foi atualizado quando endpoints mudaram;
- [ ] backend valida pré-condições, contrato de resposta e persistência;
- [ ] worker, quando houver, segue pacote por etapa e não acessa banco;
- [ ] frontend mostra cards com status, artefato e próxima ação;
- [ ] tela evita excesso de informação técnica no primeiro nível;
- [ ] testes cobrem ordem, bloqueios e persistência dos artefatos;
- [ ] artefatos publicáveis são testados contra metadados técnicos proibidos;
- [ ] logs de exceção capturada incluem contexto e stack trace;
- [ ] registro da tarefa foi feito em `/docs/registros` conforme o tema.

## 18. Referência canônica do GeraLanding

O GeraLanding permanece a principal referência de pipeline operacional porque demonstra:

1. separação por etapa no backend;
2. execução assíncrona por Worker AI;
3. endpoints administrativos e internos por etapa;
4. artefatos intermediários e consolidados;
5. HTML provisório por etapa;
6. publicação separada do artefato fonte puro;
7. quality review como gate comercial;
8. tela administrativa de pipeline com cards, contrato e configuração segura.

Novos pipelines devem reutilizar esse padrão de raciocínio, adaptando nomes e artefatos ao domínio, sem copiar responsabilidades indevidas nem criar atalhos que misturem etapas.
