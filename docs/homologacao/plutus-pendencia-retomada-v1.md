# Pendência financeira de Plutus e retomada — v1

## Escopo e reprodução — 04/11/2026

Capella, produto 7, cadeia 27, ciclo 20, experimento 113, versão
`capella-beleza-v4`, processo 126 v13, execução 97, atividade `economics`.
A tela e o banco confirmaram `PAUSED`, sem aprovação vigente, com `canResume=true`.
No mesmo produto, a execução 95 do ciclo 19 está concluída; seus recibos não
autorizam o ciclo 20. Não havia tarefa de Plutus no ciclo 20 na leitura.

O guard financeiro já protege o disparo de agentes, mas não a projeção do comando
de retomada. O resumo financeiro genérico fica distante da atividade.

## Matriz de aceite local (definida antes da execução)

| Cenário isolado | Resultado exigido |
| --- | --- |
| Sem aprovação; custo conhecido ou desconhecido | Informar aprovação ausente, sem afirmar falta de saldo; link do ciclo e seção AGENTS; sem retomada |
| Valor digitado, visita ou edição sem gravação | Permanecer sem elegibilidade e sem alteração no servidor |
| Recibo de outro ciclo, produto, cadeia ou versão | Não transferir autorização; distinguir aprovação desatualizada da ausência |
| Recibo vigente sem cobertura | Informar limite insuficiente, preservar cálculo e histórico; sem retomada |
| Custos totalmente desconhecidos | Mostrar lacuna e responsável, sem saldo inventado |
| Aprovação persistida e retorno à atividade | Reconsultar servidor; exibir retomada somente elegível; não retomar automaticamente |
| Erro de gravação, consulta ou revalidação | Mensagem de negócio; ocultar elegibilidade incerta, mesmo com cache anterior |
| Alteração concorrente entre consulta e comando | Revalidar no POST e recusar sem tarefa, custo ou transição adicional |
| STOP, ciclo fechado, versão retirada, atividade indisponível | Continuar bloqueado apesar de aprovação financeira |
| Parecer vigente e histórico | Síntese e ajuste junto da atividade/parecer, mantendo texto, provas e custos originais |
| Outro produto e caminho antes válido | Mesmas regras sem exceção por nome/ID; operação determinística e reúso preservados |
| Desktop Chromium, iPhone 15 Pro e Pixel 7 | Aviso e link visíveis junto da atividade; navegação contextual e sem overflow |

Testes usam dados locais/TEST e integrações simuladas, sem credenciais de IA,
inferência, aprovação de valores reais, retomada produtiva, campanha ou deploy.
As métricas são correção da elegibilidade e ausência de efeitos por leitura;
nenhum teste comprova venda, demanda ou aumento de contribuição.

## Alternativas e escolha

Somente copy: baixo esforço, mas manteria o comando incorreto. Consulta financeira
paralela no frontend: mais coordenação e risco de divergência. Ampliar o guard
existente e reutilizar sua projeção na tela/POST: esforço moderado e uma única
regra financeira. Adotada a terceira alternativa, preservando cálculos e recibos.

## Resultado observado

- 385 testes funcionais do backend e 92 verificações globais de arquitetura passaram.
- 105 testes de frontend passaram; `typecheck` e build concluídos.
- Chromium desktop 1440×1000, iPhone 15 Pro e Pixel 7 passaram no percurso:
  atividade → demonstração → rascunho → falha de gravação → aprovação TEST →
  retorno → retomada disponível, sem chamada automática de retomada.
- Parecer de outra referência permanece íntegro e identificado como histórico;
  a mesma pendência e link aparecem junto da análise. Nenhum parecer foi fabricado.
- A execução combinada atingiu o limite de memória na arquitetura, após 385 testes
  funcionais sem falhas. A separação em dois processos concluiu ambas as suítes
  com sucesso; não houve mudança de regra para contornar teste.

O harness de regressão passa a consultar e comandar o mesmo guard. As fixtures
antigas que permitiam retomar sem recibo agora exigem `canResume=false` e HTTP 409,
sem nova tarefa. O seletor de próxima atividade também é compartilhado com o despacho.
A métrica futura sugerida é reduzir retomadas recusadas e tempo parado por pendência
financeira, sem alegar impacto comercial ainda não medido.

### Evidências revisáveis

- [Relatório local e hashes das fontes](evidencias/plutus-pendencia-retomada-v1/local-report.json).
- [Mensagem em desktop](evidencias/plutus-pendencia-retomada-v1/desktop-plutus-notice.png).
- [Mensagem em iPhone](evidencias/plutus-pendencia-retomada-v1/iphone-plutus-notice.png).
- [Mensagem em Pixel](evidencias/plutus-pendencia-retomada-v1/pixel-plutus-notice.png).
- [Pendência junto do parecer preservado](evidencias/plutus-pendencia-retomada-v1/desktop-plutus-report.png).
- [Retomada elegível, ainda pausada](evidencias/plutus-pendencia-retomada-v1/desktop-plutus-eligible.png).

### Reprodução local

No módulo `backend/ads-service`, executar as suítes `ProcessRun*Test`,
`ProcessAutomation*Test`, `ProcessExecutionGraphTest`, `LearningCycleFinancialReviewTest`,
`LearningCycleCostWorksheetTest` e `LearningCycleVideoBudgetTest`. Executar
`com.marketinghub.architecture.ArquiteturaTest` separadamente em ambiente de memória limitada.
O parâmetro Maven `-Dplutus-resume.fixture-output=<caminho-absoluto>` exporta os contratos
HTTP apurados pelo teste `ProcessRunFinancialEligibilityTest`, com recibo real em H2.

Depois do build frontend, iniciar `node infra/testing/process-automation/frontend-server.mjs`
e executar `infra/testing/process-automation/plutus-financial-browser.mjs` com
`PLUTUS_RESUME_FIXTURE` apontando à exportação e `PROCESS_TEST_ARTIFACTS` ao diretório de
saída. O navegador intercepta integrações externas e não envia comandos a produção.
Dados e aprovações da demonstração são TEST. Nenhum changelog, valor financeiro real,
parecer produtivo, execução, campanha, imagem publicada ou deploy foi alterado.
