# Cadeia v16 — preparação comercial por tipo no Processo 5

Data: 16–17/09/2026
Estado: implementação e homologação local concluídas; sem PR, deploy ou alteração produtiva.

## Decisão

Foram comparadas três alternativas antes da implementação:

| Alternativa | Benefício | Risco | Esforço | Decisão |
|---|---|---|---|---|
| Alterar a cadeia v15 e os processos publicados | Mudança visual imediata | Reescreve contrato usado pelo ciclo #2 e mistura evidência histórica | Baixo | Rejeitada |
| Criar cadeia v16, Processo 5 v7 e Processo 6 v8 | Preserva histórico, elimina a preparação tardia e permite rollback | Exige roteamento, navegação e ficha conscientes do tipo | Médio | Escolhida |
| Manter BPM e projetar 5.1 apenas na tela | Pouco impacto no backend | Corrige numeração, mas a execução continua no Processo 6 e duplica revisões | Baixo | Rejeitada |

A alternativa escolhida é a menor mudança que corrige a causa sistêmica sem migrar o ciclo #2.

## Matriz definida antes dos testes

| Dimensão | Cenário | Critério de aprovação |
|---|---|---|
| Caminho feliz | Produto Opala em nova execução da cadeia v16 | 5.1 delega ao Opala v1; 5.2/5.3 referenciam pareceres vigentes; 5.4 homologa; 5.5 aguarda decisão humana |
| Tipo e perfil | Produto de outro tipo | Não entra em Opala; rota ausente bloqueia com causa explícita |
| Escopo | Outro produto, ciclo, experimento ou versão | Evidência não é reutilizada |
| Mudança material | Destino, criativo, oferta, checkout, público ou economia mudou | Snapshot deixa de ser vigente e retorna à preparação/revisão afetada |
| Custo | Parecer de Psique/Têmis já concluído no Opala | Nenhuma nova tarefa paga; custo incremental pai igual a zero e fonte identificada |
| Homologação técnica | Evidência técnica vigente ou falha | Reutiliza quando válida; falha preserva causa e não libera 5.5 |
| Gate financeiro | Parecer de Plutus vencido, preço divergente ou margem/limite inválido | 5.5 indisponível |
| Decisão humana | Todos os requisitos verdes | Autoriza a versão/teto/janela; não antecipa `RUNNING` para Meta |
| Processo 6 | Nova cadeia v16 | Não contém chamada de preparação Opala e inicia após autorização |
| Histórico | Cadeias v14/v15 e ciclo #2 | Definições, numeração, tarefas, custos e aprovações permanecem inalterados |
| Retomada | Reexecução e replay | Não duplica ocorrência nem custo; subprocesso mantém vínculo com o pai correto |
| Falha | Rota inválida, versão não publicada ou JSON corrompido | Bloqueio diagnosticável, sem fallback para outro tipo/versão |
| Observabilidade | Relatório da execução | Guarda tarefa/instância fonte, impressão da prova e custo no subprocesso |
| MySQL 5.7 | Aplicação, segunda aplicação, rollback e reaplicação | Sem duplicidade; versões novas publicadas/retiradas corretamente; histórico preservado |
| Desktop | Chromium | Cinco atividades e links pai/filho legíveis e acionáveis |
| iPhone | Emulação iPhone 15 Pro | Sem corte de ação, numeração ou requisito |
| Android | Emulação Pixel 7 | Sem corte de ação, numeração ou requisito |

## Resultado

Duas rodadas completas e consecutivas foram executadas em topologias limpas depois do último ajuste
funcional. As duas foram aprovadas:

| Verificação por rodada | Resultado |
|---|---|
| Backend | 3.198 testes; 0 falhas; 0 erros; 16 ignorados previstos |
| Frontend | 167 arquivos e 702 testes; todos aprovados |
| Atena | 33 testes; todos aprovados |
| MySQL 5.7 físico | Aplicação, segunda aplicação, rollback e reaplicação aprovados; teste Opala executado sem skip |
| Liquibase | Includes relativos, campos temporais e prevenção do erro 1093 aprovados |
| Build | Spotless, typecheck e build de produção aprovados |
| REST e persistência | Decisão, retomada, concorrência, idempotência, segregação e bloqueios aprovados sem chamada externa |
| Navegação | Desktop, iPhone 15 Pro e Pixel 7 aprovados; 12 jornadas gerais, 18 verificações de cadeia e 21 verificações comerciais por rodada |

O contrato específico também comprovou:

- cadeia v16 com Processo 5 v7 e exatamente cinco atividades; Opala em 5.1 e homologação técnica em 5.4;
- Processo 6 v8 sem nova preparação Opala e iniciado somente após a ativação autorizada;
- seleção do subprocesso pela definição oficial do tipo `PDE`, versão exata e publicada;
- bloqueio explícito para produto sem tipo, tipo sem rota, rota inválida, versão não publicada e
  tentativa direta de outro tipo acessar Opala;
- reutilização idempotente de Psique e Têmis com tarefa e consolidação de origem, impressão SHA-256,
  evidência `REUSED_DIRECT` e custo incremental USD 0;
- invalidação da reutilização quando o snapshot Opala deixa de corresponder à configuração atual;
- gate 5.5 realmente bloqueado quando qualquer requisito, inclusive Plutus, não está satisfeito;
- numeração do ciclo #2 preservada em 5.2, sem migrar tarefa, custo, evidência ou aprovação histórica.

## Comparação com a versão anterior

| Critério | Cadeia v15 | Candidata v16 aprovada localmente |
|---|---|---|
| Momento da preparação Opala | Processo 6, depois da homologação/autorização | 5.1, antes das revisões e da autorização |
| Seleção pelo tipo | Vínculo fixo no Processo 6 | Rota versionada pelo tipo cadastrado e ficha congelada |
| Psique/Têmis | Risco de repetição no filho e no pai | Reutilização auditável no mesmo escopo, sem novo custo |
| Homologação técnica | Subprocesso sem posição coerente com atividades intermediárias | Posição real 5.4 |
| Autorização humana | Requisitos calculados, mas a prontidão não considerava todos | Todos os requisitos precisam estar satisfeitos antes de 5.5 |
| Processo 6 | Misturava preparação e operação | Começa após ativação autorizada |

Não houve alteração de prompt, modelo ou chamada paga. O aperfeiçoamento foi feito no harness do
backend: roteamento determinístico, atribuição da evidência, verificação de vigência, idempotência e
gate. Dédalo continua executando as quatro preparações do filho; Plutus, Psique e Têmis mantêm suas
decisões independentes, e o backend apenas coordena e reaproveita prova válida.

Evidências efêmeras das duas rodadas executadas depois do último ajuste:
`/tmp/cadeia-v16-definitive-round-1` e `/tmp/cadeia-v16-definitive-round-2`. O contrato e os
aprendizados reutilizáveis ficam versionados neste documento, no cânone da cadeia e no registro de
loops.

## Limites

- A homologação local não publica cadeia, processo, campanha nem mídia.
- Venda, receita e aumento de conversão continuam hipóteses até medição comercial real.
- A decisão humana de orçamento/publicação não é simulada como autorização produtiva.
- O ciclo #2 continua na cadeia histórica; a v16 passa a reger novas execuções somente depois do
  fluxo de PR e publicação autorizado.
