# Homologação da avaliação de mercado de Atena v2

Escopo: contrato obrigatório de revisão de mercado na decisão 6.4, planejamento de expansão
de Capella e cadastro administrativo. Não inclui veiculação, pagamento real ou oferta multissetor
publicada. Validar na sandbox antes de commit/PR. Dados simulados não são evidência comercial.

| Área | Critério de aceite | Validação local |
| --- | --- | --- |
| Caminho feliz | Conciliação → pending → request auditado → resposta v2 → revisão visível, sem executar decisão | Testes de serviço, worker com modelo/HTTP simulados e UI |
| Comparação | Exatamente três escopos distintos: foco, adjacentes e dor ampla | Teste de contrato e rejeição de duplicatas |
| Falhas | Ausência de revisão, campo extra, estado inválido, decisão incompatível, fonte de outro ciclo e resposta antiga recusadas | Testes negativos do backend |
| Expansão | Exige ADJUST, novo ciclo/experimento e retorno de estratégia; não permite SCALE/CONTINUE disfarçados | Testes de domínio |
| Histórico | Execução v1 já congelada continua válida; v1 não substitui pedido novo v2 | Replay v1 e teste de downgrade |
| Integrações | Polling existente, STOP, callback, timeout e auditoria preservados | Suíte do worker e testes do backend |
| Métricas | Critério comercial explícito; lacunas de custo/atribuição preservadas; QA segregado | Fixtures com baixa amostra e zero vendas |
| Interface | Revisão, adaptações e limites legíveis; histórico v1 identificado | Vitest e Playwright local |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, sem overflow | Playwright com APIs locais simuladas |
| Capella | Plano futuro identificado, oferta atual intacta, predecessor preservado, sem campanha/gasto | Leitura antes/depois pelo frontend e MCP |

Limite da homologação: test doubles comprovam contrato e fluxo, não qualidade futura de todo
parecer do modelo nem demanda dos segmentos. Mercado valida apenas comportamento voluntário,
venda reconciliada, entrega e contribuição. Não entrevistar, recrutar ou convidar testadores.

## Evidências locais — 03/10/2026

- Backend: 29 cenários do serviço de decisão e 92 regras de arquitetura aprovados; incluem
  revisão ausente/incompleta, escopos repetidos, escolha divergente, cliques como sucesso,
  expansão sem sucessor/retorno estratégico, compatibilidade v1 e proteção durante deploy.
- Worker: 45 testes Java; contratos v1/v2, header de capacidade, request anterior ao modelo,
  timeout, STOP e ferramentas restritas. O modelo da homologação é explicitamente simulado.
- Interface: 31 testes relevantes, TypeScript e build aprovados.
- HTTP + MySQL 5.7 local: seis grupos de verificações aprovados, incluindo concorrência,
  snapshot, atribuição, auditoria, retry, lease vencida, callback divergente e aprovação.
- Backend, worker real com modelo simulado e frontend integrados: decisão visível, edição,
  aprovação, persistência e ausência de overflow em Chromium desktop, iPhone 15 Pro e Pixel 7.
- Scripts shell usados como referência: `bash -n` e `shellcheck` aprovados. Contrato de agentes
  premium, formatação e `git diff --check` aprovados. Nenhuma campanha ou API paga usada.

Os dados sintéticos usam somente os produtos 91001/91002 da fixture e o MySQL efêmero da sandbox.
A validação preserva o histórico v1 sem inventar avaliação de mercado para execuções antigas.

O teste global `AgentHarnessCatalogTest` identificou na primeira rodada a ausência dos dois
novos arquivos no manifesto de comportamento. O manifesto foi atualizado no mesmo lote, para
que prompt e schema v2 também apareçam no detalhe de Atena com conteúdo e hash auditáveis.
A correção usa o catálogo existente; não cria um segundo registro de harness.

A suíte completa do backend executou 3.836 testes: o único achado foi a cobertura do manifesto
acima; 24 testes condicionais foram ignorados pelos seus pré-requisitos de ambiente. A correção
foi revalidada no catálogo afetado, sem repetir a matriz inteira. A integração pertinente desta
entrega com MySQL 5.7 foi executada separadamente e aprovada. Os três testes do adaptador MCP
local também passaram, sem consultar o provedor real.

O catálogo passou nos 16 testes após o ajuste. O JAR final foi conferido contra as 4.205 classes
compiladas e 744 recursos externos; os catálogos iniciaram pelo próprio JAR. O preenchimento do
cadastro de Capella e de Atena foi ensaiado na interface local com gravações interceptadas, mantendo
público comercial, oferta, preço, histórico, modelo, autoridade e permissões existentes.
