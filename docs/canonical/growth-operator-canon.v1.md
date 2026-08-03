# Operador de Crescimento Canonico v1

## Objetivo

O Operador de Crescimento transforma meta, gargalo e evidencias persistidas do planejamento comercial em diagnostico auditavel. A v1 existe exclusivamente em modo `READ_ONLY_DIAGNOSIS`.

## Autoridade

- O backend cria a pendencia, congela o contexto e persiste o resultado.
- O worker consome somente o endpoint `pending/claim` e nunca acessa o banco.
- O Codex roda com sandbox `read-only`, sessao efemera e repositorio montado sem escrita.
- A v1 nao altera plano, codigo, campanha, preco, orcamento, publicacao, comunicacao ou dados comerciais.
- Toda recomendacao que exija mutacao deve retornar `WAIT_FOR_APPROVAL`.
- O backend nunca aplica automaticamente a proxima acao recomendada.

## Contrato do diagnostico

Cada execucao deve persistir objetivo, gargalo, snapshot de evidencias, exatamente tres alternativas, causa-raiz, metrica esperada, criterios de continuar/ajustar/parar, decisao, proxima acao, resposta bruta, modelo, custo e falha quando houver.

Decisoes permitidas: `CONTINUE`, `ADJUST`, `STOP` e `WAIT_FOR_APPROVAL`.

## Gates de ampliacao

A autonomia somente pode ser ampliada por nova versao e decisao explicita do usuario depois de pelo menos dez diagnosticos confirmados por eventos posteriores, sem violacao de autoridade. Preco, gasto, campanha, publicacao, comunicacao em massa e PR permanecem sujeitos a aprovacao humana.
