# Radar IA Autônoma — 2026-10-05

Relatório da rodada de 2026-10-05 17:45 America/Sao_Paulo.

## Resumo

A rodada identificou dois trabalhos recentes ainda não registrados neste radar: AIDE² (Weco AI) e SIFT (MIT + Sakana AI). Ambos mantêm os pesos congelados e melhoram persistentemente o software que organiza a execução do agente. AIDE² reescreve o próprio agente de pesquisa e seleciona versões por avaliações ocultas; SIFT reduz o custo de escolher candidatas usando comparações pairwise antes de avaliações completas.

## AIDE²

Na rodada principal, o sistema executou por oito dias, gerou 99 propostas de reescrita e aceitou sete melhorias sucessivas. O score privado do incumbent subiu de 0,703 para 0,778. O que persiste entre execuções é o código do harness: política de busca, manejo de contexto, memória e verificações. Os pesos dos modelos permanecem congelados. Entre os mecanismos descobertos aparecem busca por linhagens, forks para escapar de platôs e compressão de contexto de aproximadamente 16x. Os ganhos transferiram para benchmarks externos, incluindo WeatherBench 2 fora da distribuição de seleção.

## SIFT

O SIFT, de MIT e Sakana AI, usa comparações pairwise e Bradley-Terry para priorizar quais versões candidatas merecem avaliações mais caras. No Polyglot-225, uma configuração Qwen3-Coder-30B chegou a 31,1%; com o3-mini, o resultado reportado chegou a 35,1%. Uma busca Qwen de 30 expansões consumiu cerca de 224 CPU-hours, 6,7 horas de wall-clock e US$34,3 em API.
