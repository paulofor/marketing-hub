# Matriz de homologação — Plano Comercial Anti-Invisibilidade v1

## Objetivo

Comprovar localmente que o processo publicado `pde-commercial-plan-offer` consegue transformar o
PDE Anti-Invisibilidade Profissional em uma tese comercial única, mensurável e financeiramente
limitada, sem publicar oferta, alterar preço, contatar pessoas ou consumir mídia.

## Fonte e segregação

- produto: `product:6`;
- processo: `pde-commercial-plan-offer` v4;
- referência executada: `commercial-plan:5@v2`;
- execução: agentes Codex em sandbox somente leitura;
- dados de homologação: referências com prefixo `LOCAL_QA`, nunca contabilizadas como venda;
- efeitos externos proibidos: contato, checkout real, campanha, publicação, mudança de preço e gasto.

## Matriz ponta a ponta

| Área | Cenário | Evidência esperada | Critério |
| --- | --- | --- | --- |
| Caminho feliz | Dédalo, Têmis, Hermes, Plutus e Psique concluem suas atividades na ordem do BPM | tarefas com resultado, evidência, tokens e custo | todas as atividades aprovadas e contrato consolidável |
| Validação | plano sem público, dor, transformação ou preço | decisão `ADJUST` ou `REJECT` com campos ausentes | nenhuma aprovação fabricada |
| Formato | comparar kit, webapp e experiência guiada | exatamente três alternativas com valor, adoção, custo, risco e escala | escolha explícita e justificável |
| Prova | promessa sem demonstração ou fonte | matriz promessa → pergunta → prova → fonte → limitação | toda promessa relevante possui prova honesta |
| Distribuição | dependência exclusiva de mídia paga | três rotas comparadas e ativo acumulável escolhido | canal inicial atribuível e sem gasto automático |
| Personalização | uso de sinais ou dados pessoais | explicação, categorias, recusa e fallback neutro | nenhuma inferência psicológica ou preço oculto |
| Economia | preço, custo ou CAC ausente | cenário conservador, base e limite; lacuna explícita | contribuição positiva ou bloqueio financeiro |
| Falha de agente | resposta fora do schema ou execução interrompida | tarefa bloqueada com erro, evidência e consumo preservados | sucessora não é liberada |
| Multiagente | um de dois responsáveis da mesma atividade ainda não concluiu | sucessora permanece aguardando | todos os responsáveis da predecessora concluíram |
| Observabilidade | execução de modelo | request, resposta, modelo, tokens, cache, saída e custo estimado | nenhum custo ausente convertido em zero |
| Métricas | contrato final | compra, primeiro valor, conclusão, reembolso e margem | amostra, clique ou parecer nunca contam como venda |
| Navegadores | relatório do plano em desktop, iPhone 15 Pro e Pixel 7 | leitura sem overflow e estados compreensíveis | jornada administrativa utilizável nos três perfis |

## Decisão

- **Continuar:** plano aprovado, preço tratado como hipótese, contribuição positiva e prova antes da
  compra conectada ao valor pago.
- **Ajustar:** interesse plausível, mas formato, prova, esforço, preço ou canal ainda têm lacuna
  solucionável.
- **Parar:** promessa não comprovável, risco de dano ou discriminação, margem não positiva, ausência
  de mecanismo útil ou dependência de alegação de promoção garantida.

## Resultado de 2026-08-23

- plano #5 concluído e consolidado como versão 3;
- produto #6 avançado de `IDEIA_PRIORIZADA_PARA_TESTE` para `CONSTRUCAO_E_APROVACAO`;
- 21 tarefas concluídas e quatro tentativas bloqueadas preservadas para auditoria;
- decisões finais de Dédalo, Hermes, Plutus, Psique e Têmis: `APPROVE`;
- preço preservado em R$ 47 e orçamento externo em R$ 0;
- nenhum contato, publicação, transação, gasto ou venda gerado;
- execução #220 interrompida sem resposta do modelo e sem telemetria; a sucessora #221 foi aprovada;
- uso informado pelas demais tarefas: 7.390.772 tokens de entrada, 5.118.720 em cache, 206.227
  de saída e US$ 15,26023600 pelo fallback Standard do `gpt-5.6-sol`.
- duas rodadas locais finais e consecutivas aprovadas, cada uma com 1.749 testes do backend, 378
  do frontend, 140 dos cinco workers, seis do executor, cinco schemas JSON, Spotless, TypeScript,
  build e jornada real no desktop, iPhone 15 Pro e Pixel 7;
- catálogo e formulário do produto permaneceram sem overflow nos três perfis, inclusive com URLs
  públicas longas, e os limites do contrato do backend ficaram refletidos nos campos do frontend.
