# Registro de Ajustes do Pipeline de Experimento

Este documento é a base oficial para registrar **todo ajuste realizado no pipeline de experimento**.

## Objetivo

- Centralizar histórico de mudanças.
- Manter rastreabilidade entre problema identificado, ajuste aplicado e validação.
- Facilitar alinhamento entre backend, frontend, workers e documentação canônica.

## Como usar

1. Adicione uma nova linha na tabela **sempre que um ajuste for iniciado, atualizado ou concluído**.
2. Referencie PR, commit e arquivos impactados.
3. Informe se houve atualização dos documentos canônicos e testes relacionados.
4. Não remova entradas antigas; marque como substituída quando necessário.

## Tabela de ajustes

| Data (UTC) | Módulo | Ajuste | Motivo / Problema | Ação executada | Status | Evidência (PR/commit/log) | Cânone/Testes atualizados? | Responsável |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| YYYY-MM-DD | backend / frontend / ai-worker / facebook-ads-worker / outro | Resumo curto do ajuste | O que motivou a mudança | O que foi feito objetivamente | Planejado / Em andamento / Concluído / Bloqueado | Link ou referência | Sim / Não / N/A | Nome |

## Checklist obrigatório por ajuste

- [ ] O ajuste está alinhado ao eixo **Dor → Resultado → Mecanismo → Prova → Oferta**.
- [ ] O contrato/artefato canônico correspondente foi revisado.
- [ ] Os módulos impactados foram mapeados.
- [ ] Os testes necessários foram executados/atualizados.
- [ ] A evidência técnica foi registrada (PR, commit, logs, payloads quando aplicável).

## Observações

- Em casos de erro `422 Unprocessable Entity`, registre explicitamente:
  - payload enviado;
  - validação/contrato esperado;
  - diferença exata;
  - ação corretiva aplicada.
- Evitar `json` em campo textual de outro `json` (json dentro de json).
