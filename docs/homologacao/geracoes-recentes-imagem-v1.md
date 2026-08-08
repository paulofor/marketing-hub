# Matriz de homologação — gerações recentes de imagem v1

## Objetivo

Recuperar imagens concluídas após recarga ou timeout, sem nova chamada de IA, preservando o contexto comercial e o gate do Aprovador.

## Cenários obrigatórios

| Área | Cenário | Resultado esperado |
| --- | --- | --- |
| Caminho feliz | Selecionar produto, plano e experimento com geração concluída | Lista leve aparece e o asset escolhido é recuperado |
| Validação | Não selecionar produto ou plano | Histórico não é consultado |
| Falha | Asset persistido inválido | Erro funcional; nenhuma nova geração é disparada |
| Integração | Recuperar job do mesmo contexto | Prompt, lote e imagem são restaurados para seleção |
| Segregação | Solicitar job de outro produto, plano ou experimento | Backend responde como não encontrado |
| Observabilidade | Recuperação falha | Log contém operação e `jobId`, com stack trace |
| Métrica/custo | Recuperar imagem existente | Nenhum lançamento ou chamada de geração é criado |
| Desktop | Chromium em viewport desktop | Lista, imagem e formulário permanecem operáveis |
| Mobile | Chromium emulado como iPhone e Pixel | Lista e seleção permanecem legíveis e tocáveis |

## Rodadas locais

Executar cinco rodadas completas e consecutivas. Se qualquer rodada falhar, corrigir a causa-raiz e reiniciar a contagem.
