# Sequência comercial do Vega — v1

> [!IMPORTANT]
> Esta sequência obedece à
> [regra global de validação por agentes ou mercado](system-governance-canon.v3.md#31-regra-global-de-validação-por-agentes-ou-mercado).
> A antiga observação solicitada com cinco pessoas foi retirada e não pode voltar como gate.

Decisão do usuário em 08/09/2026. Produto `4` (Vega/MUSA), plano comercial `3`.
Referência: [análise conciliada do #91](../marketing/vega-cadeia-valor-estrategia-2026-09-08.md).

O objetivo é gerar vendas líquidas com entrega útil e contribuição positiva. O Vega permanece
em Venda, entrega e aprendizado; a melhoria da experiência é um retorno dirigido, sem reiniciar
descoberta nem considerar a cadeia concluída. O roteiro pertence ao plano do Vega. Não altera
a definição compartilhada de operação usada por Rigel, nem transfere conclusões entre produtos.

## Sequência e critérios

| Ordem | Responsável e entrega | Orientação ao operador e condição de avanço |
| --- | --- | --- |
| 1 | Hermes: reconciliar o ciclo pago #91 | Selecionar #91 no plano #3. Comparar monitor, eventos atribuídos, campanha e pagamentos com data da consulta; preservar pausa. Corrigir a leitura que omite e-mail preenchido e sobrepõe relógios. #90 é piloto direto separado. Não repetir bloqueio antigo do vídeo #38. Só concluir com divergências resolvidas ou explicitamente delimitadas. |
| 2 | Dédalo: primeiro ajuste aplicável | Solicitar microação que explique o que fazer, como aplicar, ocasião e critério da própria cliente; salvar e consultar novamente. Preferir catálogo curado, incluindo caminho neutro. Implementar e testar localmente; usar imagem exclusiva do Vega. Parecer não substitui implementação. |
| 3 | Íris: entrada mobile e continuidade da oferta | Mostrar a primeira escolha sem rolagem nos dispositivos homologados; demonstrar o ajuste real. Explicar primeiro ajuste/Dia 1 gratuitos e o benefício adicional dos Dias 2 a 7. Preservar R$ 67, pagamento único, sete dias de jornada e 90 dias de acesso. Apolo só entra se uma necessidade audiovisual for comprovada. |
| 4 | Homologação técnica; depois Psique; depois Têmis | Validar resultado → e-mail de teste → retomada → checkout de teste → acesso → missão, falhas e recuperação. Exigir evidência da mesma versão/produto. Psique avalia utilidade e esforço; Têmis verifica fidelidade, limites e integridade comercial. Rejeição volta ao responsável pela causa, seguida de nova homologação técnica e das revisões afetadas. |
| 5 | Hermes + backend: preparar a leitura do mercado | Confirmar a instrumentação da mesma versão, excluir `QA_INTERNAL`/`AGENT_VALIDATION` e predeclarar funil, atribuição, CAC, contribuição e regras de parada. Nenhuma entrevista, recrutamento, observação solicitada ou opinião entra como requisito. Essa preparação não comprova demanda nem venda. |
| 6 | Atena → Plutus → autorização humana → Hermes: próximo teste pago | Após 1–5, definir uma hipótese, versão, canal, janela, orçamento e regras de parada; validar custos, margem e atribuição. Criar sucessor do #91 pela UI somente quando autorizado. Não reativar #91 automaticamente nem interpretar saldo anterior como autorização. Após estabilizar a versão, mudar uma variável por teste. |
| 7 | Entrega + Plutus + Hermes: primeiras vendas e decisão | Buscar cinco vendas líquidas atribuídas ao canal avaliado, conciliadas e entregues, acompanhando primeiro uso, recompra, suporte iniciado pela cliente e reembolso. Registrar contribuição após custos e reembolsos. Cinco vendas são marco inicial, não prova de escala; ampliar somente com repetição por coorte, economia sustentável e nova autorização. |

## Bloqueios e interpretação

- O #91 é referência de aprendizado, não campanha nova. Na fotografia de 08/09: interrompido,
  quatro sessões mobile e nenhuma compra atribuída. A pequena amostra não prova rejeição de preço,
  público ou produto. Toda nova leitura deve preservar horário e fonte; os valores históricos
  não devem ser apresentados como consulta atual da Meta.
- A seleção explícita de experimento no plano ativo prevalece sobre um piloto `RUNNING` antigo
  **quando o selecionado já foi operado**. Um sucessor apenas `PLANNED` não rouba o histórico da
  operação. Planos encerrados/cancelados e experimentos de outro produto não governam a seleção.
- A sincronização automática de Hermes acrescenta experimentos compatíveis ao portfólio e
  atualiza métricas, mas não pode substituir uma seleção existente, nem quando ela estiver
  pausada ou planejada. Somente plano sem seleção recebe uma referência inicial automática.
  O comando explícito de selecionar um experimento em execução é separado e versionado. Os
  snapshots novos preservam experimento selecionado, portfólio, causa e meta operacional; versões
  históricas incompletas não são reescritas.
- O bloqueio e a próxima ação persistidos em um plano `BLOCKED` devem aparecer no fluxo
  operacional antes de recomendações genéricas. Isso não aprova homologação, publicação ou gasto.
- Falha de dados volta à reconciliação; falha da microação volta a Dédalo; fricção ou mensagem volta
  a Íris; falha de acesso/pagamento/entrega volta à correção técnica; economia inviável volta a
  Atena e Plutus. Repetir análise sem evidência nova não é avanço.
- Meta mensal anterior de R$ 1.340 não é promessa nem receita. O marco inicial é cinco vendas de
  R$ 67 (R$ 335 brutos antes de custos), com análise líquida por canal. Orçamento de plano e mídia
  permanecem limites distintos e não somáveis; este roteiro não acrescenta verba.
- O roteiro orienta execução dos especialistas e decisões de governança do proprietário. Registrá-lo no plano não cria tarefas,
  não implementa as melhorias do produto e não transforma suas condições em gates automáticos
  novos no BPM compartilhado.

## Alternativas consideradas

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Refazer a cadeia completa | Rever tudo desde descoberta | Alto esforço, perda de foco e repetição sem evidência de necessidade | Descartada |
| Trocar o processo compartilhado | Padronizar a recuperação para todos | Afeta Rigel e execuções não incluídas no pedido; requer decisão de escopo | Não aplicada |
| Roteiro exclusivo no plano atual + correção da referência operacional | Foco na primeira entrega, aprendizado e receita; preserva histórico | Exige concluir cada entrega; roteiro não substitui evidência de execução | Escolhida para Vega |

## Evolução para ciclos executáveis — 08/09/2026

O roteiro passa a ser conduzido pelo módulo transversal de
`ciclos-aprendizado-vendas-canon.v1.md`, quando o operador adotar o experimento na nova tela.
O #91 entra como referência histórica na medição; uma decisão de ajuste permite vincular um
sucessor planejado, com memória, versão, hipótese e métricas próprias. A adoção não modifica o
plano, não substitui o #91 pelo #90 e não reativa campanha. As tarefas novas recebem o contexto
do ciclo exato no contrato de entrada dos especialistas. As revisões e autorizações continuam
com suas autoridades canônicas.

A campanha histórica do #91 possui recibo Meta persistido, mas não tem run/preflight registrado.
Essa lacuna deve acompanhar a adoção e a memória do sucessor; não exige reativar #91 nem fabricar
aprovação retroativa. O backend deve aceitar a referência pelo contrato de publicação histórica
do cânone dos ciclos. A nova publicação continua exigindo sua própria homologação e autorização.
