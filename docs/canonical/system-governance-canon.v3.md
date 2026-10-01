# Marketing Hub — System Governance Canon v3

> Changelog v3
> - estabelece validação obrigatória exclusivamente por agentes ou pelo próprio mercado
> - proíbe dependência operacional de entrevistas, recrutamento, testes privados e opiniões solicitadas
> - separa homologação sintética de prova comercial baseada em vendas e contribuição
> - dá precedência constitucional às regras globais expressamente destacadas neste documento
>
> Changelog v2
> - adiciona regra de precedência canônica
> - adiciona critérios explícitos para criar novos cânones de domínio
> - adiciona matriz curta de ownership
> - vincula mudanças cross-domain a ADRs

> [!IMPORTANT]
> **REGRA GLOBAL — TODO PRODUTO OU PROJETO É VALIDADO POR AGENTES OU PELO PRÓPRIO MERCADO.**
> O Marketing Hub não depende de entrevistas, recrutamento, convites para testar, leituras privadas,
> grupos de opinião, pedidos de avaliação ou qualquer ação que exija encontrar pessoas para validar
> um produto. Antes do mercado, agentes e testes determinísticos comprovam prontidão. No mercado,
> comportamento voluntário, vendas reconciliadas e contribuição comprovam demanda e viabilidade.

## 1. Propósito

- Estabelecer um documento-mãe curto que orienta como o Marketing Hub deve preservar a unidade de regras entre backend (`backend/ads-service`), frontend, workers (por exemplo `ai-worker`, `facebook-ads-worker`, `video-management-service`) e serviços satélites (`lead-portal`, `email-service`, `image-watermark-service`, `image-zipper-service`, `lead-portal-payments-service`).
- Criar uma "constituição" de governança para reduzir o drift observado nas evoluções independentes de módulos.
- Definir o roteiro para cânones específicos por domínio, mantendo este arquivo como referência global mínima.

## 2. Escopo

| Cobre agora | Ainda não cobre |
| --- | --- |
| Princípios que determinam onde mora a fonte da verdade, como contratos de domínio, modelos do backend, schemas canônicos e decisões operacionais relevantes. | Regras detalhadas de cada fluxo, como publicação de anúncios, validações campo a campo de formulários, sequências completas do Lead Portal ou contratos externos específicos. |
| Critérios para detectar divergências entre frontend, backend, workers e serviços auxiliares. | Diagramas de arquitetura completos, design de infraestrutura, listas de endpoints ou tabelas completas. |
| Estrutura futura da família de cânones e como evoluir versões. | Decisões irrevogáveis sobre topologias, ferramentas ou refactors abrangentes. |

## 3. Princípios Canônicos Globais

1. **Fonte de verdade explícita.** Cada regra operacional relevante deve apontar para um contrato oficial e identificável.
2. **Backend e domínio decidem; interfaces consomem.** Frontend, workers e integrações não devem reinventar regra de negócio já decidida no domínio.
3. **Workers e integrações emitem fatos.** Serviços assíncronos e conectores externos devem produzir fatos, resultados e eventos; a interpretação final da regra pertence ao domínio.
4. **Flags derivadas não são verdade primária.** Rótulos de UI, atalhos de leitura, campos calculados e indicadores transitórios são projeções, não a regra original.
5. **Contratos entre módulos são explícitos e versionados.** Nenhum módulo pode depender de campos implícitos, estados informais ou convenções não registradas.
6. **Mudanças relevantes exigem teste e documentação correspondente.** Regra operacional sem teste e sem contrato atualizado é candidata imediata a drift.
7. **LHM é determinístico por definição canônica.** O Landing HTML Module (LHM) é responsável pela renderização previsível do HTML final de landing pages a partir de artefatos/contratos canônicos; não é camada de ideação livre de copy.
8. **Evolução visual via artefato canônico.** Ajustes de estética/tema para landing pages devem ser modelados em artefato canônico estruturado (ex.: `landingPageDesignPreset`) consumido pelo LHM; evitar etapa de geração livre de HTML/JS fora do contrato.
8.1. **Paridade obrigatória em geração paralela de landing.** Quando houver geração de múltiplas variantes públicas de HTML para o mesmo experimento (ex.: `deterministic` e `ai`), todas devem usar o mesmo snapshot de entrada canônica e o mesmo conjunto de validações obrigatórias.
9. **Arquitetura robusta orientada a vendas.** Robustez técnica, validações e contratos existem para sustentar resultado comercial: melhorar conversão, reduzir fricção e manter continuidade de mensagem entre criativo, landing e oferta. Toda decisão arquitetural deve explicitar seu impacto em receita (CVR, CPL/CPA e avanço de funil), não apenas conformidade técnica.
10. **Stack mandatória para módulos de apoio + deploy rastreável.** Módulos de apoio/satélites devem adotar baseline **Spring Boot + Java + Maven** e manter workflow CI/CD dedicado com teste, build e deploy por **disparo manual (`workflow_dispatch`)** além dos gatilhos automáticos necessários.
11. **Exceções capturadas preservam ponto de falha.** Todo tratamento de exceção que captura, converte ou relança erro deve registrar log com contexto operacional e a exceção completa para manter stack trace e permitir diagnóstico de causa-raiz. Mensagens resumidas de causa raiz podem ser retornadas ao usuário/API, mas não substituem o log completo no ponto da captura.
12. **Sem metainstrução no artefato final.** Qualquer texto técnico/operacional vazado em conteúdo final ao usuário (copy, CTA, FAQ, HTML publicado) deve bloquear publicação com erro explícito de contrato e apontamento do campo literal rejeitado.
13. **Wireframe só aceita estilos existentes em definições canônicas.** O artefato de wireframe não pode introduzir `style`, `surfaceStyle`, `contrastMode`, `layoutPreset` ou variação visual fora do conjunto previsto nas definições canônicas vigentes. Qualquer estilo inexistente nas definições deve ser rejeitado em validação de contrato com indicação literal do valor inválido e da definição esperada.
14. **Classes aplicadas do wireframe ficam somente em `estilos`.** No artefato `landingPageWireframe`, as categorias `estrutura`, `posicao`, `layout` e `mistas` existem apenas dentro de `definicoes` para declarar classes canônicas. Dentro de `pagina.head`, `pagina.corpo`, `pagina.corpo.secoes[]`, `elementosSeccao[]` e `elementosInternos[]`, nomes de classes aplicadas devem aparecer exclusivamente em `estilos[]`, sem duplicação em campos categorizados.
15. **Liquibase aplicado é imutável.** Changelog ou `changeSet` Liquibase que já pode ter sido executado em qualquer ambiente não pode ser alterado em conteúdo, `id`, `author`, caminho, ordem interna ou SQL/YAML. Alterar migration aplicada muda o checksum registrado em `DATABASECHANGELOG` e pode derrubar o backend no bootstrap. Toda correção, evolução, seed, reparo de dado ou ajuste comercial deve nascer em novo changelog incremental. A única exceção é restaurar o arquivo aplicado ao conteúdo original quando uma alteração indevida já tiver sido feita e o objetivo for recuperar o checksum registrado.
16. **Liquibase MySQL 5.7 sem auto-subconsulta em tabela-alvo.** Changelogs que executam `UPDATE` ou `DELETE` não podem consultar a mesma tabela-alvo em subconsultas usadas no `WHERE` ou `SET` para validar idempotência/conflito, porque o MySQL 5.7 pode bloquear a migração com erro 1093 (`You can't specify target table ... for update in FROM clause`). A regra canônica é modelar essas validações com `LEFT JOIN ... IS NULL`, tabela derivada materializada em nível seguro ou comandos separados, revisando explicitamente cada changelog antes do PR.
17. **Comentários Java em português.** Comentários escritos dentro de classes Java devem estar em português, incluindo a responsabilidade básica da classe, a explicação breve de métodos e comentários internos necessários para esclarecer decisões de implementação.
18. **Rotinas agendadas pertencem ao módulo executor.** Ajustes de execução operacional de rotinas — cron, polling, frequência, janela de execução, pausa/retomada, retries locais e decisão de quando rodar — devem residir no módulo que executa a rotina. O backend principal entrega contratos/dados, expõe pendências e recebe status/resultados; ele não deve virar orquestrador de agendamento de módulos externos. Exceção: quando houver solicitação explícita de tela administrativa, o backend pode persistir e expor a configuração/comando usado pela UI, mantendo a execução agendada no módulo executor.
19. **Backend principal não executa OpenAI.** O backend (`backend/ads-service`) não deve chamar APIs da OpenAI, criar batches, fazer polling de resposta de modelo ou manter clientes runtime de IA em fluxos de negócio. Quando uma ação precisar de IA, o backend deve apenas persistir a solicitação/execução, expor endpoint canônico `pending`, receber `claim`/status/resultado do módulo executor e consolidar o artefato. A chamada à OpenAI, retry operacional, timeout, prompt final enviado e validação primária da resposta pertencem ao AI Worker ou ao worker executor responsável pela etapa.
20. **Entrega autorizada por Pull Request.** Pedidos de implementação, correção ou integração autorizam validação local, PR, revisão, merge e acompanhamento dos workflows/deploys, sem novo clique ou pedido de PR. Análises sem alteração não exigem PR; restrições explícitas de somente local ou não publicar prevalecem. Usar conector GitHub, API oficial ou `gh` com identidade autorizada. Consultar revisões reais, não aprovar o próprio PR, não usar bypass e respeitar revisões e checks obrigatórios. Branch enviada sem integração não representa entrega concluída. Esta regra acompanha a autonomia já registrada no cânone de aperfeiçoamento dos agentes em 21/09/2026.
21. **Frontend administrativo acessível ao Codex.** O ambiente operacional deve declarar a URL pública do frontend administrativo do Marketing Hub para que fluxos comerciais, experimentos, PDEs, campanhas, vídeos e validações pós-deploy sejam executados e verificados pela mesma superfície usada pelo usuário. Endpoints oficiais do backend podem apoiar diagnóstico, automação e investigação, mas não devem substituir a validação visual pela tela quando a funcionalidade administrativa existir. Se a URL não estiver disponível, essa limitação deve ser registrada antes de concluir uma decisão comercial dependente da experiência administrativa.
22. **Validação exclusivamente por agentes ou mercado.** Nenhum projeto, produto, evolução, BPM, gate, plano ou recomendação pode depender de recrutar pessoas, realizar entrevistas, pedir testes privados ou solicitar opiniões. Agentes e testes determinísticos homologam antes do mercado; somente comportamento voluntário do mercado comprova demanda, venda, valor entregue e viabilidade econômica.

## 3.1 Regra global de validação por agentes ou mercado

Decisão do usuário em 28/09/2026: esta é uma regra constitucional para **todo projeto, produto atual
ou futuro e toda evolução do Marketing Hub**.

- O sistema não deve criar, recomendar nem aguardar entrevistas, recrutamento, convites a pessoas,
  leitura privada, grupo focal, teste moderado ou não moderado com participantes externos, pesquisa
  de opinião, pedido de feedback ou atividade equivalente.
- Nenhuma dessas ações pode aparecer como gate, pendência, tarefa do usuário, próximo passo,
  critério de aceite ou justificativa para interromper um fluxo. Uma execução não pode entrar em
  `WAITING_HUMAN` por falta de participante, entrevistado, avaliador ou opinião externa.
- Antes da exposição ao mercado, o produto é pesquisado e homologado por agentes, fontes públicas,
  cenários sintéticos explicitamente identificados, testes determinísticos e revisão independente.
  Essa evidência comprova prontidão, coerência, segurança e mensuração; nunca deve ser apresentada
  como cliente real, preferência humana, satisfação, demanda ou venda.
- A validação externa acontece pelo **próprio mercado**, em canal público/comercial autorizado e sem
  recrutar participantes. São evidências válidas os comportamentos voluntários e atribuídos do funil:
  impressão, clique, sessão, uso, avanço, checkout, pagamento reconciliado, entrega, recompra,
  cancelamento, suporte iniciado pelo cliente e reembolso.
- Engajamento isolado não valida negócio. Demanda comercial exige venda paga e reconciliada; escala
  exige contribuição positiva depois de mídia, taxas, entrega, reembolso e demais custos variáveis.
  Os experimentos devem preservar orçamento, critérios de parada e segregação de tráfego interno.
- Aprovações do proprietário do sistema sobre preço, orçamento, publicação, campanha e gasto
  continuam válidas: são decisões de governança, não testes com público. Atendimento a compradores
  também continua válido, mas não pode ser convertido em obrigação de pedir opinião, depoimento ou
  participação em pesquisa.
- Fontes humanas históricas ou espontâneas já existentes podem ser analisadas como insumo, com
  consentimento e anonimização quando aplicável, mas o Marketing Hub não deve criar trabalho para
  obter novas pessoas ou respostas. Menções históricas em documentos permanecem apenas para
  auditoria e não são executáveis.
- Todo cânone de domínio, processo, prompt, agente e plano futuro deve aplicar esta regra. Uma
  instrução divergente é drift e deve ser corrigida antes da execução; exceção só pode nascer de
  nova decisão explícita do usuário e nova versão deste cânone.

### Alternativas consideradas

| Alternativa | Benefício | Risco, custo e aderência | Decisão |
| --- | --- | --- | --- |
| Recrutar pessoas para entrevistas, testes ou opiniões | Pode trazer relato qualitativo direto | Depende de capacidade operacional inexistente, não escala e paralisa o produto antes de testar venda | Proibida |
| Usar somente agentes e declarar o produto validado | É rápido, barato e totalmente automatizável | Agentes não comprovam disposição a pagar, receita, entrega ou lucro | Insuficiente como prova comercial |
| Homologar com agentes e validar comercialmente no mercado | Automatiza a prontidão e mede comportamento, venda e contribuição reais | Exige instrumentação, orçamento controlado e leitura disciplinada do funil | Escolhida |

## 3.2 Regra global de exclusividade de artefatos (todo o sistema)

- Todo artefato gerado direta ou indiretamente por fluxos oficiais do sistema é **exclusivo do contexto de origem** (por exemplo `experimentId`, `leadId`, `campaignId` ou equivalente canônico do domínio).
- Para um artefato ser classificado como **não exclusivo**, ele não pode ter sido produzido em nenhuma etapa de pipeline/fluxo oficial vinculada a um contexto específico.
- Reuso de artefatos entre contextos distintos só é permitido quando houver contrato canônico explícito de compartilhamento no domínio correspondente.

## 4. Precedência Canônica

Em caso de conflito, a precedência deve ser:

1. **Regras constitucionais globais expressamente destacadas neste documento**
2. **Schema ou contrato canônico publicado**
3. **Cânone de domínio correspondente**
4. **Demais regras do System Governance Canon**
5. **Implementação atual no código**
6. **Comportamento observado em frontend, worker ou integração externa**

Regra prática:

- Implementação divergente não redefine a regra; ela sinaliza drift.
- Ausência de contrato explícito não autoriza cada módulo a decidir por conta própria.
- Quando dois documentos canônicos conflitarem, o conflito deve ser registrado explicitamente até ser resolvido.
- Plano, prompt ou cânone de domínio não pode transformar entrevista, recrutamento, teste privado ou
  opinião solicitada em exceção à regra constitucional da seção 3.1.

## 5. Critérios para identificar risco de drift

- **Mesma regra repetida em múltiplos módulos.** Quando a mesma elegibilidade, bloqueio ou transformação aparece em backend, frontend e worker, há risco alto.
- **Bloqueios condicionais diferentes entre camadas.** Se UI e backend calculam permissões, prontidão ou status de formas diferentes, existe drift em andamento.
- **Workers inferindo elegibilidade.** Quando um worker passa a aprovar, reprovar ou liberar ações com heurísticas locais, ele virou dono informal da regra.
- **Ausência de contrato único para estados encadeados.** Fluxos que atravessam vários módulos precisam de estados e transições formais.
- **Flags múltiplas para o mesmo conceito.** Dois nomes, dois campos ou duas flags para representar a mesma ideia são sinal de modelo mal consolidado.
- **Cópias locais de modelo.** Toda vez que um módulo redefine localmente entidades ou estados já existentes em outro lugar, o risco de drift cresce.
- **Dependência de pessoas externas para validar.** Entrevista, recrutamento, leitura privada, teste
  solicitado ou opinião externa em gate, tarefa ou próximo passo viola a execução autônoma e deve
  ser substituída por homologação de agentes ou experimento de mercado instrumentado.

## 6. Quando criar um novo cânone de domínio

Criar um novo cânone de domínio quando um assunto:

- possui estado próprio
- possui regras próprias
- envolve mais de um módulo
- já gerou drift ou tem alto risco de gerar drift
- precisa de contrato, tabela de decisão, transições ou testes específicos

Regra prática:

- Se o tema cabe apenas como princípio global, ele fica neste documento.
- Se o tema exige estados, comandos, invariantes ou contratos próprios, ele deve sair deste documento e ganhar cânone próprio.

## 7. Ownership canônico

| Tema | Dono da regra | Consumidores típicos |
| --- | --- | --- |
| Regras operacionais de domínio | backend / domínio correspondente | frontend, workers, integrações |
| Regras de negócio do domínio **MOIS** | **módulo MOIS (`/mois`)** | backend principal (gateway/contrato), frontend, workers |
| Schemas e contratos de decisão | domínio + backend responsável | todos os consumidores do contrato |
| Artefatos canônicos de landing (`landingPageCopy`, `landingPageWireframe`, `landingPageDesignPreset`) | backend / domínio de experimentos | ai-worker, frontend administrativo, LHM |
| Contrato visual runtime do LHM (`landingPageDesignPreset.lhmRuntime.baseCss`, `sectionPresets.surfaceStyle`, `sectionPresets.contrastMode`) | backend / domínio de experimentos | LHM, ai-worker, frontend administrativo |
| Matriz de conversão visual e confiança (hierarquia, prova, acessibilidade, sinais legais) | cânone de experimentos + backend validador | ai-worker, LHM, frontend |
| Projeções de UI | frontend | usuário final |
| Fatos externos e resultados assíncronos | workers / integrações | backend / domínio |
| Execução runtime de IA/OpenAI | AI Worker ou worker executor da etapa | backend / domínio, frontend |
| Solicitações, pendências e resultados persistidos de IA | backend / domínio correspondente | AI Worker, frontend, relatórios |
| Governança global do sistema | `system-governance-canon` + ADRs relevantes | todo o projeto |

### 7.1 Regra arquitetural mandatória para MOIS

- O **backend principal** não deve conter regra de negócio específica do MOIS.
- Para MOIS, o backend principal atua como:
  - gateway HTTP;
  - camada de contrato/validação;
  - camada de leitura/escrita de dados quando aplicável.
- A orquestração, decisões, cálculo de score, transições e políticas de domínio devem residir no **módulo MOIS**.

## 8. Áreas candidatas a futuros cânones específicos

| Domínio sugerido | Justificativa breve |
| --- | --- |
| **Experiments & Activation** | Abrange nicho → hipótese → targeting → ad sets → creatives → métricas. Exige estados e contratos próprios. |
| **Lead Capture & Portal / Payments** | Lead Portal e pagamentos compartilham eventos, submissions, pacotes, compras e reenvios. |
| **Media Asset Lifecycle** | Watermark, zipper, distribuição, miniaturas e entrega exigem lifecycle e contratos claros. |
| **Ads Delivery & Channel Integrations** | Integrações com Meta e canais pagos têm dependências externas, estados e contratos próprios. |
| **AI Prompt & Worker Governance** | Workers de IA compartilham obrigações de prompt, modelo, auditoria, orçamento e versionamento. |
| **Vitrines & Content Entitlements** | Regras de acesso, plano, papel, magic links e visibilidade de conteúdo merecem modelo próprio. |

## 9. Regras de evolução

- Este documento deve continuar enxuto e estável.
- Detalhes operacionais devem migrar para cânones de domínio assim que surgirem estados e regras próprias.
- Mudanças que alterem princípios, escopo, precedência, ownership ou estrutura da família de cânones exigem nova versão.
- Mudanças cross-domain ou arquiteturalmente significativas devem gerar ADR correspondente.
- Conflitos entre documentos devem ser registrados explicitamente, nunca escondidos.
- Toda alteração que muda comportamento em produção deve alinhar: código, testes, contrato e cânone relevante.
- Todo novo módulo de apoio deve nascer com workflow CI/CD próprio contendo, no mínimo, etapa de testes, build de imagem e deploy com acionamento manual (`workflow_dispatch`).

## 10. ADRs

Decisão estrutural desta versão:

- [ADR — validação exclusivamente por agentes ou mercado](../adr/2026-09-28-validacao-agentes-ou-mercado.md)

Usar ADR quando a decisão:

- afeta mais de um domínio
- muda precedência entre fontes de verdade
- altera ownership de uma regra importante
- introduz ou remove um mecanismo estrutural relevante
- cria exceções permanentes ao cânone atual

Regra prática:

- cânone descreve a regra estável
- ADR explica por que a decisão estrutural foi tomada e quais consequências ela traz

## 11. Estrutura-alvo da família de cânones

```text
docs/canonical/
├─ system-governance-canon.v3.md              # documento-mãe
├─ procedimento-experimento-canon.v1.md        # procedimento ponta a ponta dos experimentos
├─ pipeline-operacional-canon.v1.md            # padrão operacional de pipelines, etapas e filas
├─ experiments-decision-schema.v1.json        # schema machine-readable para validações automáticas
├─ lead-capture-canon.v1.md                   # fluxos do Lead Portal, packages e payments
├─ media-packages-canon.v1.md                 # lifecycle watermark → zip → email → entrega
├─ ads-integrations-canon.v1.md               # contratos Meta / canais pagos
├─ facebook-campaign-publication-canon.v1.md   # prontidão, liberação e funil do Facebook Ads Worker
├─ mois-worker-canon.v1.md                    # cânone único do worker MOIS (fluxo, contratos e OpenAI)
└─ <domínio>-decision-schema.v1.json          # schemas específicos quando necessário
```

> Cada novo documento deve declarar propósito, limites, contratos oficiais e tabela de estados, referenciando explicitamente os módulos que implementam as regras descritas.
