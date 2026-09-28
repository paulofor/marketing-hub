# ADR — validação exclusivamente por agentes ou mercado

> STATUS: ACEITO
> DATA: 2026-09-28
> FONTE CANÔNICA: `docs/canonical/system-governance-canon.v3.md`

## Contexto

Planos de produto voltaram a recomendar entrevistas, recrutamento, leituras privadas e testes
solicitados com poucas pessoas. O Marketing Hub não possui essa capacidade operacional. A exigência
paralisa produtos, cria uma etapa manual não escalável e ainda não comprova venda nem lucro.

## Alternativas

| Alternativa | Benefício | Risco e custo | Aderência a vendas e lucro |
| --- | --- | --- | --- |
| Recrutar pessoas para cada produto | Relato qualitativo direto | Operação indisponível, lenta e não escalável | Baixa; opinião não comprova compra |
| Declarar a validação somente por agentes | Automação e baixo custo | Pode confundir simulação com demanda real | Parcial; comprova prontidão, não receita |
| Agentes antes do mercado + experimento comercial depois | Automação, velocidade e fatos econômicos | Exige instrumentação e disciplina de orçamento | Alta; separa prontidão, venda e lucro |

## Decisão

Adotar a terceira alternativa para todos os projetos, produtos e evoluções:

1. agentes, fontes públicas e testes determinísticos pesquisam e homologam o produto antes do
   mercado;
2. nenhum fluxo cria ou espera entrevista, participante, teste solicitado, leitura privada ou
   opinião;
3. o mercado é observado somente por comportamento voluntário em canal autorizado;
4. pagamento reconciliado comprova venda e contribuição positiva comprova condição de escala;
5. tráfego interno e evidência sintética permanecem segregados das métricas comerciais.

## Consequências

- Planos e próximos passos tornam-se executáveis sem recrutamento externo.
- Agentes não podem alegar cliente, preferência, satisfação, demanda ou venda.
- Cliques, uso e checkout continuam sinais intermediários; não substituem pagamento.
- Aprovações do proprietário para preço, orçamento, publicação e gasto permanecem obrigatórias e não
  são tratadas como pesquisa com pessoas.
- Contratos históricos continuam auditáveis, mas não podem orientar novas execuções.
- Qualquer implementação ativa que ainda imponha validação privada deve ser tratada como drift e
  corrigida antes de orientar ou bloquear um produto.
