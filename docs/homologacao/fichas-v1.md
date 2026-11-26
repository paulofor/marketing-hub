# Piloto de fichas v1.0 — Capella

Piloto administrativo de consulta histórica, sem despacho. Não altera ficha financeira,
cadastro de agentes, permissões, custos, experimentos ou gates existentes. Habilitar somente
na instância de homologação com `marketinghub.fichas.homologacao=true`.
O padrão é desabilitado. Nenhuma configuração produtiva é alterada.

Tela: `/products/7/fichas?learningCycleId=20`. Em desenvolvimento, o histórico do produto
oferece o atalho preservando os parâmetros de ciclo. API:
`GET /api/products/7/fichas/v1?ciclo=20`. Produtos inexistentes recebem 404;
produto/ciclo sem acervo recebe lista vazia. A mesma leitura retorna as mesmas revisões.
O acervo é imutável no Git; não há comando de escrita ou despacho neste piloto.

Esquema e dois exemplos: `backend/ads-service/src/main/resources/fichas/v1/`.
Cada exemplo é uma lista de fichas. O primeiro reúne agente, recurso e revisão bloqueada;
o segundo registra a revisão recuperada e referencia as revisões anteriores por ID/revisão.
Adicionar revisão exige novo registro, preservando registros anteriores; alterar schema exige versão.
O ID de Dédalo foi confirmado pelo vínculo de `agent_task:927` ao cadastro 7.

## Evidência e limites

Consulta MCP de 26/11/2026: execução 97 do produto 7/ciclo 20/experimento 113 está COMPLETED,
3/3, processo 126 v13. Tarefas 898, 926 e 927 COMPLETED; 927 entregue em
23/11/2026 19:20:51 UTC. Definição 117 v11 RETIRED e 127 v12 PUBLISHED.
Execução 104 WAITING_INPUT, 4/10: exige URL executável aceita. O histórico integral de
request:4020 confirma recibo 9248 e passagem 9250. A revisão 1 conserva a falha original;
a revisão 2 registra a recuperação, sem concluir que o produto está pronto.
A tela global do produto mostra Processo 5; não é a mesma visão do ciclo 20.
Fontes históricas não autorizam um despacho atual: o comando oficial deve reconferir
versão publicada, entradas e controles. O piloto não substitui essa decisão do backend.

## Matriz de homologação definida antes dos testes

| Caso | Aceite |
| --- | --- |
| Dois resultados | Bloqueio histórico e passagem concluída, com fontes e lacunas |
| Revisão histórica | Vínculos exatos; revisão anterior continua navegável |
| Critérios | Confirmado/falho sem fonte é inválido no schema; pendente/sem evidência aceitos |
| Identidade | IDs estáveis, revisão positiva, origem e data obrigatórias |
| Integração | Controller e service reais com cadastro de produto simulado; nenhum write |
| Idempotência | Repetição devolve resposta igual sem criar registros |
| Isolamento | Outro produto/ciclo não recebe fichas de Capella; inexistente retorna 404 |
| Interface | Desktop e Chromium mobile: revisão, fontes e lacunas legíveis |
| Falha | API indisponível apresenta mensagem, sem inferir conclusão |
| Observabilidade | Ambiente histórico explícito, fontes datadas; erro de leitura registra contexto |
| Métricas | Nenhuma inferência ou venda simulada entra no histórico comercial |

Testes: `FichasTest`, `ProductFichasPage.test.tsx` e
`scripts/tests/test-fichas-schema.py`. O último usa `jsonschema==4.23.0`.

## Alternativas e melhoria do harness

1. Ampliar ficha financeira: reaproveita a tela, mas mistura prova e autorização de gasto.
2. Criar cadastro operacional paralelo: maior flexibilidade, alto custo e risco de divergência.
3. Acervo versionado de leitura: menor custo, histórico explícito; escolhido para o piloto.

Lacuna observada: resumo antigo indicava apenas passagem bloqueada; fonte integral e banco
comprovam recuperação posterior. A prevenção reutilizável é exigir ID/revisão, origem e
fonte nos estados conclusivos, com regressões negativas do schema. Métrica técnica:
rejeitar critérios conclusivos sem fonte e preservar vínculos históricos. Impacto sobre
vendas desconhecido. O piloto não amplia a capacidade executora nem atribui aprendizado
comercial aos agentes.

O campo `tentativa` admite `null` quando a fonte histórica não comprova o número. A tela
mostra “desconhecida” e conserva a lacuna; não usa o contador atual como se fosse histórico.
Teste integrado de aceite em TAP: `node --test --test-reporter=tap scripts/tests/test-fichas-acceptance.cjs`.
Pré-requisitos: dependências Maven/Node do projeto, Chromium/Playwright e jsonschema 4.23.0
(disponível no Python ou em `.local/fichas-python`). O navegador usa API simulada explicitamente;
o contrato HTTP é testado separadamente com controller/service reais e cadastro simulado.
Não se trata de execução produtiva nem prova de software entregue por Dédalo.

Na revisão local foi reforçado o isolamento do acervo: ao filtrar ciclo, agente/recurso
só aparece quando é alcançado por vínculo de revisão de uma execução daquele ciclo.
O teste inclui recurso sem vínculo no mesmo produto para prevenir associação indevida.

## Resultado local de 26/11/2026

Aceite TAP aprovado: `C1 fichas v1: contrato, API isolada, revisões e navegação homologadas sem agentes`.
Passaram quatro testes de schema, três do backend, dois de interface, sete regressões do
histórico, typecheck e navegação Chromium desktop/iPhone 15 Pro. O build produtivo do
frontend passou isoladamente com `NODE_OPTIONS=--max-old-space-size=2048`.
Uma tentativa concorrente anterior foi encerrada por OOM no limite de 3,5 GiB da sandbox;
a repetição isolada passou sem mudança no código da aplicação. Não houve inferência paga,
escrita no banco operacional ou criação de tarefa de agente.
