# Conciliador de processos v1

O usuário executa o processo no cabeçalho da tela de atividades. O backend persiste a autorização,
ordena o trabalho pelo BPM e reutiliza os contratos dos agentes e gates. Este módulo consulta
`pending` e solicita conciliação; não decide atividades, não acessa banco, não chama outros workers.

O intervalo e a concorrência ficam neste executor. O estado fica no backend: reiniciar o módulo ou
fechar o navegador não perde progresso. O backend serializa o mesmo produto; produtos diferentes
podem avançar em paralelo. Pausa aguarda tarefas em curso sem cancelá-las. Falta de prova não é sucesso.

- `BACKEND_BASE_URL`: backend principal.
- `PROCESS_EXECUTION_WORKER_TOKEN` ou `PROCESS_EXECUTION_WORKER_TOKEN_FILE`: credencial exclusiva.
- `PROCESS_EXECUTION_CONCURRENCY`: concorrência de conciliação, padrão 4, máximo 16.
- `PROCESS_EXECUTION_BATCH_SIZE`: lote, padrão 20, máximo 100.
- `PROCESS_EXECUTION_POLL_MS`: intervalo entre lotes, padrão 5000.

Validação: `npm test` neste módulo. A matriz HTTP com backend e MySQL reais está em
`infra/testing/process-automation/api-matrix.mjs`. O Dockerfile não instala dependências externas.

Publicação: a imagem é construída junto ao backend pelo workflow existente `deploy-containers.yml`.
O `deploy/bin/apply.sh` prepara/reutiliza uma credencial protegida sem exibi-la, atualiza o backend,
verifica sua saúde e só então inicia o conciliador. Rollback interrompe o conciliador até nova
publicação compatível, preservando o diário. Nenhuma nova workflow publicadora fica fora do
coordenador de intervenções. A entrega exige o fluxo de PR do usuário.
