# Vega — acesso Runway e coerência do plano de clipes

Data: 13/09/2026. Escopo preservado: produto 4, cadeia 14, processo 75/v6,
ciclo de aprendizado 2, experimento 92, versão `musa-pde-entry-v12-primeiro-ajuste-aplicavel`,
projetos 4/5 e perfis 59/60. Mira permanece separada: produto 10, processo 64/v8,
referência privada, execução 6.

## Diagnóstico confirmado antes de corrigir

- `sandbox-ssh root@177.153.62.107` passou. O executor de vídeo está saudável.
  A credencial permanece no arquivo montado no host, sem exportação para a sandbox.
- A leitura autenticada de `/v1/organization` respondeu HTTP 200 com 1.372 créditos.
  `/v1/routers` respondeu HTTP 200, `data: []`, `hasMore: false`.
  A lista vazia confirma que a configuração `marketing-hub-campaign-final-v1`
  usada nos projetos ainda não está cadastrada nesse projeto da conta.
- MCP confirmou banco `marketinghubdb` saudável. Os preflights 12/13 dos projetos
  4/5 continuam bloqueados por `PROVIDER_ROUTER_CONFIG_MISSING`, sem tarefa ou job.
  Os sucessos históricos 9–11 usaram `product_ugc@2026-06`, outra rota: não comprovam
  existência do Router nem autorizam transportar referências do experimento 91.
- Os dois projetos selecionam explicitamente `Runway Gen-4.5 (RUNWAY)`, com vídeo
  final de 15 segundos. O contrato `pending` dimensiona dois clipes, de até dez
  segundos. O painel e os metadados posteriores à decisão financeira usam outro
  resolvedor: ignoram `(RUNWAY)` e assumem Seedance, anunciando um clipe de 15 segundos.
  Isso cria divergência entre simulação, planejamento de Apolo e relato financeiro.
- A tela e o banco confirmam Vega em `WAITING_HUMAN`, 0/4 objetivos; Mira em
  `WAITING_HUMAN`, quatro objetivos concluídos, uma dispensa e uma decisão humana pendente.
- O coordenador está em `AWAITING_MERGE`, intervenção
  `398d1f700931474c9e7d89b8194f47dd`, aguardando a revisão de Mira
  `3222f37dd60bb0794ef50ffc4f720b57760eb0ed`, incluída no PR #5183 ainda aberto.
  Não foi liberada proteção nem alterada configuração produtiva.

## Alternativas comparadas

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Limitar todos os clipes a dez segundos | Resolve Gen-4.5 com pouco código | Altera desnecessariamente Seedance, Veo e a receita UGC | Não |
| Acrescentar Gen-4.5 apenas no resolvedor do ciclo | Pequena correção pontual | Mantém duas fontes de duração e a possibilidade de divergência | Não |
| Compartilhar a política de duração entre fila, painel e metadados | Corrige a causa e preserva limites por modelo | Esforço localizado, exige contratos cruzados | Sim |

Para a configuração externa: provisionar Router restrito ao Gen-4.5 já homologado
preserva a seleção dos projetos e tem menor esforço. Usar Product UGC tem precedente
de sucesso, mas exige novas referências governadas; produzir apenas captura e montagem
determinísticas preserva melhor a demonstração, porém exige verificar o contrato completo
de áudio, custos e revisão. A primeira é a candidata para restabelecer a rota existente;
nenhuma alternativa autoriza gasto antes dos gates próprios.

## Matriz definida antes dos testes

| Dimensão | Critérios locais |
| --- | --- |
| Caminho feliz | Projeto Gen-4.5 de 15s → preflight isolado com dois clipes de até 10s → payload do executor de 10s + 5s → painel e metadados de Apolo coerentes após decisão financeira simulada |
| Variações | Aliases explícitos, caixa de texto, referência a alternativa no texto, Seedance, Veo, UGC e plano legado Luma conforme a regra já existente |
| Falhas e gates | Router ausente, modelo não homologado, teto insuficiente, resposta inválida, reserva vencida, decisão indevida e repetição paga continuam bloqueados |
| Integrações | Services reais e serialização backend/executor; persistência e providers simulados; nenhuma API externa na homologação |
| Observabilidade e métricas | Preservar IDs, teto, custo conhecido, payload congelado, histórico e `publicationAllowed: false`; clipes cobrados separados de cortes editoriais |
| Segregação | Fixtures locais não usam IDs produtivos nem registram venda, cliente, campanha, autorização humana ou consumo real |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: dois clipes de até 10s, campos financeiros e ausência de erros de página |
| Configuração externa | JSON candidato conforme schema oficial, lista fechada e teto por geração; provisionamento e dry run reais dependem do coordenador `ACTIVE` |

Após a correção, executar duas rodadas locais completas e consecutivas desta matriz.
Uma falha exige correção e reinício da contagem. Esta homologação não comprova a
produção dos vídeos, QA audiovisual, vendas nem encerramento do processo comercial.

Fontes oficiais consultadas: [configuração do Router](https://docs.dev.runwayml.com/model-routers/configuration/),
[dry run sem cobrança](https://docs.dev.runwayml.com/model-routers/generating/)
e [schema da API](https://docs.dev.runwayml.com/api/).
Evidências sanitizadas em `artifacts/runway-access-recovery/`.

## Resultado da correção e validação

O teste novo reproduziu o defeito antes da alteração: as duas seleções explícitas de
Gen-4.5 esperavam limite de dez segundos, mas o ciclo retornava quinze.
`reproduce-before-fix.log` preserva as duas falhas. Depois da correção, preflight,
resposta do ciclo e metadados do job concordam; o executor real monta requests de dez
e cinco segundos. Os marcadores legados de UGC, Seedance, Veo e Luma estão cobertos.

Durante a preparação, a fixture visual precisou receber os campos obrigatórios do
projeto. A revisão também preservou a precedência do marcador Product UGC. Ao tornar
o schema oficial uma fixture versionada, a primeira extração removeu indevidamente o
campo `description`; o validador rejeitou o candidato. A extração foi corrigida para
preservar nomes de propriedades e remover apenas anotações de schema. Essas tentativas
não contam como rodadas finais. Depois da última correção, `final6` e `final7` passaram
integralmente e consecutivamente:

| Verificação | Resultado por rodada |
| --- | --- |
| Backend: vídeo, preflight, gate financeiro e arquitetura | 252 testes executados, nenhuma falha; dois testes opcionais de publicação via browser fora deste escopo |
| Executor de vídeos | 157 testes executados, nenhuma falha |
| Estúdio e API do frontend | 39 testes executados, nenhuma falha |
| Contrato entre módulos | Pending exportado pelos services reais → request real do worker → dois clipes, 10s + 5s |
| Configuração candidata | Restrição estrutural da API oficial, Gen-4.5 exclusivo, fallback por capacidade desabilitado, teto por request e payload de dry run conferidos |
| Navegação local | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; painel coerente, nenhuma escrita ou chamada externa, nenhum erro de página |
| Revisão | Spotless, comentários de responsabilidade e `git diff --check` aprovados |

Total: **448 testes executados por rodada**, além das verificações de contrato,
configuração e navegador. Persistência e decisões foram simuladas; não houve teste
com MySQL nem necessidade de changelog nesta alteração. O replay entre módulos é
local e não equivale à aceitação de um job pelo provedor real.

O backend foi empacotado após os testes. A conferência do JAR confirmou 3.944 classes
idênticas às compiladas/testadas, 418 recursos íntegros e inicialização de 213 cartões
do catálogo; logs em `backend-package.log` e `verify-package.log`. O servidor local de
homologação foi encerrado. Nenhuma topologia Compose foi criada nesta solicitação.

Reprodução: [roteiro local](../../infra/testing/runway-clip-plan/README.md).
Logs e contratos: `artifacts/runway-access-recovery/final6/` e `final7/`.
Prévia: [iPhone](../../artifacts/runway-access-recovery/final7/browser/iphone.png),
[desktop](../../artifacts/runway-access-recovery/final7/browser/desktop.png).

## Limites operacionais e continuação

A leitura autenticada sanitizada de 13/09/2026 às 06:48 UTC confirma acesso saudável,
1.372 créditos, uso diário Gen-4.5 igual a zero e lista completa de Routers vazia.
O segredo foi usado apenas em memória no host, sem ser copiado para a sandbox.
Evidência: `artifacts/runway-access-recovery/ssh-runway-readonly.json`.

O [JSON candidato](../../video-management-service/config/runway/marketing-hub-campaign-final-v1.json)
permite somente `gen4.5`, com limite de 400 créditos por geração. Para dois clipes,
isso admite até 800 créditos por vídeo, equivalente ao limite de USD 8 já registrado
no planejamento, conforme a unidade de crédito persistida. É limite, não estimativa
de consumo. O teto total de USD 20 para produção e revisão das duas peças permanece
preservado; novas decisões continuam dependentes de Plutus e dos gates de qualidade.

O PR [#5183](https://github.com/paulofor/marketing-hub/pull/5183) da intervenção anterior
de Mira continua aberto. O coordenador informa `AWAITING_MERGE` e
`safe_to_intervene: false`; `begin` recusa outra intervenção enquanto a anterior
não estiver `RELEASED`. A retomada automática dessa intervenção já está preparada.
Não houve `begin`, `execute`, `resume`, modificação de container, provisionamento do
Router, publicação de imagem, commit, push ou abertura de PR nesta solicitação.
As mudanças locais de dimensionamento **não fazem parte do PR #5183**.

Após a integração e a liberação automática, a recuperação já autorizada poderá
iniciar sua própria intervenção, aplicar o backend validado e provisionar o candidato,
conferir o dry run real e continuar pelo Estúdio com o backend coordenando os gates.
Não foi repetido o preflight enquanto o mesmo Router permanece ausente.
A aceitação real pelo provedor, geração, revisão audiovisual e conclusão comercial
continuam pendentes; não se usou publicação como mecanismo de teste.

- **Vega:** processo 75/v6, ciclo 2, experimento 92; continua com 0/4 objetivos comprovados.
  [Processo](http://191.252.181.168:5173/products/4/value-chain-history/processes/75/activities?chainId=14&learningCycleId=2).
- **Mira:** processo 64/v8, execução 6; quatro objetivos comprovados, uma dispensa e
  uma decisão humana pendente. Íris 414, Psique 415 e Têmis 416 estão concluídas;
  a peça 128 permanece para avaliação humana na atividade 4.1.6.
  [Decisão de uso](http://191.252.181.168:5173/products/10/value-chain-history/processes/64/activities?chainId=14#activity-human).

Oportunidade comercial preservada como hipótese: usar a interface real aprovada na
demonstração de Vega para mostrar o primeiro ajuste aplicável e a retomada, mantendo
público, oferta e preço. Prioridade alta e esforço de edição moderado; medir primeiros
resultados por início, avanço ao checkout e vendas líquidas. Nenhum efeito comercial
foi medido nesta recuperação de acesso.
