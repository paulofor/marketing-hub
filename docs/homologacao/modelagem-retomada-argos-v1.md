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


## Recuperação da coleta após plano pronto — 26/10/2026

O caso Vega revelou divergência de contrato: o produtor oferecia `PUBLIC_BROWSER` para ciclo `UNSPECIFIED`, mas o callback aceita somente B2C/Instagram. O plano e os tokens estavam preservados na tentativa #819, sem síntese. Compararam-se três opções: forçar B2C (altera o escopo de pesquisas futuras), duplicar a política no executor (permite divergência) e centralizar a regra no backend (adotada). Pesquisas não elegíveis continuam com fontes públicas e histórico disponível, sem alegar ausência de anúncios.

Critérios adicionais de aceite local, antes de nova retomada real:

| Cenário | Verificação |
|---|---|
| Elegibilidade | Produtor e callback compartilham a regra; B2C/Instagram continua válido; B2B e UNSPECIFIED não recebem coleta recusável. |
| Plano preservado | Pesquisa original e outra identidade retomam coleta e síntese, com zero invocações de planejamento. |
| Compatibilidade | Executor antigo não reserva recuperação que desconhece; executor novo declara `supportsPlanReplay=true`. |
| Auditoria | Plano/resposta/modelo originais permanecem iguais; uso anterior é estimado pelo catálogo, sem API de cobrança e sem somar tokens novamente. |
| Falhas | Plano de outro ciclo, plano inválido, análise já pronta, outra falha e tarefa ativa não autorizam recuperação. |
| Integração | Pending → plano recuperado → fontes → síntese → callback terminal usa a mesma pesquisa, lease e um resultado terminal. |
| Isolamento e regressão | Identificadores distintos, fluxo normal anterior, STOP e limites mantidos; dados sintéticos não entram em métricas comerciais. |
| Tela | Mesmo comando de retomada explícita; desktop, iPhone e Pixel preservam contexto e mostram motivo da recuperação. |

Testes locais verificam comportamento, não demanda, vendas ou efeito comercial. Não há nova autorização de mídia ou audiovisual.

Resultado local: 102 testes iniciais de backend e 165 de executor aprovados; 92 regras de arquitetura aprovadas. Matriz MySQL 5.7, reinício e navegação desktop/iPhone/Pixel PASS, com topologia removida. Revisão final acrescentou regressão para distinguir replay de ampliação determinística normal; somente os contratos afetados foram revalidados.


## Cobertura de referências na pesquisa — 26/10/2026

O relatório real expôs zero fontes de Whering/Indyx entre 30 selecionadas apesar do plano explícito. Matriz local adicional: domínio declarado antes do encerramento por limite; uma fonte observada por referência mantida mesmo com várias ofertas; outro produto com domínios diferentes; ausência de retorno sem fabricar evidência; fluxo anterior sem referências. Provider é test double, sem custo externo. Reproduções e resultados: `reference-search-coverage.test.js`; corpus do plano em `fixtures/research-plan-replay.json`.


Complemento após timeout #821: planejador determinístico existente para aprofundamento público com referências, sem nova inferência de planejamento; fontes distribuídas entre candidatas sem duplicar consulta; até cinco referências e três candidatas; análise factual mantém modelo e gates. Plano produzido pelo Node aceito por `ProductDiscoveryGapResearchContractService.validatePlan` real em sandbox; 171 testes do executor PASS. O timeout histórico permanece com custo não informado, sem atribuir zero.


## Adoção do estudo internacional — 26/10/2026

Antes dos testes desta passagem: conferir estudo `RESEARCHABLE` com duas fontes primárias e contraponto; recusar fonte ausente, domínio falso, data/texto nulo, limite vazio, rejeição, revisão humana e escopo antigo; preservar `DOSSIER_READY` anterior. Percorrer salvar → iniciar → concluir pesquisa sintética → vincular → consultar contexto → replay/reinício no MySQL 5.7 e na tela em desktop/iPhone/Pixel. Verificar Big Idea, identidade, maturidade, hipóteses, limites e zero tarefa/custo na adoção. Conferir o contrato consumidor de Atena com fontes literais e três alternativas. Antes: os dois casos de estudo retornavam 409 localmente e o caso real também retornou 409 pela tela. Depois: somente estudo factual completo recebe a base preparatória, sem alterar prontidão comercial.

## Formato estrito de Atena — 26/10/2026

Reproduzir antes da correção a falta de dois campos em `required` no schema v12. Depois,
percorrer recursivamente todos os schemas empacotados, inclusive objetos anuláveis. No processo
Codex simulado e HTTP local, verificar o arquivo efetivamente enviado, auditoria, resultado,
perda/replay de callback e Big Idea fixada em duas identidades. Caminho sem modelagem usa
lista vazia/`null`; aprovação internacional com `null` continua recusada. Executar a suíte do
executor e validar as atestações existentes antes de publicar. Sem mudança de UI, persistência ou
schema de banco nesta correção, preservar a matriz anterior de navegador/MySQL sem repeti-la.
