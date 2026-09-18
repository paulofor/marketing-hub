# Homologação do canal de validação da candidata PDE

Data: 18/09/2026

Escopo: corrigir o smoke da superfície candidata sem publicar antecipadamente o contrato comercial.

Matriz local:

- caminho feliz publicado: v5, v6 e v7 consultam o contrato público;
- caminho feliz candidato: v8 consulta o preflight interno autenticado e compara contrato, proxy, runtime e fingerprint;
- validações: modo desconhecido, token ausente, identidade divergente e endpoint indisponível bloqueiam;
- recuperação: todos os checkouts do workflow validam a autorização antes de continuar;
- integração: backend PDE, frontend, Psique, evidências, Actions, Docker, rollback e isolamento entre versões;
- observabilidade: o log identifica o canal sem imprimir segredo;
- métricas e dados: nenhum evento comercial, gasto, cobrança, campanha ou chamada de IA é produzido;
- navegadores: as jornadas do conjunto integrado preservam os perfis desktop e mobile já cobertos pelo PDE.

Resultado: duas rodadas integradas consecutivas aprovadas após a última correção, cada uma com
28/28 controles. Foram aprovados testes unitários de Psique, Dédalo, Apolo e backend PDE; catálogo
do harness; empacotamento de evidências; builds; navegador; consistência HTTP; Actions; ShellCheck;
promoção isolada, falha pós-troca, rollback e continuidade das demais versões em Docker.

Evidências locais:

- `artifacts/actions-psique-pde-2026-09-08/final-round-1/results.tsv`;
- `artifacts/actions-psique-pde-2026-09-08/final-round-2/results.tsv`.

Limite: a correção permanece somente na worktree e ainda não foi confirmada no workflow produtivo.
