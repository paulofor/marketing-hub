# Matriz de homologação — execução semanal do Plano Comercial v1

## Objetivo

Comprovar localmente que o Planejamento Semanal realiza temporalmente a versão oficial do Plano Comercial, sem autorizar gastos ou misturar produtos.

| Área | Cenário | Resultado esperado |
|---|---|---|
| Caminho feliz | Criar compromisso na janela semanal | Persiste ação, versão atual, responsável, resultado, prazo, custo e receita planejados. |
| Evolução | Alterar estado de planejado até concluído | Atualiza somente estado/nota e preserva a versão estratégica congelada. |
| Validação | Enviar versão ausente ou antiga | Backend rejeita e solicita recarga do plano. |
| Validação | Enviar custo ou receita negativa | Backend rejeita sem persistir o compromisso. |
| Falha | Usar compromisso de outro plano | Backend responde não encontrado e preserva segregação. |
| Integrações | Selecionar agente cadastrado | Tela envia `agentKey` e apelido, sem ampliar autoridade do agente. |
| Observabilidade | Abrir semana do plano | Exibe planejado versus custo, receita e funil realizados pelas fontes oficiais. |
| Métricas | Concluir entrega sem venda | Estado operacional muda, mas receita não é inferida como venda. |
| Gates | Planejar gasto/publicação | O compromisso não libera gasto, campanha, preço ou publicação sem gate próprio. |
| Segregação | Alternar entre planos | Semanas e compromissos permanecem vinculados ao `planId` selecionado. |
| Desktop | Chromium em largura desktop | Formulário e cards legíveis, sem sobreposição. |
| Mobile | iPhone 15 Pro e Pixel 7 | Campos, status e metas financeiras utilizáveis sem overflow horizontal da página. |

## Critério de aprovação

Uma rodada integral sem defeito conclui a homologação. Se houver correção após defeito funcional, executar duas rodadas integrais consecutivas sem falhas, reiniciando a contagem se surgir novo defeito.
