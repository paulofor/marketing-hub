# Social Media Worker Canon v1

## Objetivo

Criar o executor oficial de midias sociais do Marketing Hub para aquecer mercado,
distribuir videos de PDEs e transformar conteudo organico em sinais de venda.

## Decisao arquitetural

Foram consideradas tres alternativas:

| Alternativa | Beneficio | Risco | Decisao |
| --- | --- | --- | --- |
| Colocar YouTube no backend | Menor quantidade de modulos | Backend assumiria OAuth, upload, polling e execucao operacional | Rejeitada |
| Ampliar `facebook-ads-worker` | Reaproveita padrao existente | Mistura midia paga com organico e social listening | Rejeitada |
| Criar `social-media-worker` | Separacao clara, plugavel por canal e escalavel | Exige novo modulo e contratos de backend | Escolhida |

## Escopo v1

- YouTube como primeiro canal.
- Consumo de fila pelo endpoint existente de distribuição orgânica:
  `/api/social-distribution/publications/pending`.
- Publicacao de videos por `videos.insert` quando OAuth estiver conectado.
- Retorno operacional ao backend por `/publishing`, `/published` e `/failed`.
- Modo `dryRun` habilitado por padrao para validar funil sem publicar.
- Plano de criacao/conexao de canal quando o canal ainda nao existir.
- Plano de aquecimento de mercado com sequencia inicial de conteudo.

## Regra de criacao de canal

O worker nao deve prometer criacao automatica de canal YouTube para contas comuns.
O fluxo correto e criar ou selecionar o canal na conta Google, conectar OAuth no
Marketing Hub e entao permitir que o worker publique e organize o canal.

## Resultado esperado para vendas

Cada publicacao deve preservar:

- produto, experimento ou PDE de origem;
- canal e plataforma;
- status de publicacao;
- URL externa quando existir;
- objetivo comercial;
- proximas acoes de aquecimento;
- falha e causa quando houver bloqueio.

O objetivo nao e apenas postar conteudo: e criar uma base de sinais para melhorar
copy, oferta, criativos pagos e priorizacao de mercado.

## Plano de crescimento orgânico

A Distribuição Orgânica deve persistir, antes da publicação, um plano ligado ao
produto com público, hipótese, objetivo comercial, CTA, destino e campanha UTM.
Cada pauta deve declarar formato, pilar e etapa do funil. A URL rastreável é
gerada pelo backend e a pauta exige aprovação humana antes de ser vinculada a
uma publicação; aprovação nunca enfileira conteúdo automaticamente.

O relatório do plano deve separar audiência de resultado comercial, incluindo
visualizações engajadas, espectadores recorrentes, sessões na landing, leads,
checkouts, vendas e receita atribuída. Visualização ou impacto estimado nunca
deve ser contado como venda. A decisão do backend deve orientar coleta de
amostra, ajuste do CTA ou continuidade usando os eventos persistidos.
