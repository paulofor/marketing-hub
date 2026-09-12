# Vega — próxima ação explícita no processo

## Diagnóstico confirmado em 12/09/2026

UI, API e MySQL via MCP confirmaram produto 4, cadeia 14, processo 75/v6,
ciclo 2, experimento 92 e versão `musa-pde-entry-v12-primeiro-ajuste-aplicavel`.
O processo 4 terminou; o processo 6 tem 0/4 objetivos comprovados e execução 4
em `WAITING_ACTIVITY`. O ciclo está em `VIDEO_BRIEF`, revisão 7, sem evento
`AUTHORIZE_VIDEO_BUDGET`. A conciliação continua respondendo, mas o primeiro
ramo de atividade em andamento descarta a orientação específica e grava uma
mensagem genérica. Esperar não registra o teto nem conclui o briefing.

O MCP identificou backend `4cc83a6cd29849d3857f2085bb20661fd64d9cb6`.
A tela financeira existe na revisão local posterior `2f80d6d1`; o endpoint
correto de orçamento retornou 404 no ambiente publicado. Não é evidência de
cache do navegador. Nenhuma escrita produtiva foi executada nesta análise.
Logs MCP funcionaram; filtro ProcessRun não retornou linhas e consulta geral
retornou logs atuais. Histórico e loops BPM/ciclo e financeiro foram consultados.
Evidências brutas: `artifacts/vega-next-action/live-*` e `db-state-*`.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Publicar apenas a tela financeira existente | Disponibiliza o formulário; esforço baixo | Card permanece ambíguo e não revela a dependência | Insuficiente para esta reclamação |
| Deduzir a pendência pelo nome do produto no frontend | Atalho rápido; esforço baixo | Diverge do backend e pode misturar ciclos e autorizações | Rejeitada |
| Resolver pendência no backend e apresentar ação direta no card | Mostra motivo, responsável, decisão e continuidade reais | Esforço moderado; exige regressão de estado e identidade | Escolhida |

A correção usa a autorização persistida da versão exata. A leitura não avança
o processo. A conciliação registra a espera humana e seu motivo no diário.
Após o teto, o card orienta o briefing e a avaliação; não promete produção
automática nem aprovação comercial. Pausas, trabalho real em curso, histórico
e outros processos continuam com seus próprios estados.

## Matriz definida antes dos testes

| Área | Critério |
| --- | --- |
| Caminho feliz | Processo → teto em USD → recibo → retorno → próxima ação de briefing; contexto preservado |
| Estado e histórico | Espera humana sem animação de execução; 0/4 preservado; diário atualizado só na conciliação; teto não conclui processo |
| Validações | Valor inválido, revisão antiga e identidade divergente recusados; autorização de outra versão não libera |
| Regressões | Processo pausado/concluído, outro produto, etapa posterior e tarefa de agente em andamento não recebem pendência financeira indevida |
| Integração | Controller/service/JPA/MySQL 5.7 reais; fontes externas simuladas; nenhuma API paga |
| Observabilidade | Motivo, responsável, próxima ação, recibo e correlação ciclo/experimento disponíveis; falha de consulta não autoriza comando |
| Métricas | Nenhuma venda, receita, render, campanha ou cobrança de teste entra em produção |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; teclado, links, leitura após retorno, cópia do contexto, sem overflow |
| Qualidade | Testes relevantes Java e frontend, TypeScript, build, formatação e diff; duas rodadas consecutivas após a última correção |

Somente sandbox. PR e publicação dependem do fluxo solicitado pelo usuário.

## Defeito adicional confirmado na integração local

O teste REST/MySQL reproduziu `PAUSING` indefinido sem tarefas pagas ou reservadas:
`inFlight` contava o estado agregado `IN_PROGRESS` do ciclo como trabalho real.
Alternativas: esconder o botão (barato, elimina controle útil), pausar ignorando
todas as atividades (barato, arrisca trabalho real), distinguir a entrada humana
do trabalho em curso (moderado, preserva tarefas). Escolhida a terceira.
A pausa pode concluir na definição dos vídeos quando não há tarefa ativa; tarefas
reais e delegações continuam protegidas. A matriz inclui pausa e retomada REST.
Rodadas anteriores são diagnósticas; o aceite exige duas novas rodadas completas.

## Aceite local

As rodadas `guidance-round1` e `guidance-round2` passaram consecutivamente após
a última correção. Os 18 arquivos de implementação e testes mantiveram os mesmos
SHA-256, registrados em `artifacts/vega-next-action/validated-files.json`.

| Validação por rodada | Resultado |
| --- | --- |
| Backend, ciclos, orçamento e arquitetura | 302 testes, sem falhas ou testes ignorados |
| Interface financeira, ciclos, processo e cópia | 97 testes, sem falhas |
| REST / MySQL 5.7 real | 8 grupos aprovados, incluindo pausa/retomada, concorrência e invalidação por versão |
| Navegação real da aplicação local | Desktop, iPhone 15 Pro e Pixel 7 emulados; 10 verificações por perfil |
| Erros de página e chamadas externas da jornada | Zero |
| TypeScript, build, Spotless, Prettier e Swagger | Aprovados |
| Diferenças e isolamento | Diff revisado; mesmo projeto exclusivo; containers e rede removidos ao fim |

As capturas mostram produto explicitamente identificado como fixture. Avisos globais
de Facebook/renovação provêm dos doubles; não são diagnóstico de produção.
As APIs e a persistência do ciclo, teto e execução de processo são reais na sandbox.
Fontes comerciais adjacentes são simuladas. Emulação Chromium não representa Safari nativo.
O build registrou somente avisos conhecidos de bundle grande e API CJS do Vite.

Evidências:

- `artifacts/vega-next-action/round1.log` e `round2.log`.
- `artifacts/video-finance/guidance-round1/` e `guidance-round2/`: relatórios Java,
  frontend, REST, logs de auditoria, capturas e limpeza.
- `artifacts/video-finance/guidance-round2/browser/desktop-process-pending.png`:
  decisão explícita com link ao teto.
- `artifacts/video-finance/guidance-round2/browser/iphone-process-after-budget.png`:
  retorno com orientação atualizada sem anunciar conclusão.

Reprodução:

```bash
export VIDEO_FINANCE_COMPOSE_PROJECT='<projeto exclusivo informado pela sandbox>'
bash infra/testing/video-finance/run-round.sh guidance-round1
bash infra/testing/video-finance/run-round.sh guidance-round2
```

## Situação produtiva e próxima ação

A conferência final de 12/09/2026 manteve execução #4 em `WAITING_ACTIVITY`, 0/4,
ciclo #2/experimento #92, e HTTP 404 no endpoint financeiro. A revisão em produção
continua anterior à tela financeira. Nenhuma publicação, PR, commit, envio de imagem,
gravação produtiva, campanha ou tarefa paga foi realizado nesta solicitação.

Depois de integrar e publicar backend e frontend pelo fluxo de PR do usuário,
o operador poderá abrir **Informar teto dos vídeos**, conferir o contexto e registrar
o limite total em USD. O retorno orientará o briefing e a avaliação financeira.
Isso não conclui venda, entrega ou aprendizado: seus objetivos exigem evidências
e autorizações próprias. O histórico do experimento #91 permanece intacto.
