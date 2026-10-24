# Controle operacional PLAY/STOP por produto — cânone v1

## Objetivo

Permitir que a operação pause ou retome novas execuções automáticas de um produto específico sem
alterar status comercial, experimentos, qualidade, preço, publicação ou histórico do produto.

## Contrato

- `PLAY`: autoriza executores orientados ao produto a iniciar novos trabalhos automáticos.
- `STOP`: impede o início de novos trabalhos automáticos do produto.
- Uma execução já iniciada termina normalmente para preservar artefatos, custos e auditoria.
- Ações manuais explícitas continuam sujeitas aos próprios gates e não são inferidas como automação.
- Produtos existentes começam em `PLAY` para preservar compatibilidade operacional.
- Toda mudança registra produto, decisão, operador e data em trilha append-only.
- A tela sempre mostra a verdade persistida pelo backend e não altera o estado por inferência local.

## Leituras operacionais

- A visão operacional de produtos deve consultar o backend com `playOnly=true` e receber somente
  produtos em `PLAY`; o frontend não pode filtrar ou deduzir esse estado localmente.
- O catálogo administrativo geral permanece disponível sem esse parâmetro para auditoria e para a
  gestão de produtos em `STOP`.
- A pesquisa por nome, nome interno, apelido ou slug com `playOnly=true` continua restrita a
  produtos em `PLAY`.

O controle é independente do status comercial. Em especial, `STOP` não pausa campanha, não cancela
experimento, não retira página do ar e não transforma produto em descontinuado. Essas ações exigem os
contratos e autorizações próprios.

## Matriz de homologação

| Cenário | Evidência esperada |
| --- | --- |
| Produto em PLAY recebe STOP | backend persiste STOP, registra evento e a tela troca somente o produto selecionado |
| Produto em STOP recebe PLAY | backend persiste PLAY, registra evento e a tela troca somente o produto selecionado |
| Comando repetido | resposta idempotente, sem novo evento ou alteração comercial |
| Produto inexistente | resposta 404 e nenhum registro criado |
| Requisição sem decisão | resposta 400 e nenhum registro criado |
| Requisição em andamento | botão do produto fica desabilitado e mostra carregamento |
| Desktop e mobile | estado, rótulo e ação permanecem legíveis e acionáveis sem overflow |

Uma rodada local dos testes relevantes conclui a homologação quando os critérios são atendidos.
Se houver defeito, corrigir a causa e repetir somente as verificações afetadas e regressões
relacionadas; não exigir uma quantidade mínima de rodadas sem justificativa técnica.

## Visão Produtos em play — 24/10/2026

O menu **Produtos em play** (`/products/in-play`) apresenta produto, atividade corrente,
responsável e status por `GET /api/products/operations/v1/in-play`. PLAY é permissão de
automação, não prova de tarefa executando. O backend consolida a atividade canônica e a
execução persistida, seguindo delegações do mesmo produto/cadeia/ciclo/referência.

A cadeia histórica do ciclo permanece preservada. A fila operacional precede processos
pausados; um subprocesso aguardando entrada não é execução ativa do agente. Reprovação
de atividade anterior não substitui a projeção atual. Falhas são exibidas como não
confirmadas, sem ocultar outros produtos. Totais vêm do backend; leituras, atualização
automática da tela e links não disparam trabalho nem alteram autorizações.

Motivos e último acompanhamento ficam sob demanda. Uma falha de atualização mantém a
fotografia anterior explicitamente datada. Não exibir status de experimento como se fosse
status da atividade. A consulta agregada fica no repositório de leitura do produto, sem
adicionar dependências do catálogo ao contexto mínimo do coordenador. Testes:
`ProductOperationsTest`, `ProductOperationsRunRepositoryTest`,
`ProcessRunRepositoryMinimalContextTest` e
`frontend/e2e/products-in-play-responsive.mjs`.
