# AI Workers Canon v1

## 1. Propósito

Definir regras canônicas para integrações dos módulos de AI Worker com modelos OpenAI, incluindo parâmetros operacionais obrigatórios para execução em lote (batch).

## 2. Escopo

Este documento cobre:

- regras operacionais mínimas para chamadas OpenAI em workers de IA;
- padronização de timeout para processamento batch;
- referência única para evitar drift entre configurações de ambiente e implementação.

## 3. Regra canônica de timeout para OpenAI Batch

Para toda integração do **Worker AI** com OpenAI em **modo batch**, o timeout de acesso/processamento deve ser **sempre de 30 minutos**.

Regras mandatórias:

1. valor canônico fixo: `30 minutos` (`PT30M`);
2. não é permitido reduzir esse timeout em código, configuração local ou variável de ambiente;
3. qualquer exceção futura exige nova versão deste cânone e atualização explícita dos módulos consumidores.

## 4. Conformidade operacional

- Backend e Worker AI devem manter o mesmo valor de referência para evitar divergência de estado entre job e polling.
- Logs de timeout devem registrar ao menos: `jobId`/`batchId`, `model`, `timeoutConfigured`, `timestamp`.

## 5. Referências normativas

- `docs/canonical/system-governance-canon.v2.md`
- `docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md`
