# Homologação da sequência comercial do Vega

Matriz definida antes dos testes em 08/09/2026. Escopo: configuração do plano #3 pela UI e
correções locais da referência de operação e da orientação de plano bloqueado.
Não é homologação da futura experiência do Vega nem execução de observações/vendas.

| Controle | Resultado esperado |
| --- | --- |
| Seleção canônica | Plano em andamento/bloqueado seleciona seu experimento já operado, inclusive pausado, na leitura e na criação de tarefa. |
| Histórico | Sucessor PLANNED não substitui operação anterior; tentativas e custo históricos continuam consultáveis. |
| Sincronização recorrente | Salvar seleção pela UI e executar três sincronizações locais de Hermes preserva a referência, o roteiro, a pausa, o preço e o orçamento; snapshot mantém a seleção original. |
| Segregação | Plano cancelado/concluído/rascunho e experimento de outro produto não mudam a operação; Rigel mantém #89. |
| Orientação | Plano BLOCKED apresenta causa e próxima ação persistidas, sem sugerir publicação ou aprovação. |
| Gates existentes | Ausência de homologação permanece bloqueada nos demais planos; aprovação não é inferida de custo ou tarefa. |
| UI local | Plano editável, roteiro legível e vínculo #91 preservado em Chromium desktop, iPhone e Pixel com API simulada. |
| Falha de integração | Erro ao salvar mantém o texto e expõe falha; nenhuma tarefa/campanha é criada. |
| Persistência produtiva | Após salvar pela UI, ler versão e campos pela API/MCP; conferir #90, #91, preço, teto e processo de Rigel. |
| Dados de teste | Navegação local usa doubles; nenhuma chamada de escrita produtiva fora da edição autorizada do plano. |

Se houver correção de defeito durante a rodada, executar duas rodadas completas consecutivas
depois da última correção. As entregas futuras do roteiro possuem homologação própria, descrita
no cânone, incluindo e-mail descartável, checkout de teste, observabilidade e pessoas consentidas.

## Diagnóstico e mudança aplicada

O subprocesso `operacao-otimizacao-experimento` (#66 v5) pertence tanto ao Vega quanto ao
Rigel. A escolha foi ajustar o plano atual do Vega (#3), sem editar essa definição compartilhada.
As alternativas, responsáveis e critérios de avanço estão no
[cânone Vega v1](../canonical/vega-sequencia-comercial-canon.v1.md).

A edição foi feita em `http://191.252.181.168:5173/planning/3`, pelo formulário e pelo botão
**Salvar planejamento**. Uma única requisição de escrita retornou HTTP 200 e criou a versão
comercial **6**, registro **26**. Objetivo, causa, bloqueio, roteiro de sete entregas e critérios
ficaram salvos. O plano ficou `BLOCKED`, com marco operacional de R$ 335 brutos (cinco compras
de R$ 67, antes de custos e reembolsos) e uma abordagem por teste. Esse valor é uma meta,
não receita realizada. A meta mensal anterior de R$ 1.340 foi preservada.

A conferência posterior pela API e pelo MCP revelou a falha adicional: a resposta do salvamento
selecionava #91, mas a sincronização de Hermes voltou a selecionar #90 em oito segundos. O
restante do roteiro persistiu. O código confirmado no repositório fazia essa substituição em
toda sincronização. A correção local preserva a escolha existente na sincronização passiva,
mantém o comando explícito de selecionar RUNNING e registra seleção, portfólio, causa e meta
nos novos snapshots. O histórico anterior continua intacto.

Também foram corrigidos localmente: a referência de operação na leitura e no comando, a
prioridade da orientação deliberada de um plano bloqueado, a atualização do painel após salvar
e a leitura das quebras de linha em desktop e celular.

## Conferência do ambiente publicado

Consulta de **08/09/2026 às 03:25 UTC**, com
[evidência estruturada](../marketing/evidencias/vega-sequencia-comercial-2026-09-08.json):

- Onze campos solicitados, além da seleção, permaneciam exatamente como salvos pela UI.
- Seleção observada depois do ciclo automático: **#90**, ainda incorreta; desejada: **#91**.
- #91 permaneceu `USER_STOPPED`; #90 permaneceu `RUNNING`.
- Preço de R$ 67 e orçamento do plano de R$ 200 permaneceram iguais. Não houve comando de
  retomada, publicação, compra nem alteração de verba de mídia.
- Todas as definições de processo permaneceram iguais; Rigel continuou em `experiment:89`.
- Não foram criadas tarefas de agentes, observações humanas ou vendas por esta edição.

As correções de código permanecem locais. O ambiente publicado ainda não sustenta a seleção
do #91. **Após PR e deploy**, editar o plano pela mesma tela, selecionar #91 e salvar; conferir
que a seleção permanece após a sincronização de Hermes, que o painel apresenta o roteiro
(`CORRECT_CURRENT_PLAN`) e que a operação do Vega usa `experiment:91`. Essa é uma validação
posterior à publicação, não uma substituição dos testes locais.

## Resultados locais

Depois da última correção foram concluídas **duas rodadas locais completas e consecutivas**,
sem falhas:

| Controle por rodada | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend: planejamento, versões, operação BPM, referência e contratos de Hermes | 119 testes / 19 classes | 119 testes / 19 classes |
| Frontend: formulário, orientação imediata e contrato de API | 26 testes | 26 testes |
| Browser: salvar/recarregar + falha/recuperação, em três dispositivos | 6 jornadas | 6 jornadas |
| Typecheck e build do frontend | Aprovados | Aprovados |
| Spotless das classes alteradas e Prettier do frontend alterado | Aprovados | Aprovados |

O teste recorrente cobre quatro estados da seleção e três ciclos de sincronização de Hermes
depois de atualizar o plano. Os testes BPM preservam tentativas anteriores e comparam a referência
da consulta com a do comando. O frontend comprova que salvar Vega não atualiza o cache de outro
produto e que uma falha de persistência não é apresentada como sucesso.

Comandos principais, no módulo correspondente:

```text
mvn -B -q '-Dtest=BusinessProcessActivityExecutionServiceTest,CommercialPlan*Test,HermesProductProcessActivityReadinessProviderTest,ProductSubprocessPositionResolverTest,GrowthOperatorServiceTest' test
npm test -- --run src/pages/planning/CommercialPlanningPage.test.tsx src/pages/planning/CommercialOperationalFlowPanel.test.tsx src/config/api.test.ts
npm run typecheck
npm run build
```

Os testes usam doubles locais para persistência, trabalhadores e APIs; Chromium real executa
a UI da sandbox com desktop e emulação de iPhone 15 Pro e Pixel 7. Os digests dos logs de cada
rodada estão na evidência estruturada. Não houve migração de schema nem necessidade de Docker,
SSH ou API de IA paga para validar esta mudança. Nenhum código foi publicado.

A homologação cobre este ajuste do planejamento, não a futura melhoria da experiência MUSA,
a Meta Ads, o checkout produtivo ou a validação qualitativa com pessoas.
