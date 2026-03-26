# Manual do Usuário – Framework Dor → Resultado → Mecanismo → Prova → Oferta

Este manual descreve como trabalhar com a nova experiência de hipóteses do Marketing Hub, cobrindo criação, revisão, uso da IA e checklist de aprovação.

## 1. Onde acessar
- **Novo**: acesse *Nicho → Nova hipótese*. O formulário já abre com o quadro do framework.
- **Editar/Revisar**: acesse *Hipóteses → Detalhes* para abrir o painel com abas e (se necessário) clique em *Editar* para alterar campos manuais ou o checklist.

## 2. Estrutura com abas
Cada hipótese possui um único quadro com cinco abas:
1. **Dor** – descreve dores de superfície, raiz, emocionais, sociais e o custo de permanecer no estado atual.
2. **Resultado** – qual transformação o lead espera, qual identidade busca, qual impacto de negócio e qual sinal prova o progresso.
3. **Mecanismo** – como a oferta entrega o resultado: mecanismo central, diferencial único, evidência visível e porque é crível.
4. **Prova** – tipo de prova recomendado, ativo/asset, mensagem principal e etapa do funil onde ela entra.
5. **Oferta** – nome da oferta, promessa, entregáveis, mitigação de risco, narrativa de preço, CTA e (quando houver) preço/estrutura.

> As informações básicas da hipótese (Título, Persona, Problema, Promessa, etc.) ficam acima das abas e continuam sendo exigidas para manter compatibilidade com os experimentos e relatórios.

## 3. Geração com IA
- Cada aba possui um botão **Gerar com IA** e um campo opcional de instruções extras.
- Ao clicar, o Marketing Hub envia o contexto (nicho + hipótese) para o Worker/IA e atualiza apenas a seção selecionada, preservando as demais.
- O histórico de custo e tokens fica registrado automaticamente em *Configurações → IA worker logs*.

### Boas práticas
- Use instruções curtas (“Explore ganhos emocionais”, “Traga prova tipo diagnóstico”) para orientar o modelo.
- Gere primeiro *Dor → Resultado* antes de *Mecanismo/Prova/Oferta* para manter coerência.

## 4. Checklist de aprovação
- O checklist aparece no final das abas em **Editar** e como painel informativo na visualização.
- Itens:
  - Dor validada
  - Resultado claro
  - Mecanismo explicável
  - Prova definida
  - Oferta empacotada
  - Aprovado para experimento
  - Notas de revisão
- Marque todos antes de mover a hipótese para *Testing* ou vincular a um experimento. Isso garante rastreabilidade do que já foi revisado humanamente.

## 5. Fluxo recomendado
1. **Criar** hipótese com o formulário completo → preencher dados básicos → detalhar cada aba → salvar.
2. **Iterar com IA** no detalhe da hipótese: gere novas sugestões aba por aba e valide se fazem sentido.
3. **Checklist**: abra a hipótese em modo *Editar*, marque os itens concluídos e registre observações.
4. **Liberar para experimento**: somente após checklist completo e dor/resultado aprovados.

## 6. Perguntas frequentes
- **Posso usar IA antes de salvar?** No modo criação os botões ficam desabilitados porque a hipótese ainda não tem ID.
- **O que muda nos experimentos?** Nada. Eles continuam lendo os campos principais (problema, promessa etc.), que agora são mantidos em sincronia com o framework automaticamente.
- **Como ver o histórico de gerações?** Em *Configurações → IA Worker → Gerações* filtre por domínio `hypothesis.framework.*`.

Em caso de dúvidas ou sugestões, registre no canal #marketing-hub-product.
