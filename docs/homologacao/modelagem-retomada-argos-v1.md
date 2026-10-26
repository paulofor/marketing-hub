# Retomada de modelagem após reconexão de Argos — v1

Aceite: pesquisa inicial autenticada volta à mesma fila uma vez, preserva histórico e orçamento,
e só pode ser levada ao planejamento depois de produzir fontes válidas. Sem novas inferências
ou serviços externos durante a homologação local.

| Área | Caso | Critério |
|---|---|---|
| Caminho feliz | Recusa 401, sem plano, reconexão posterior confirmada | Nova ocorrência, mesma pesquisa e versão do processo |
| Validações | Sessão antiga/pendente, outra causa, outro agente/etapa | Comando recusado e motivo explícito |
| Saída/custo | Plano, resposta, tokens ou custo informado | Não repetir inferência; preservar recuperação específica |
| Concorrência | Dois comandos; replay em fila ou execução | Uma única nova ocorrência, sem duplicar pesquisa |
| Segurança | Produto em STOP | Não reabrir trabalho |
| Integração | Serviços reais, MySQL 5.7, dependências externas simuladas | Estado persistido, isolamento e dados após reinício |
| Observabilidade | Reconexão e tentativa | IDs/justificativa preservados; custo desconhecido permanece desconhecido |
| Métricas | Fila e passagens | Retomada não é dossiê, aprovação, venda ou margem |
| Segregação | Caso original e outras identidades sintéticas | Sem chamadas externas nem eventos comerciais de teste |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 | Comando claro, confirmação, carregamento, sem vazamento horizontal |

Regressões: `ModelingResearchRecoveryTest`, `ModelingServiceTest`,
`ProductDiscoveryBpmAuditServiceTest`, `ProductModelingPage.test.tsx` e matriz existente em
`infra/testing/product-modeling`. Evidências locais em `artifacts/product-modeling/local`.

## Resultado local — 26/10/2026

- 161 testes Java aprovados, incluindo 92 regras de arquitetura e regressões da descoberta,
  auditoria BPM, modelagem, adoção, retomada e STOP. Seis testes da tela, tipagem e build aprovados.
- MySQL 5.7: início e retomada concorrentes, replay, duas identidades, saída preservada,
  adoção e reinício aprovados. Nenhum resultado ou custo histórico foi reescrito.
- Chromium desktop/iPhone 15 Pro/Pixel 7: comando de retomada, confirmação e nova tentativa na
  fila aprovados; links usam o espaço da pesquisa. Capturas conferidas, sem rolagem horizontal.
- A fixture foi ajustada após a inspeção para mostrar também a nova ocorrência pendente,
  preservando a falha anterior. A matriz afetada foi repetida e aprovada.
- A arquitetura passou isoladamente com heap limitado após a interrupção por memória; o
  build frontend passou com heap de 2200 MiB no processo. Nenhuma ferramenta global foi alterada.
- Topologia temporária removida com volumes. Custo externo e eventos comerciais de teste: zero.

Limites: recuperação específica de recusa 401 anterior a plano/dossiê. Não recupera saída perdida,
não autoriza orçamento, não aprova dossiê e não comprova vendas. A retomada produtiva deve ser
confirmada separadamente pelo consumo da fila e pela entrega de Argos ao ciclo correto.
