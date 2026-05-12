# Registros — Gera Landing

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

- 2026-05-12 09:25:00 UTC-3 — Atualizado o contrato da etapa landing-page-wireframe para formato JSON simplificado com raiz `pagina` (head/corpo/secoes/elementos), incluindo novo schema e regras de prompt alinhadas ao novo payload.

- 2026-05-12 09:40:00 UTC-3 — Ajustado o contrato para explicitar que `elementosInternos` em `elementosSeccao` é recursivo (filhos, netos e níveis seguintes com o mesmo schema), alinhando regra de composição de seção.
