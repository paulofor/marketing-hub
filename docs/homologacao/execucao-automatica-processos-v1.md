# Execução automática dos processos — homologação local

## Decisão e evidências

Em 12/09/2026 o usuário passou a iniciar processos completos. A execução continua alocada
nos BPMs e nas cadeias existentes. A captura anexada identifica o cabeçalho da tela do produto.
O MCP confirmou as tentativas 379–399: falhas e sucessos do mesmo ciclo convivem no histórico;
novos comandos não podem reutilizar falha antiga como estado atual nem repetir trabalho sem
mudança de entrada. Referência: `docs/registros/loops.md`, loops de Vega, tarefas repetidas e
acompanhamento com retransmissão de prompts.

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Coordenar por eventos e outbox nos callbacks existentes | Reage imediatamente a cada resultado | Exige integrar todos os callbacks legados e tratar entrega duplicada; esforço maior | Evolução possível |
| Introduzir motor externo de workflows | Infraestrutura especializada | Migração de contratos e nova infraestrutura operacional | Não agora |
| Coordenador persistente no backend e conciliador externo | Reutiliza filas, gates, auditoria e banco | Esforço moderado; exige testes de transação e retomada | Sim |

O módulo externo somente consulta pendências e pede conciliação. A escolha e o disparo da
atividade são do backend. Não cria modelo de IA, consulta de banco ou integração entre workers.
Cada conciliação faz no máximo um disparo; o resultado funcional determina a passagem seguinte.
Execuções do mesmo produto são serializadas; produtos diferentes avançam em paralelo.

Para concorrência, foram comparados **lock por produto** (baixo esforço e protege as entradas
compartilhadas), **lock por ciclo/experimento** (mais paralelismo, mas versões do produto ainda são
compartilhadas) e **reserva por artefato/atividade** (maior paralelismo e maior custo de manutenção
do grafo de recursos). A primeira alternativa foi escolhida para v1: não reduz qualidade por
competição de escrita e permite escalar entre produtos. Não reserva o produto por tempo máximo;
a pausa espera o trabalho já iniciado e uma falha não é convertida em sucesso por expiração.

O heartbeat confirma comunicação com o backend antes do lote e entre resultados válidos. Assim,
um lote demorado não bloqueia a inicialização saudável do conciliador. A conciliação consulta
somente a identidade antes do lock; o estado mutável é lido depois, inclusive com contexto JPA
mantido durante a requisição. Provas invalidadas permitem revalidação sem apagar conclusões passadas.

## Matriz definida antes dos testes

| Dimensão | Critérios |
| --- | --- |
| Caminho feliz | Clique no processo, fila, tarefa, callback, próximo trabalho, gate e conclusão funcional |
| Ordem | Grafo com atividades fora da ordem do JSON; predecessoras, ramo condicional e recuperação explícita |
| Completude | Toda atividade TASK do BPM possui definição e projeção da versão exata; contrato incompleto impede conclusão |
| Qualidade | Tarefa tecnicamente encerrada sem objetivo não conclui processo; correção e revalidação preservam evidências |
| Revalidação | Invalidação posterior não permanece exibida como aprovação atual; retomada conserva as conclusões anteriores |
| Repetição | Duplo clique, múltiplos conciliadores, resposta perdida e reinício não duplicam tarefa |
| Falhas | Erro transacional não perde diagnóstico; repetição sem progresso bloqueia com causa; retomada explícita |
| Controle | Pausa não cancela tarefa em andamento; retomada usa seu resultado; produto STOP impede novos disparos |
| Encerramento | Ciclo encerrado, produto STOP ou pai concluído permitem registrar provas já obtidas; ciclo fechado com objetivos pendentes encerra o controle sem sucesso e libera a fila |
| Isolamento | Produtos em paralelo, fila por produto, processo/versão/cadeia/ciclo/referência exatos e contexto divergente recusado |
| Autorização | Decisões humanas, formulários, publicação e gasto preservam contratos; iniciar processo não confirma decisão |
| Subprocessos | Delegação contextual, retomada do pai, ausência de contrato identificada sem sucesso artificial |
| Persistência | MySQL 5.7, migração e reaplicação, unicidade e transações concorrentes; recuperação após reinício |
| Catálogo publicado | Topologia de todos os 12 processos PRODUCT publicados, consultados no MCP em 12/09/2026; fixture somente estrutural |
| Imagem do conciliador | Dockerfile versionado, Node 22, usuário restrito, filesystem somente leitura, token em arquivo, heartbeat e parada graciosa |
| Observabilidade | Progresso persistido, atividade/responsável, horários, tarefa, motivo, histórico e custo com cobertura explícita |
| Interface | Loading, erros/retry, navegação/reload, aba fechada, painel no cabeçalho e ausência de disparos individuais automatizáveis |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, teclado, movimento reduzido e sem overflow |
| Métricas | IDs e banco locais segregados; conclusão técnica não conta como venda; IA e serviços externos simulados |

Uma rodada sem defeitos encerra a homologação. Se houver correção, executar duas rodadas
completas consecutivas sem falhas após a última mudança. Não usar publicação como teste.

## Resultados

Implementação recuperada em 12/09/2026 do workspace preservado após a interrupção. A revisão
encontrou evidências das rodadas anteriores, mas houve ajuste entre elas; a contagem final foi
reiniciada após os ajustes desta revisão. O runner agora reconstrói a interface em cada rodada,
para não validar um bundle anterior. Banco e tela publicados foram consultados somente para leitura:
as tentativas #380/#385 bloqueadas convivem com #394/#396–#399 concluídas no experimento #92,
confirmando a necessidade de preservar histórico e usar o resultado funcional vigente.

A revisão reproduziu um defeito de completude: um TASK sem projeção não interrompia a
ordenação. O teste `rejectsMissingOrUnmappedActivityContracts` falhou antes da correção e passou
após conferir cobertura, duplicidade e identidade, preservando atividades condicionais legítimas.
O mesmo contrato é validado antes da autorização e da apresentação de conclusão.

A revisão visual da primeira rodada também ajustou a mensagem permanente do cabeçalho: ela
informa que progresso e histórico ficam salvos, sem anunciar execução ativa quando o backend
está em `ERROR`. A regressão cobre esse estado. As rodadas finais válidas são as executadas
depois desse ajuste, incluindo nova compilação da interface em cada uma.

A homologação reproduziu quatro falhas no reconhecimento de resultados após encerramento:
os bloqueios de novos disparos eram consultados antes das provas já persistidas. O histórico
do MCP contém o ciclo 1 do Vega `ADJUSTED` com `closed_at` preenchido; o contrato existente
de `LearningCycleService.close` e a projeção de subprocessos confirmam que fechar o ciclo
faz parte do caminho legítimo. Não é correto exigir um ciclo aberto para reconhecer seu resultado.

Foram comparadas três soluções: **finalizar por callbacks de cada módulo** (resposta imediata,
mas acoplamento e maior esforço), **uma fila exclusiva de encerramentos** (isolamento, mas mais
infraestrutura e risco de divergência) e **conferir as provas antes de autorizar novos disparos**
(baixo custo e mesma fonte de verdade). A terceira foi adotada. Se faltarem objetivos em um
ciclo fechado, o controle fica `CLOSED`, apresentado como “Encerrado com pendências”, conserva
as contagens e libera a fila do produto. Não gera tarefas, não oferece retomada do ciclo fechado
e não conta como conclusão. Se todas as provas existirem, registra `COMPLETED`, inclusive
depois de STOP, pausa ou conclusão do processo pai. A matriz física cobre seis cenários desse
ciclo de vida, além dos dezessete cenários gerais da API.

### Rodadas finais

| Verificação | release-1 | release-2 |
| --- | --- | --- |
| Backend completo | 2.708 executados, 7 exclusões existentes, sem falhas | Mesmo resultado, sem falhas |
| Interface | 605 testes, TypeScript e build aprovados | Mesmo resultado, sem falhas |
| Conciliador | 6 testes; imagem Node 22 e parada graciosa aprovadas | Mesmo resultado, sem falhas |
| API e ciclo de vida no MySQL 5.7 | 17 + 6 cenários aprovados | 17 + 6 cenários aprovados |
| Reinício e reaplicação Liquibase | Aprovados, sem duplicação nem perda de histórico | Mesmo resultado, sem falhas |
| Navegação | Desktop, iPhone 15 Pro e Pixel 7 emulados aprovados | Três configurações aprovadas |
| Entrega versionada | Contratos de deploy e 3 testes de empacotamento aprovados | Mesmo resultado, sem falhas |

Os 61 arquivos de implementação, contratos e regressões foram congelados antes das rodadas;
as impressões SHA-256 estão em `artifacts/process-automation/review/release-source-sha256.json`.
Este relatório de resultados é atualizado depois das execuções, sem alterar a implementação.
Logs, contagens e capturas ficam em `artifacts/process-automation/release-1/` e `release-2/`.
As duas rodadas completas consecutivas terminaram sem falhas e sem mudanças de código entre
elas. A verificação consolidada está em `artifacts/process-automation/review/final-verification.json`.
Spotless dos arquivos Java alterados, sintaxe dos scripts, YAML, contratos de workflow e revisão
do diff também foram aprovados. O actionlint mantém somente a exceção para `concurrency.queue`,
chave preexistente na revisão base e ainda não reconhecida pela versão local dessa ferramenta.
As topologias temporárias e suas imagens de teste foram removidas ao final de cada rodada.

A integração usa o coordenador, controllers, transações, JPA, MySQL 5.7 e conciliador reais.
As filas e respostas dos agentes legados são simuladas em adaptadores locais. Não chama IA,
campanhas, cobrança ou serviços produtivos. As doze topologias publicadas são verificadas como
contratos estruturais; isso não representa doze processos comerciais executados em produção.
Os três dispositivos usam Chromium com emulação mobile; não representam Safari físico.

Nenhum PR, push, deploy ou alteração de produto produtivo realizado por esta implementação.
