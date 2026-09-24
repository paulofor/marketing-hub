# Homologação — schemas estritos de Argos por atividade

Data: 2026-09-23

## Escopo e identidade

- processo independente: Descoberta factual da oportunidade PDE v7, definição #90;
- execuções que evidenciaram a falha: #27/ciclo #66/tarefa #477 e #28/ciclo #67/tarefa #478;
- atividade original: `research` — Reunir evidências factuais e situações de compra;
- executor: Product Discovery Worker / Argos.
- versão do executor corrigido: Argos v5.

O provedor recusou o schema antes da inferência porque `candidateGaps` e `researchLimits` estavam
declarados, mas não constavam em `required`. Nenhuma das duas tarefas registrou tokens ou custo.

## Matriz de validação

| Área | Caso | Critério de aceite |
|---|---|---|
| Caminho feliz | Planejamento inicial | Usa schema sem campos de candidatas e produz plano válido. |
| Caminho feliz | Aprofundamento | Usa schema próprio, exige lacunas/limites e preserva 2–3 candidatas. |
| Integração | Síntese factual | Schema continua compatível com o mesmo validador estrito. |
| Validação | Campo declarado fora de `required` | Falha localmente antes de iniciar `codex exec`. |
| Validação | Palavra-chave incompatível | Falha localmente com caminho e palavra-chave identificados. |
| Observabilidade | Erro JSON no `stdout` | Callback registra `invalid_json_schema`, não apenas código 1. |
| Reinício | Worker com contrato inválido | Startup falha antes do polling e não adquire tarefa. |
| Retomada | Nova execução pela tela após deploy | Pesquisa deixa de falhar por schema; histórico bloqueado é preservado. |
| Custo | Rejeição anterior à inferência | Tokens/custo permanecem ausentes, nunca convertidos para zero. |
| Isolamento | Arquivos temporários e contexto | Diretório é removido; nenhuma credencial entra no prompt ou erro. |
| Processo | Resultado com candidatas | Só abre aprofundamento após 5–8 entrevistas consentidas válidas. |
| Processo | Resultado sem candidata factual | Encerra honestamente sem fabricar candidata, entrevista ou handoff. |

Não há alteração de interface. A validação visual de retomada deve usar a tela publicada em desktop
e celular; testes do worker e do contrato precedem PR e deploy.

## Decisão

1. Campos sempre obrigatórios: simples, mas inventa conceitos do aprofundamento na pesquisa inicial.
2. Schema permissivo: menor esforço, mas perde o contrato estrito e posterga falhas para o parser.
3. Schema por atividade e validação no startup: mantém significado, bloqueia publicação inválida e
   melhora o diagnóstico com esforço contido.

A opção 3 foi escolhida. Ela corrige a causa-raiz sem alterar tema, produto, estratégia, orçamento
ou gates de evidência.
