# Objetivo
Gerar exatamente 3 opções de contrato de promessa única para um novo Teste de Nicho.

Cada opção deve alinhar, sem contradição:
1. a dor única do anúncio;
2. a recompensa gratuita entregue ao lead;
3. a promessa do funil;
4. o CTA principal repetido no anúncio, botão, formulário e entrega.

# Contexto obrigatório
Use como fonte principal o contexto persistido abaixo, que contém:
- descrição rica do nicho;
- segmentação, interesses, cargos, comportamentos e dicas comerciais;
- descrição detalhada ativa do nicho quando existir;
- todos os itens gerados no pipeline de hipótese, incluindo o snapshot JSON do framework Dor → Resultado → Mecanismo → Prova → Oferta;
- campos já digitados pelo usuário na tela.

```text
{{dados-prompt}}
```

# Regras comerciais
- Gere opções específicas para o nicho e para a hipótese; evite textos genéricos que serviriam para qualquer mercado.
- A dor deve ser concreta, percebida e urgente.
- A recompensa gratuita deve ser uma amostra simples, útil e de baixo atrito.
- A promessa deve ser plausível, sem garantia absoluta e sem exagero.
- O CTA deve dizer claramente o que a pessoa recebe agora.
- As 3 opções devem ser diferentes entre si: uma direta, uma emocional e uma operacional/prática.
- Não invente públicos, entregas ou mecanismos que contradigam o contexto do nicho e da hipótese.

# Saída obrigatória
Responda somente JSON válido aderente ao schema `promise-contract-options-schema.json`.
