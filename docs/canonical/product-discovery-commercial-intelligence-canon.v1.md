# Descoberta PDE — inteligência comercial v1

## Status

CANÔNICO.

## Objetivo

O Descoberta PDE deve encontrar possibilidades comerciais auditáveis. Ele não pode transformar tema digitado, resultado de busca vazio ou texto genérico em produto supostamente promissor.

## Contrato obrigatório

Cada ciclo deve produzir no mínimo três hipóteses concorrentes e avaliar separadamente:

- demanda e recorrência da dor;
- concorrentes e ofertas observáveis;
- preço e sinais de disposição de pagar;
- lacuna das soluções atuais;
- viabilidade e margem da entrega;
- risco comercial e regulatório;
- feedback real de checkout, compra, reembolso e entrega, quando existir.

Página de busca, fallback do provedor e ausência de resultados nunca são evidência. Sem evidência comercial suficiente, a decisão obrigatória é pesquisar mais.

## Estágios de decisão

- `IDEIA`: hipótese ainda sem pesquisa suficiente.
- `PESQUISAR`: evidência incompleta ou contraditória.
- `VALIDAR_ORGANICAMENTE`: dor e oferta plausíveis, mas intenção de compra ainda fraca.
- `TESTAR_PAGO`: demanda, concorrência, preço e entrega plausíveis com gate financeiro definido.
- `REJEITAR`: risco, falta de controle sobre o resultado ou ausência persistente de demanda.

Os contratos legados podem representar esses estágios com as decisões técnicas existentes, mas a tela deve explicar o estágio comercial e nunca chamar score heurístico de venda validada.

## Fechamento do aprendizado

O ranking deve ser recalculado com dados persistidos dos ciclos e, quando disponíveis, dos experimentos. Checkout, compra aprovada, reembolso, custo de entrega e prazo real têm precedência sobre sinais textuais de busca.
