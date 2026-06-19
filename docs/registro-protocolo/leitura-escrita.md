# Registro do protocolo leitura escrita

## 2026-06-19 — OPRM NichoCNAE v2
- Pacote backend protegido: `com.marketinghub.oprm.nichocnae.v2..`.
- Módulo executor externo responsável pelo controle operacional: `oprm-coletor-mei`.
- Regra aplicada em `ArquiteturaTest` para manter a v2 do backend restrita a leitura, escrita, contratos, persistência, publicação de pendências e recebimento de callbacks, bloqueando responsabilidades de execução operacional como `@Scheduled`, polling, workers/runners/processors, núcleo `Stage*` e tecnologias externas de execução.
