# Mira — correção de criativos a partir do parecer

Data: 2026-09-13. Escopo: produto 10, cadeia 14/v14, processo 64/v8,
referência `product:10@agent-validation-v1`, execução 6 e pai 63/v7, execução 5.
Não há ciclo nem experimento nesta preparação privada.

## Diagnóstico confirmado

A tela, os endpoints oficiais e o MySQL consultado pelo MCP concordam: tarefa
412 de Íris concluída, tarefa 413 de Psique bloqueada com `decision=ADJUST`.
O parecer exige identificar a aplicação web, ampliar a prova no celular e tornar
legível a ressalva de demonstração sintética sem compra ou cobrança.

O PNG 127 foi conferido contra o SHA-256 persistido. Os logs dos dois workers
confirmam produção, upload, callback e consumo do mesmo PNG. O histórico de Vega
406–408 demonstra sucesso com produção e revisões; não justifica remover gates.
O backend consultado pelo MCP retornou banco saudável (`marketinghubdb`). A busca
por 413 no log do backend retornou zero linhas; os callbacks estão comprovados
pelos workers e pelas instâncias 276/277 persistidas.

`CreativeProductionReadinessProvider` reabre corretamente a produção após ADJUST,
mas `ProcessRunService.actionKey` considera somente objetivos concluídos. O parecer
novo não altera essa chave: o evento 123 registra NO_PROGRESS sem abrir a correção.
O worker já recebe `blockedActivities`, incluindo os três ajustes; sua fonte visual
é limitada à captura desktop. A ressalva usa fonte fixa de 25 pixels em PNG de
1080 pixels, aproximadamente 9 pixels quando exibido a 393 pixels de largura.

Rastreamento: a tela consulta `/api/business-processes/64/products/10/automation/v1`
e `activity-executions`; `ProcessRunController` chama `ProcessRunService`, que persiste
controle/eventos e solicita a atividade pelo service do domínio. O executor inicia
por `.../communication-director/stage-executions/pending`, recebe o contexto e acessa
`/{taskId}/visual-inputs`. Upload, auditoria e callback retornam ao backend. Tarefas,
instâncias BPM e eventos persistidos confirmam cada resultado; os logs de Íris e
Psique confirmam download e revisão dos mesmos pixels.

Provas inspecionadas: PNG 127 da tarefa 412, SHA-256
`475bacd2f992914b6858085dd70054bd0ecdcb361753e089997709fb1a40e9d5`;
fonte mobile 96 da tarefa 371, SHA-256
`317e00af335f3f651f301e0f480267d2cb11dd7b8a98124969bfa170608629c3`.
A fonte pode ser consultada pela rota oficial
`/api/agent-tasks/371/visual-evidence/96/content`. A prévia local usa recorte
`x=12, y=356, width=369, height=212`; não foi cadastrada como peça real nem aprovação.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Retomar manualmente após cada parecer | Reutiliza comando existente | Exige intervenção recorrente e não melhora a entrada visual; esforço operacional permanente | Não adotada |
| Repetir toda tarefa pendente ou bloqueada | Dispensa intervenção | Pode repetir gasto com a mesma causa e entrada; implementação simples, risco alto | Não adotada |
| Vincular correção ao parecer novo e corrigir a composição | Automação auditável com proteção contra repetição | Exige testes de retorno, escopo, idempotência e renderização; esforço localizado | Adotada |

Para a prova visual foram comparados: ampliar apenas o desktop (pouco ganho em
textos longos), redesenhar uma tela (perderia a prova real) e disponibilizar a
captura mobile aprovada com recorte fiel (melhor leitura, sem inventar produto).
Adotada a terceira, preservando desktop como opção quando necessário.

## Matriz definida antes dos testes

| Dimensão | Critérios |
| --- | --- |
| Caminho feliz | Processo → produção → ADJUST → nova produção → Psique → Têmis → aguarda decisão humana → conclusão somente após decisão explícita em fixture local |
| Idempotência | Um parecer novo permite uma correção; polling repetido, falha própria e parecer inalterado não criam tarefas duplicadas |
| Identidade | Mesmos produto, cadeia, definição, referência e versão; histórico, custos e decisões preservados; outra referência e revisão antiga não liberam trabalho |
| Gates | BLOCKED técnico não equivale a ADJUST; evidência ausente/inválida ou arquivo divergente bloqueia; nenhuma aprovação fictícia |
| Integração | Backend, HTTP, fila/callbacks, persistência local e executor com modelo/storage simulados; processo decide o avanço |
| Conteúdo | Identificação clara de aplicação, produto informado e resultado real; ajustes recebidos no prompt; nenhuma promessa clínica ou comercial nova |
| Visual | PNG real, hash, recorte e fonte rastreáveis; ressalva próxima ao CTA e legível; fonte desktop/mobile aprovada; inspeção a largura de feed mobile |
| Observabilidade | Tarefa corretiva ligada ao parecer; request/resposta preservados; histórico, erro, custo e ausência de métricas comerciais de teste verificáveis |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7; links para pai preservados, estado apresentado conforme backend |
| Segregação | IDs e banco de QA locais; sem SMTP real, clientes, vendas, mídia ou cobrança de teste em produção |
| Entrega | Duas rodadas locais completas sem falhas após a última correção; revisão do diff, empacotamento e conteúdo das imagens antes de intervenção |

## Resultados

As rodadas `final1` e `final2` terminaram completas e consecutivas sem falhas.

| Verificação por rodada | Resultado |
| --- | --- |
| Backend, incluindo ArchUnit e ciclo com persistência real | 2.884 executados; 8 cenários opcionais sem fixture externa dispensados |
| Persistência privada no MySQL 5.7 local | 3 testes |
| Íris, incluindo fontes mobile/desktop, hashes, recorte e repetição da imagem rejeitada | 30 testes; 1 cenário opcional específico de Vega dispensado |
| Psique e Têmis | 106 + 91 testes; 1 cenário opcional específico de ciclo em cada módulo dispensado |
| Executor de processos e frontend | 6 + 50 testes |
| Contratos de CI e aplicação preservando configuração | 8 + 3 testes |
| Navegação e renderização | Desktop, iPhone 15 Pro e Pixel 7; contexto privado, retorno ao pai, decisão explícita, conclusão conforme backend, ausência de overflow e erros JavaScript |
| Empacotamento | 3.944 classes idênticas às testadas; 418 recursos íntegros; catálogos inicializados no JAR executável |

Total: **3.181 testes executados por rodada**, além da conferência visual nos três
perfis. O caminho completo local termina somente depois da decisão explícita da
fixture humana. Os estados simulados não comprovam a conclusão produtiva de Mira.

O teste de persistência foi executado também com a implementação anterior de
`ProcessRunService`: ambos os cenários falharam no retorno à produção, que permanecia
`BLOCKED`. Com a correção, passam. A rodada exploratória e os ajustes das fixtures
de navegador não entram na contagem das duas rodadas finais.

Evidências locais, sem dados comerciais de teste: `artifacts/mira-creative-recovery/`.
Os diretórios `final1` e `final2` contêm logs, contagens, contrato exportado pelo backend,
prévia e screenshots. Execução reproduzível: `bash infra/testing/mira-creative-recovery/run-round.sh <rodada>`;
a fonte aprovada e a especificação da prévia ficam em `diagnostic/asset-96.png` e
`diagnostic/replay-spec.json`. Requer MySQL local da topologia `infra/testing/process-automation/compose.yml`
e frontend compilado servido por `infra/testing/process-automation/frontend-server.mjs`.

Conferência publicada anterior à aplicação: execução 6 ainda `BLOCKED`, 1 objetivo
concluído, 4 restantes, 1 dispensa; custo conhecido USD 0,694708. Nenhuma tarefa paga
foi repetida durante o diagnóstico e a homologação local. A decisão humana de uso
permanece reservada ao usuário, depois das revisões independentes da peça corrigida.
