# Catálogo Vivo — migração dos catálogos para o banco — v1

## Decisão e estado

Decisão de 16/09/2026. **Catálogo Vivo** é o nome do projeto de organização e migração
gradual dos catálogos do Marketing Hub para o banco, administrados pelo backend.
Usar esse nome e o número da etapa nas conversas e registros de entrega.

Objetivo: tornar explícito quem executa cada trabalho, com quais instruções e regras,
reduzindo inconsistências que interrompem a preparação e a operação comercial.
O catálogo de prompts armazena **texto**, identidade e versões; não é um repositório
de arquivos ou anexos. Aproveitar cadastros existentes antes de criar novas estruturas.

Estado em 16/09/2026: **Piloto Opala implementado e homologado na sandbox**,
com migração executada em MySQL 5.7 segregado. Disponibilização em produção pendente
do fluxo de publicação do usuário; nenhum ciclo de produção foi alterado.

### Entrega autorizada: Catálogo Vivo — Piloto Opala

Em 16/09/2026 foi autorizada a implementação local conjunta das etapas 1–4,
limitada ao subprocesso `opala-commercial-preparation-v1`. Reutilizar agentes,
atividades, processos, tipos e produtos existentes; acrescentar somente versões,
vínculos e auditoria necessários aos sete textos de atividade. Os núcleos estáveis
dos agentes e os schemas permanecem versionados nos executores nesta entrega.

O ciclo antigo poderá aderir explicitamente pela tela, fixando a definição Opala
sem trocar sua cadeia, orçamento, janela, aprovações ou histórico. O comando deve
ser idempotente, validar produto/tipo/experimento/etapa e registrar a adesão antes
de disponibilizar a execução aos agentes. Adesão não autoriza gasto ou publicação.
Vega/#92 é a referência de diagnóstico; nenhum ID de produção deve virar regra
de migração automática. Evidências e matriz: [Piloto Opala](../homologacao/catalogo-vivo-opala-v1.md).

### Contrato efetivo do piloto

- Gestão em `/catalogo-vivo/opala`: sete atividades, rascunhos sem sobrescrita,
  revisão do hash exibido e ativação do conjunto completo com controle de concorrência.
  Recuperação seleciona versões já revisadas. A autoridade é a fronteira administrativa
  existente; responsável e parecer são declarados pelo operador e auditados. O piloto
  não cria um sistema novo de login, papéis ou assinatura digital de revisão.
- API administrativa `/api/catalogo-vivo/v1/opala`; o consumo permanece no `pending`
  canônico de cada agente, que entrega `catalogPrompt` fixado na criação da tarefa.
  Tentativas da mesma ocorrência conservam a versão anterior. O callback deve comprovar
  instrução, contexto e referência da versão efetivamente enviados ao modelo.
- Banco é a única fonte operacional dos sete textos. Ausência, hash divergente ou
  schema incompatível bloqueiam a tarefa com orientação persistida. Não há retorno
  silencioso ao arquivo nem preenchimento automático de texto para tarefas antigas.
- A tela do ciclo oferece **Integrar e iniciar preparação**, preservando cadeia,
  experimento, versão, orçamento, janela e aprovações. O comando inicia o motor BPM
  existente na mesma transação da adesão e é idempotente.
- Ativação de textos serializa decisões no processo; fixação e revisão usam leituras
  com lock para não reutilizar um snapshot antigo do MySQL. O catálogo exibe cobertura,
  versões, utilizações e bloqueios de resolução; tempos, custos e resultados permanecem
  nos relatórios de execução existentes, sem confundir preparação com vendas reais.

### Evolução do contrato — revisão comercial Opala v2

Em 19/09/2026, a tarefa Vega #457 comprovou uma divergência entre a instrução armazenada e o
contrato de auditoria do executor: o contexto entregava cartões de pesquisa e o validador exigia
seus identificadores, mas o prompt v1 não mandava citá-los. O parecer comercial foi aprovado e só
então bloqueado pelo worker, depois de consumir a inferência.

Para novas tarefas de `commercialIntegrityReview`, o vínculo Opala passa a apontar para a versão 2,
que exige usar apenas a rota `meta-ad-approver`, cobrir ao menos um cartão de cada coleção entregue
e registrar o `cardId` em `evidence`. O validador não é flexibilizado: identificador ausente, cartão
não entregue ou coleção sem cobertura continua sendo erro. Versões e tarefas anteriores permanecem
imutáveis. Evidências: [recuperação da tarefa #457](../homologacao/vega-tarefa-457-cartoes-pesquisa-v1.md).

### Evolução do contrato — revisão comercial Opala v3

Em 19/09/2026, a retomada Vega #458 citou corretamente os cartões de pesquisa, mas revelou que o
contexto comercial ainda entregava todas as versões de criativo sem distinguir a candidata efetiva
nem anexar os direitos da mídia exata. O pacote versionado também possuía suporte funcional, porém
não declarava esse contrato como evidência. Repetir a inferência manteria as mesmas lacunas.

Para novas tarefas de `commercialIntegrityReview`, a versão 3 considera efetivo somente o único
criativo folha com estado `READY` e parecer `APPROVED`, preservando `sourceCreativeId` nas versões
anteriores para auditoria. O backend anexa a essa candidata a prova canônica de governança da mídia;
o manifesto vigente declara rota autenticada, persistência, retomada e fallback do suporte. A
revisão compara os termos materiais da candidata efetiva com landing e checkout, sem transformar
uma versão supersedida em contradição nem dispensar preço, cobrança, acesso, direitos ou suporte.
Versões históricas continuam imutáveis. Evidências: [recuperação da tarefa #458](../homologacao/vega-tarefa-458-candidata-final-suporte-direitos-v1.md).

## Catálogos e responsabilidades

| Elemento | Responsabilidade | Exemplo |
| --- | --- | --- |
| Agentes | Identidade, capacidades, limites e executor responsável | Dédalo, Plutus |
| Atividades | Trabalho, entradas, saídas, critérios de conclusão e agente responsável | Preparar entrada do PDE |
| Processos e subprocessos | Composição versionada de atividades, dependências e gates | Preparar operação comercial Opala |
| Tipos de produtos | Regras compartilhadas e processos aplicáveis | Opala, código oficial PDE |
| Produtos | Oferta concreta, contexto e particularidades comerciais | Vega |
| Prompts | Instruções textuais versionadas para agente e atividade | Preparar entrada comercial |

São seis conceitos, não uma determinação de criar seis tabelas novas. Processos e
subprocessos compartilham o catálogo de definições. Execuções, tarefas, ciclos,
aprovações, custos e resultados pertencem ao histórico operacional vinculado a essas
definições; não devem se confundir com as definições reutilizáveis de atividade.

Relação de referência: **produto → tipo → processo/subprocesso → atividade → agente
responsável → versão do prompt**. Um agente pode executar várias atividades e uma
atividade pode ser reutilizada em diferentes processos, com vínculos explícitos.

O produto fornece oferta, preço, público, orçamento e versão como contexto persistido;
não exige uma cópia de prompt por produto. A identidade e as regras estáveis do agente
podem compor a instrução da atividade, com versões e ordem de composição auditáveis.

## Alternativas e escolha

| Organização | Benefício | Risco | Esforço e aderência |
| --- | --- | --- | --- |
| Prompts por agente | Cadastro simples | Confunde trabalhos distintos do mesmo agente | Baixo; insuficiente para atividades especializadas |
| Prompts por produto | Personalização direta | Multiplica cópias e divergências | Alto na manutenção; baixa reutilização |
| Prompts por atividade e agente, especializados por tipo | Reutilização com contexto explícito | Exige validar vínculos e seleção | Intermediário; escolhido para o Catálogo Vivo |

A especialização por tipo deve ser explícita. O backend resolve primeiro o vínculo
específico do tipo; usa o genérico somente quando a definição permitir. Ambiguidade,
ausência ou incompatibilidade bloqueiam a execução com orientação persistida.

## Governança do texto e integridade

1. Cada prompt tem identidade estável, finalidade, texto, versão, autoria, datas,
   estado e identificação do contrato de saída compatível. Edição de versão ativada
   cria novo rascunho; versões utilizadas são imutáveis e não podem ser excluídas.
2. A ativação exige conteúdo não vazio, placeholders válidos, compatibilidade com o
   executor/schema e cobertura de todas as instruções exigidas pela atividade.
   Exigências devem constar no contrato versionado da atividade, não em uma segunda
   lista manual de caminhos de arquivos.
3. Chaves estrangeiras, unicidade e campos obrigatórios protegem os relacionamentos.
   Regras semânticas, cobertura e transições são validadas pelo backend em transação.
   Não confiar em `CHECK` para essas garantias no MySQL 5.7. Concorrência não pode
   ativar vínculos conflitantes; a estratégia concreta será validada na etapa 2.
4. Cada execução fixa as versões das definições e do prompt ao ser criada. Preserva
   texto resolvido efetivamente enviado, contexto, hash, request, response bruto,
   resultado, erro, modelo e tokens/custo quando disponíveis. Retry conserva a versão;
   ativar outra versão não modifica execução em andamento ou histórico.
5. Somente o backend acessa o banco e resolve os vínculos. Workers recebem texto e
   referências pelos contratos oficiais da própria fila/atividade, iniciando pelo
   `pending`, executam e reportam resultado. O backend decide o avanço do processo.
6. O schema de saída não é o texto do prompt. No piloto, permanece versionado no
   executor, referenciado por identificador, versão e hash compatíveis. Fluxos que já
   governam schemas no banco preservam seus contratos até migração específica.
7. Por vínculo migrado, o banco passa a ser a única fonte operacional do texto. Não
   manter edição concorrente em arquivo, sincronização bidirecional ou fallback
   silencioso para arquivo quando a consulta falhar. Fluxos ainda não migrados mantêm
   sua fonte atual, identificada no inventário.
8. O harness deve mostrar versão, origem e hash do catálogo para textos migrados.
   Recursos que continuam em arquivos mantêm cobertura e verificação de empacotamento.
   A migração deve retirar a dependência de cadastro duplicado de caminhos do texto,
   sem simplesmente desabilitar os testes globais de integridade.
9. A gestão cotidiana ocorre pelo frontend administrativo, com permissões, revisão e
   auditoria. Alterar prompt não concede autoridade para gasto, publicação, cobrança
   ou aprovação humana. Segredos não integram textos, contexto exibido ou auditoria.

Esta decisão substitui a obrigatoriedade de prompt textual em arquivo **apenas para
os vínculos formalmente migrados e homologados no Catálogo Vivo**. Preserva contratos
de saída, limites dos agentes, gates financeiros e demais regras de execução.

## Etapas da migração

Cada etapa é uma unidade completa de entrega local, com evidências e critérios
cumpridos antes de publicação. Não publicar correções parciais para descobrir falhas.

| Etapa | Entrega | Critério de conclusão | Estado |
| --- | --- | --- | --- |
| 1 — Inventário e vínculos | Mapear tabelas, APIs, telas, textos, consumidores e contratos dos seis catálogos; distinguir existente, duplicado e ausente | Matriz de origem/destino, dependências e escopo do piloto; conferir schema real via MCP | Concluída localmente para o Piloto Opala em 16/09/2026; demais fluxos pendentes |
| 2 — Base e integridade | Reutilizar estruturas existentes e implementar versões textuais, vínculos, ativação transacional e auditoria | Migração MySQL 5.7 local validada, rejeição de vínculos inválidos, concorrência e preservação histórica comprovadas | Concluída localmente para o Piloto Opala em 16/09/2026; demais fluxos pendentes |
| 3 — Gestão administrativa | Consultar, editar rascunhos, revisar e ativar pelo frontend; visualizar onde o prompt é usado | Jornada UI → backend → banco validada; erros e permissões claros; consulta ao histórico | Concluída localmente para o Piloto Opala em 16/09/2026; demais fluxos pendentes |
| 4 — Piloto Opala | Migrar o conjunto completo da preparação comercial Opala e seus consumidores; adequar harness e contratos de cobertura | Processo local ponta a ponta com prompts do catálogo, todas as atividades cobertas, auditoria e rollback comprovados | Concluída localmente para o Piloto Opala em 16/09/2026; demais fluxos pendentes |
| 5 — Expansão gradual | Migrar os demais fluxos por conjuntos completos de dependências | Mesmos critérios do piloto por lote; cobertura dos seis catálogos e ausência de duplicação operacional | Pendente |
| 6 — Consolidação | Remover leitores e arquivos textuais operacionais substituídos após prova de desuso; manter schemas e histórico necessários | Nenhuma referência ativa órfã, testes globais aprovados e relatório final de cobertura | Pendente |

O piloto Opala inclui entrada PDE, criativo, checkout, público, economia e homologação,
inventariando todos os agentes responsáveis, não somente os quatro textos de Dédalo.
Vega pode fornecer o caso de referência; o ciclo atual, orçamento e aprovações não
serão migrados ou reinterpretados automaticamente. Homologação usa dados segregados.

Antes de ativar um lote, registrar vínculos abrangidos, versões anteriores e novas,
executor compatível e evidência local. Rollback deve selecionar uma versão já
validada para novas execuções, preservando as existentes. Retorno ao runtime anterior,
se necessário, exige mudança explícita pelo fluxo versionado de publicação; falhas
de acesso ao catálogo ficam bloqueadas e auditadas, nunca disfarçadas por fallback.

## Matriz mínima de homologação local

Definir os casos concretos antes de testar a etapa. Usar dependências locais ou test
doubles para agentes e APIs externas, sem gasto, campanha ou cobrança reais.

| Dimensão | Verificações obrigatórias |
| --- | --- |
| Caminho feliz | Cadastrar/revisar/ativar texto, resolver atividade e tipo, consumir pending, executar, registrar callback e exibir resultado |
| Validações e falhas | Texto vazio, placeholder ausente, vínculo inválido/ambíguo, cobertura incompleta, schema incompatível, resposta inválida e indisponibilidade do backend |
| Integridade e concorrência | Duplicidade, duas ativações simultâneas, rollback da transação, migração idempotente, retry/callback duplicado e versão fixada |
| Integrações | Contratos do backend e de todos os workers do lote; harness; schemas e recursos empacotados; testes globais de cobertura com todas as divergências em um relatório |
| Observabilidade | Tela identifica prompt/versão, estado, bloqueio e próxima ação; request/response, custo e evidências correlacionados à execução |
| Métricas e segregação | Medir cobertura, falhas de resolução, tempo de preparação e retrabalho; excluir testes das métricas comerciais e impedir cruzamento de produto/ciclo/ambiente |
| Navegação | Frontend em Chromium desktop e emulação mobile iPhone/Pixel; registrar outros navegadores relevantes e limitações de validação, sem alegar teste Safari real por emulação |
| Continuidade e autoridade | Ativação não altera histórico; rollback preserva rastreabilidade; orçamento, gates e aprovações humanas continuam obrigatórios |

Executar a rodada local relevante, corrigir causas-raiz e repetir somente as
validações necessárias à correção e regressões relacionadas. Antes de commit ou
publicação, revisar diff e critérios completos do lote. Limitação essencial deve ser
registrada com evidências; PR, Actions e deploy não substituem teste local possível.
PR somente por solicitação explícita do usuário; publicação pelo fluxo versionado.

## Referências e acompanhamento

- [Governança de agentes](agent-governance-canon.v1.md).
- [Catálogo de processos e subprocesso Opala](business-process-catalog-canon.v1.md).
- [Tipos de produtos](product-types-canon.v1.md).
- [Cadastro de produtos](product-catalog-canon.v1.md).
- [Prompts, schemas e auditoria de IA](openai-informacoes-tratadas-canon.v1.md).
- [Arquitetura dos agentes premium](premium-ai-agent-architecture-canon.v1.md).

Atualizar a tabela de etapas ao concluir cada entrega, com data, escopo, evidências,
limitações e indicação separada de validação local e disponibilização em produção.
Nenhuma etapa pode ser marcada concluída somente por documentação ou cadastro parcial.
