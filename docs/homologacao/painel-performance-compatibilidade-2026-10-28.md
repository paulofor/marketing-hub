# Compatibilidade das provas após otimizar o painel — 28/10/2026

O PR de desempenho altera a consulta administrativa de atividades em
`BusinessProcessActivityExecutionService`: quando há referência explícita, lê as
ocorrências desse contexto e evita carregar o histórico completo dos experimentos
no resumo. Os caminhos históricos sem referência e os critérios de aceite são
preservados. Não há mudança da experiência, do preço ou da autorização de produtos.

O primeiro CI detectou corretamente que o hash dessa fonte compartilhada na
atestação privada v57 de Mira não correspondia mais ao código. A compatibilidade
v58 preserva a v57 integralmente, registra seu SHA-256 e atualiza apenas a fonte
revalidada. Não flexibiliza o empacotador nem renova pareceres comerciais.

## Validação local

- 35 testes de execução de atividades, incluindo consulta por contexto explícito,
  leitura histórica e regressões que impedem voltar a carregar todo o histórico.
- Dez testes de revalidação de atividades PDE e cinco de contexto de ciclo,
  preservando isolamento, versões e encaminhamentos.
- 92 testes de arquitetura e as demais validações de desempenho registradas em
  `docs/registros/performance-painel-2026-10-28.md`.
- 38 testes do empacotador de provas e da seleção de publicação: versões históricas
  são imutáveis; compatibilidade não seleciona uma nova publicação de produto.

Os testes usam dependências locais ou simuladas, sem chamadas de IA, compras,
campanhas ou geração audiovisual. Esta prova não declara eficácia comercial nem
renova o estado operacional histórico registrado nos manifestos de Mira.

## Limites preservados

`mira-private-candidate.v58` mantém a experiência `mira-private-candidate-v3`,
`EVIDENCE_COMPATIBILITY_ONLY` e `automaticDeployOnMerge: false`. O contrato de
publicação, o fingerprint do frontend e as provas independentes continuam os
mesmos. O registro novo certifica somente compatibilidade da consulta compartilhada.

O empacotamento local conferiu 741 arquivos e 180 manifestos. A seleção da nova
atestação retornou `target=none`; o manifesto histórico foi comparado byte a byte
com o Git e permaneceu intacto.
