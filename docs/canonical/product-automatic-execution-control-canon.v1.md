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

Uma rodada local completa sem defeitos conclui a homologação. Se houver correção após defeito, duas
rodadas completas e consecutivas sem falhas passam a ser obrigatórias.
