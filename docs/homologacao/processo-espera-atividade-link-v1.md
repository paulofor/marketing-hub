# Espera de atividade e acesso à pendência

## Escopo e decisão

Correção de apresentação do contrato existente `automation/v1`: `WAITING_ACTIVITY`
era traduzido como “Em execução” e recebia animação contínua. O histórico em
`vega-ciclo2-espera-comercial-v1.md` confirma espera sem `userAction`.
`ProcessRunService.current` já fornece `navigationUrl` a partir do controle da atividade.
Não há necessidade de novo endpoint ou alteração de estado/backend.

Alternativas: trocar apenas o rótulo (baixo esforço, orientação insuficiente);
montar formulário pelo ID no frontend (atalho com risco de contexto incorreto);
reutilizar ação/destino oficiais junto do motivo (baixo esforço e preservação dos contratos).
Escolhida a terceira: ação específica tem prioridade, destino da atividade é fallback,
e ausência de URL é informada sem inventar formulário. Falha de atualização oculta o CTA.

## Matriz definida antes da validação

- Espera com destino oficial: rótulo claro, motivo, sem spinner, link preservando contexto.
- Espera com formulário: prioridade ao `userAction.actionUrl` e texto oficial.
- Espera sem destino: ausência explícita, sem formulário inventado.
- Falha de consulta: pedir atualização e ocultar ação potencialmente obsoleta.
- Comandos, retomada, histórico e progresso existentes preservados.
- Desktop, iPhone e Pixel em Chromium: renderização, link clicável e ausência de overflow.
- Integração local com componente real e API simulada; sem escrita em produção,
  gasto ou eventos comerciais. Métricas comerciais não são objeto deste ajuste.
- Duas rodadas consecutivas após a correção: testes do painel/contexto e navegação local.

## Resultados

- Rodada 1: 36 testes aprovados (20 painel + 16 contexto); 9 casos de navegação aprovados.
- Rodada 2: mesmos 36 testes e 9 casos de navegação aprovados, sem alteração de código entre rodadas.
- TypeScript: `npm run typecheck` aprovado.
- Navegação: componente React e CSS reais, API simulada, desktop 1440×900,
  iPhone 15 Pro e Pixel 7; três casos por dispositivo (pendência, formulário e ausência de destino).
  Links clicados e URLs conferidas, sem overflow. Captura do iPhone inspecionada.
- Testes unitários cobrem erro de atualização, prioridade do formulário, destino ausente,
  preservação do contexto, comandos e histórico. Nenhuma ação comercial disparada.
- O primeiro ensaio do harness de navegador interceptava também imports do Vite;
  a interceptação foi restrita ao endpoint HTTP local antes das rodadas aprovadas.
- `git diff --check` aprovado. Causa revisada: apresentação compartilhada corrigida
  para qualquer execução, sem exceções por produto/ciclo.

Comandos: `npm test -- --run src/pages/product/ProductProcessAutomationPanel.test.tsx
src/pages/product/productProcessContext.test.tsx` (em `frontend`) e
`node artifacts/activity-wait/browser.cjs rodada1` / `rodada2` com harness Vite local.
Capturas e harness preservados em `artifacts/activity-wait/` na sandbox.

Limites: validação de apresentação/navegação com dependências simuladas; não executa
formulário financeiro real nem comprova liberação de campanha. Backend e produção
não foram alterados. Disponibilização da interface depende de PR e deploy pelo fluxo autorizado.
