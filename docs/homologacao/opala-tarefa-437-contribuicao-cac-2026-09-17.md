# Homologação — Opala tarefa 437 e conclusão econômica do Vega v12

Data: 17/09/2026  
Produto: Vega (4)  
Ciclo: 2  
Experimento: 92  
Processo: Preparar operação comercial Opala v1 (77)  
Atividade: `economics` — Validar custos, limites e margem

## Resultado

A atividade econômica foi concluída na ocorrência BPM 296 pela tarefa 439. O histórico preserva
as tarefas 435, 436, 437 e 438 como bloqueadas, com seus resultados e custos. A visão operacional
passou de 4/8 para 5/8 atividades concluídas e selecionou `humanExperienceReview` como próxima
atividade, sem autorizar campanha, publicação, cobrança ou gasto de mídia.

O parecer aceito registra somente viabilidade projetada do piloto:

- preço: R$ 67,00;
- custo variável por venda, sem CAC: R$ 42,82;
- contribuição antes do CAC: R$ 24,18;
- margem antes do CAC: 36,08%;
- CAC máximo separado: R$ 15,00;
- contribuição projetada após CAC no cenário-base: R$ 9,18;
- orçamento máximo do ciclo: R$ 100,00;
- validade do parecer: 24/09/2026.

## Causas-raiz e correções

1. O prompt e os validadores usavam posições diferentes da ponte econômica. Worker, backend e
   prompt Opala v4 agora usam contribuição antes do CAC e mantêm aquisição separada.
2. A tentativa 438 herdou o prompt v1 já fixado na ocorrência. Cada tarefa nova passa a fixar a
   versão ativa e revisada na criação, sem alterar tentativas históricas.
3. A tarefa 439 e a instância 296 foram concluídas, mas a projeção reabriu a atividade porque
   comparava nós JSON por tipo e escala. Preço e orçamento agora são comparados por valor decimal;
   o plano financeiro, por sua identidade imutável `id + revision`.

## Decisões comparadas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Aceitar contribuição antes ou depois do CAC | Mudança pequena | Mantém contrato ambíguo e pode duplicar aquisição | Rejeitada |
| Forçar contribuição pós-CAC em todos os campos | Aproxima o resultado líquido | Mistura custo variável com CAC e mascara a ponte econômica | Rejeitada |
| Padronizar contribuição antes do CAC e CAC separado | Reconciliação determinística e auditável | Exige alinhar prompt, worker, backend e testes | Adotada |
| Comparar o snapshot JSON integral | Detecta qualquer diferença textual | Reabre atividade por `67` versus `67.0` | Rejeitada |
| Normalizar recursivamente todo o JSON | Preserva equivalência numérica | Complexidade ampla e risco fora do domínio | Rejeitada |
| Usar revisão imutável e valores decimais materiais | Detecta mudança real sem falso positivo | Depende da imutabilidade já garantida pelo plano | Adotada |

## Homologação local

Após a última correção foram executadas duas rodadas completas e consecutivas:

- backend: 3.207 testes por rodada, zero falhas e zero erros; 17 ignorados previstos;
- Plutus: 51 testes por rodada, zero falhas e zero erros;
- Spotless e `git diff --check` aprovados;
- regressão aceita escalas numéricas equivalentes e exige nova análise para outra revisão;
- aplicação do catálogo no MySQL 5.7 já havia sido executada duas vezes após a ativação do prompt
  v4, preservando a versão anterior e fixando a nova em retries futuros.

## Intervenção homologada

- backend temporário: `marketinghub-backend:task437-f4dbb631c723`;
- SHA-256 do JAR local e remoto:
  `589fa9e4411d3c3d475dbfaca6f7d81f2041606ab7e829ed2d56b858590731a9`;
- health oficial: `UP`, com MySQL `UP`, e zero reinícios após a troca;
- frontend real conferido em desktop, iPhone 15 Pro e Pixel 7: 5 concluídas, 5 de 8, atividade
  econômica concluída e próxima atividade exibida;
- custo das tentativas 435–439 preservado: USD 2,370112; tarefa 439: USD 0,6296552.

A intervenção permanece protegida pelo coordenador
`1d50447fad9f4c0ca78feeff519a1762`. Ela não substitui o Pull Request. Os publicadores devem
continuar pausados até o commit exato desta revisão ser integrado à `main` e comprovado pelo
reconciliador.

## Pendência posterior, fora da atividade 437

O processo Opala está em 5/8 e aguarda `humanExperienceReview`. A prontidão registrada informa
pendências reais de versão comercial, entrada do PDE e criativo aprovado. Essas pendências não
invalidam a conclusão econômica e não autorizam repetir Plutus.
