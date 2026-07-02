# Registro OPRM NichoCNAE v3 — 2026-07-02

## Busca curta e nome real de persona materializada

- Diagnóstico: o NichoCNAE ainda podia consumir tempo excessivo no `source-searcher` quando recebia várias queries e variações.
- Diagnóstico complementar: a etapa final podia materializar nicho como `CNAE <codigo> — <descricao>` mesmo quando o executor já entregava persona operacional útil.
- Causa-raiz: a busca pública tentava variações demais antes de parar, enquanto o backend lia apenas campos de topo do payload final.
- Correção: o `source-searcher` limita queries/variações e para cedo quando encontra fontes qualificadas suficientes.
- Correção: o backend passa a ler `marketNicheCandidate.title` e `materializedProfile.personaName` antes do fallback fiscal do CNAE.
- Prevenção: testes cobrem parada antecipada da busca e materialização com payload funcional aninhado do executor.
