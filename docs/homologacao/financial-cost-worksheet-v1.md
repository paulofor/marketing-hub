# Planilha financeira com seis grupos — 02/11/2026

Matriz definida antes da implementação e dos testes. Caso observado: Capella, produto 7,
cadeia 27, ciclo 19, experimento 112, versão capella-beleza-v3. Tela e MCP confirmaram
US$ 67,37689 de estimativas de agentes; nenhum desses valores é consumo audiovisual ou
de mídia. O ciclo está aberto em PLANNING, revisão 2; limite US$ 80, reserva US$ 20.
Não alterar esses valores nem iniciar tarefas pagas durante esta entrega.

Alternativas comparadas: (1) acrescentar rótulos aos cartões, simples mas sem edição ou
previsões; (2) criar outro sistema financeiro, maior custo e risco de duplicar aprovações;
(3) planilha contextual com eventos imutáveis e autorizações existentes, esforço moderado,
preserva fontes e resolve o pedido. Adotada a terceira.

| Aceite | Validação local prevista |
| --- | --- |
| Seis grupos e edição | Salvar orçamento, custo com fonte e previsão; recarregar e recuperar os mesmos dados |
| Cálculos | Subtotais por moeda, lacunas, zero explícito, resultado somente com cobertura e câmbio |
| Aprovações | Preservar recibos operacionais; aprovação de planejamento não libera gasto; edição invalida aprovação |
| Falhas | Negativo, precisão, fonte ausente, período inválido, contexto trocado, ciclo encerrado e revisão obsoleta |
| Recuperação | Mesmo request recupera recibo após perda de resposta; outra carga com mesma chave é recusada |
| Integração | Controller e serviços reais, persistência local, nenhuma API paga; custos automáticos separados dos manuais |
| Regressão | Caso Capella sintético, outro produto e caminho anterior de aprovação de preparação |
| Observabilidade | Autor, horário, versão, fontes e histórico preservados; zero mutações produtivas |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; tabela acessível e formulários na mesma página |
| Métricas | Contagem de fontes/lacunas e cobertura; nenhuma venda ou melhora comercial fabricada |

Lacuna concreta do harness: os contratos e fixtures só distinguiam três limites, favorecendo
a interpretação de custo parcial como custo total. A classificação executável compartilhada
e as regressões passam a exigir os seis grupos, base e cobertura explícitas. Hipótese de
benefício: decisões de orçamento com menos omissões e dupla contagem; impacto em vendas e
lucro requer medição comercial posterior. Adoção condicionada à matriz; reversão pelo PR
preserva eventos e recibos anteriores.

## Resultados

- 852 testes locais de ciclos, projeção das atividades, execução e custos passaram sem falhas.
- 92 regras de arquitetura, 20 testes específicos da planilha, regressão JPA do bloqueio e
  testes de projeção/wiring passaram. A leitura e gravação MySQL foram reconferidas após os ajustes.
- 21 testes da interface, typecheck e build de produção passaram. O fluxo anterior de
  aprovação foi preservado em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados.
- API real com MySQL 5.7: quatro pedidos concorrentes produziram um único evento; replay,
  conflito de revisão, contexto de outro produto e cálculo de R$ 880 sintéticos conferidos.
- Fluxo navegador → API → MySQL em três perfis: editar seis grupos, salvar, perder a resposta
  depois do commit, retentar, recarregar e aprovar os três orçamentos administrativos. Um evento
  na recuperação e outro no aceite; nenhum limite operacional ou estágio alterado.
- Os testes identificaram erros nas fixtures (decodificação de texto, identidade do segundo
  experimento e evidência obrigatória), corrigidos localmente. Uma execução de wiring coincidiu
  com recompilação e foi repetida sequencialmente; os cinco testes de wiring passaram.
- Revisão funcional: teto de mídia cadastrado não prova aceite; a planilha exige recibo da
  versão e janela vigentes. Orçamento amplo de produção não é comparável ao teto específico
  dos vídeos; essa diferença não é calculada. Testes protegem ambos os limites.
- Lacuna causal adicional: a projeção do processo lia o último evento, incluindo ajustes
  financeiros. Agora busca a última transição funcional, preservando MEASUREMENT_BLOCKED
  após planilha ou ajustes de orçamento. Teste JPA real e regressão da projeção cobrem o caso.
- Testes são sintéticos. Custo informado permanece declaração com fonte, não conciliação
  automática; custos reais dos demais grupos e demanda continuam dependentes de fontes.
  A emulação móvel não comprova Safari nativo ou aparelhos físicos.

A compatibilidade de Mira preserva a experiência, as provas e pareceres anteriores. Somente
os contratos administrativos de planilha e a seleção do evento funcional foram ampliados;
não há nova homologação comercial, inferência, gasto ou publicação do produto.
