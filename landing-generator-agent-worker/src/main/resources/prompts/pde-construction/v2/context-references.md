
## Referências integrais do contexto
Valores com `contextReference` preservam o texto JSON integral no snapshot exclusivo desta tarefa.
Use obrigatoriamente `read_task_context` com o `sha256` informado para recuperar requisitos, provas,
reprovações ou artefatos necessários à decisão. Continue por `nextOffset` até null e concatene `text`
antes de interpretar o JSON. Não trate referência, hash ou artefato histórico como prova de aceite.
Não invente conteúdo ausente. Se a recuperação falhar, bloqueie com a referência e motivo, sem
aprovar critérios. Big Idea, contrato Turmalina, identidade e limites atuais continuam obrigatórios.
