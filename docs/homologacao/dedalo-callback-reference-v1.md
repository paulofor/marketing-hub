# Recuperação de callback de Dédalo — 23/11/2026

## Caso e limites

Tarefa 925, produto 4, cadeia 27, ciclo 22, experimento 115, processo 127 v12,
execução 103: saída READY é especificação PROTOTYPE_SLICE, não implementação.
Retorno original: 26.863 bytes, SHA-256
`6219823a1328c0e6b42d91a2101057166e4a66496309a94cd42a0223bda0e583`.
Nenhuma nova inferência, mídia, audiovisual, cobrança, preço ou aumento do teto US$200.
Preservar 102/103, 922/923/924, artefatos, recibos 140/141 e entregas 4003/4004.

## Alternativas

1. Buscar substring em toda a resposta: baixo esforço, mas aceita referência embutida/ambígua.
2. Reescrever o resultado ou gerar outro: perde bytes originais ou repete custo.
3. Campo explícito nas novas respostas e leitura legada delimitada: pequeno ajuste de contrato,
   preserva auditoria e recusa referências divergentes. Opção adotada.

Para rejeições, comparar deixar HTTP 500 (loop), descartar o retorno (perda) e persistir
bloqueio recuperável com recibo e replay oficial (adotado). Falhas técnicas continuam transitórias;
rejeição funcional não autoriza outra chamada nem autoaprovação.

## Matriz de homologação definida antes da correção

| Área | Verificação e aceite |
| --- | --- |
| Referência | campo canônico; literal legado; prefixo legado inteiro; ausente, errada, ambígua, embutida e de outro contexto recusadas |
| Identidade | produto, fonte/ciclo, processo/versão e baseline atuais; outra identidade sintética e caminho antes válido |
| Decisão | READY completa somente especificação; BLOCKED preserva reprovação e não avança |
| Transporte integrado | worker real, HTTP isolado, controller, service, hook e persistência; falha reproduzida antes |
| Recuperação | bytes originais, auditoria, tokens, claim/fencing; replay exato, concorrente e tardio; sucessora recusa retorno antigo |
| Rejeição semântica | bloqueio persistido, HTTP distinto de 500, sem reenvio terminal infinito; outbox preservada até recibo confirmado |
| Falha transitória | 503/desconexão preserva retorno, retry sem modelo e sem dupla contabilização |
| Processo | instância sem falso aceite na rejeição; aceite único e próxima pendência legítima na recuperação |
| Observabilidade | mensagem clara; diagnóstico bruto só em auditoria; READY não representa software construído |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 nas mensagens afetadas |
| Financeiro | testes isolados, zero inferência adicional; limites, reservas e custos desconhecidos preservados |

## Evidência inicial

MCP confirmou 925 IN_PROGRESS sem resultado persistido e 924 COMPLETED. Logs do backend
confirmam IllegalArgumentException na referência. Executor na imagem `19ca6ab3…` tem só Java,
sem processo de IA ativo. Outbox original tem operação result, modelStarted=true, zero recusas,
claim original e uso gpt-6.1-sol/max: entrada 868696, cache 715648, saída 17328.
O SHA-256 e tamanho baixados para diagnóstico conferem com o original informado.
Não houve alteração operacional nem nova execução.

## Contrato e recuperação adotados

- Novos componentes Turmalina usam campo raiz explícito e schema/prompt v5 catalogados no harness.
  Retornos antigos permitem somente o literal inteiro ou `specificationReference=<literal>`
  em acceptanceCriteria. Divergência entre campos bloqueia. O validador reconfere a baseline
  contra a especificação atual do mesmo processo/fonte; não altera bytes nem aceita substring.
- Rejeição do hook confirma transação BLOCKED com resultado/evidência/auditoria/consumo, HTTP 422
  e código estável. A outbox fica em quarentena, sem POST semântico repetido ou nova inferência.
  `POST /api/agent-tasks/{id}/replay-result` revalida exclusivamente o original sob lock;
  reenvio já aceito é idempotente. Tentativa posterior impede recuperação histórica.
- Recibo somente leitura confirma status e hashes dos bytes do callback. Só COMPLETED com hashes
  idênticos permite liberar outbox recusada. Antes de liberar a fila, o executor arquiva envelope,
  arquivo bruto, eventos e identidade do processo sem normalizar seu conteúdo.
- O resultado bruto da 925 tem 26.863 bytes/SHA `6219823a…`; o resultJson do callback original,
  já serializado pelo executor antes desta correção, tem 25.613 bytes/SHA
  `de7ef0b550672dda60317f062a4a2435e8783a20c0cf0e68d28c85a5be0bb50f`.
  Ambos foram preservados em arquivo restrito no executor antes da publicação; não são o mesmo
  conjunto de bytes e não devem ser confundidos na prova do banco.

## Seleção operacional e segurança da publicação

A API real de Produtos em play retornou run 103/processo 127, mas atividade audiovisual/Apolo.
O processSituation do histórico prioriza bloqueios legados. A projeção operacional agora prioriza
somente a atividade indicada pelo run quando uma tarefa ativa confirma processo e fonte exatos.
A contagem de aceites e as pendências históricas não são modificadas. Outra versão/fonte cai no
caminho anterior; regressões cobrem duas identidades e o caminho antes válido.

Na publicação, foram comparados parar o executor manualmente (intervenção dispensável), substituir
o container imediatamente (risco de interromper a próxima tarefa) e reutilizar o lock durável já
validado de Atena (adotado). O pipeline de Dédalo aguarda a liberação do lock Java antes da troca,
mantém proteção até saúde confirmada e conserva a pausa em falha. A marca impede novas reservas,
mas não impede devolver resultado já produzido. Não cancela trabalho nem publica por SSH manual.
O teste executa o comando remoto em doubles locais, validando aspas, estado UP/DOWN e ShellCheck;
o teste existente comprova exclusão real via fcntl. Nenhuma regra de arquitetura foi modificada.

## Reprodução antes da correção

- Teste de serviço com o prefixo original falhou em dois contextos na implementação anterior.
- O harness integrado usou o consumidor real compilado, HTTP local, MVC, JPA/transações e o
  bytecode do ProductValueDeliveryService anterior: dois casos retornaram HTTP 500, mantiveram
  IN_PROGRESS e não aceitaram a atividade. O bytecode corrigido foi restaurado antes de validar.
- Nenhum desses testes usou rede de modelo nem produtos persistidos em produção.

## Resultados locais da solução

- Backend: 278 testes relacionados aprovados, incluindo o callback privado original validado
  somente em memória. A matriz integrada executou mais 15 casos com worker real/HTTP/MVC/JPA,
  sem skip, nas referências canônica, prefixada e literal; bloqueios e replays não duplicaram
  consumo. Nenhuma dependência de modelo externo foi usada.
- Arquitetura: 92 regras existentes aprovadas, sem alteração de testes/regras.
- Dédalo: 128 testes, 127 aprovados e uma fixture privada opcional não fornecida ao worker;
  a resposta original foi conferida separadamente no backend, sem inferência.
- Frontend: nove testes relacionados e TypeScript aprovados; Chromium desktop, iPhone 15 Pro
  e Pixel 7 passaram por recusa/revalidação e cartão da atividade atual, com dados sintéticos
  exportados pela API local do backend. Diagnóstico bruto permaneceu fechado, fora da mensagem.
- Publicação: 42 testes de contratos/comando aprovados e cinco testes existentes do lock real;
  bash -n e ShellCheck do comando extraído aprovados. YAML dos workflows/Swagger válido.
- A primeira rodada concorrente com build frontend excedeu a memória da sandbox (exit 137).
  As rodadas foram repetidas separadamente, com JVM limitada; não houve ajuste de critérios.
  O harness integrado deixou de depender do diretório corrente e passou a exigir classes do
  executor compiladas, eliminando skip silencioso na conferência sem fork.

As evidências locais estão declaradas em `.codex/delivery-artifacts.json`; não incluem credenciais
nem o contexto privado original. CI, revisão, merge, saúde publicada e recuperação produtiva serão
conferidos no PR. Testes técnicos não comprovam demanda, receita ou contribuição por venda.

O comando combinado de frontend também excedeu o heap Node de 1.200 MiB definido para a rodada.
A validação final aprovou TypeScript separado (tsc --noEmit) e o bundle Vite com as opções
funcionais da configuração versionada; somente o checker redundante foi retirado do processo
do bundle pelo wrapper local, com heap de 2.200 MiB. Bundle concluído em 35,26 s. A configuração
produtiva não foi alterada e o CI continua exigindo o build normal do repositório.

A conferência final dos metadados garante que novas respostas de componentes declarem v5,
sem regenerar a evidenceJson v2 do callback original. A suíte completa do worker permaneceu
com 127 aprovações e um skip opcional. Os 42 contratos de publicação e o gate premium passaram,
conservando o contrato SSH existente e sem modificar testes de arquitetura. O manifesto local
foi conferido contra os arquivos existentes/ignorados; originais privados não são incluídos.
