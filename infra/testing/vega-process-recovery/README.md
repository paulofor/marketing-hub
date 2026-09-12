# Homologação do processo privado do Vega

A matriz usa Node 22, Java 21, Docker Compose v2 e um projeto Compose exclusivo da sandbox.
Não usa banco, filas, SMTP ou tarefas de produção. Os agentes e o armazenamento são simulados.
A prova de origem é somente leitura: PNG desktop já aprovado da homologação técnica da v12,
obtido pelo endpoint oficial da tarefa e conferido pelo SHA-256 persistido.

Antes de iniciar, configure `PROCESS_COMPOSE_PROJECT` com o projeto exclusivo fornecido pelo
ambiente e `VEGA_CREATIVE_SOURCE` com o caminho absoluto desse PNG dentro do workspace.
Para cada rodada, configure `VEGA_CREATIVE_PREVIEW` como caminho absoluto de saída, por exemplo
`$PWD/artifacts/vega-process-recovery/rodada-local-1/creative-preview.png`, e execute:

```bash
bash infra/testing/vega-process-recovery/run-round.sh rodada-local-1
```

Use outro nome e outra saída na segunda rodada. A imagem real não é versionada: os testes Java
usuais também usam pixels sintéticos e não precisam desse arquivo. O teste visual completo da
matriz usa a prévia produzida pelo compositor Java para conferir os pixels na interface real em
desktop, iPhone 15 Pro e Pixel 7. As rotas de API são interceptadas localmente antes de qualquer
conexão; escritas e demais conexões externas são recusadas.

O relatório funcional, os contratos, as alternativas consideradas, os limites e os resultados
ficam em `docs/homologacao/vega-processo-comunicacao-recuperacao-v1.md`. Cada rodada salva logs,
contagens e capturas em `artifacts/`. A aprovação local não autoriza uso comercial, publicação de
campanha ou cobrança.
