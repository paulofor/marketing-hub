# AGENTS.md — MOIS Página de Vendas (Sales Library Worker)

## Regra obrigatória de logs em integrações OpenAI (semelhante ao Gera Landing)
- Sempre que o worker MOIS de Página de Vendas executar uma requisição para a OpenAI, registrar log com:
  - envio para a OpenAI contendo **request cru** + **jobId do Marketing Hub**;
  - resposta da OpenAI contendo **resposta crua** + **jobId do Marketing Hub**;
  - envio para o backend contendo **payload enviado** + **jobId do Marketing Hub**.
