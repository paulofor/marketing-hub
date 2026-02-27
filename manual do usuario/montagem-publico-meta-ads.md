# Montagem do público Meta Ads em 4 etapas

Este guia explica como aproveitar os interesses, cargos e comportamentos sugeridos pelo Marketing Hub até que o experimento apareça como **“Público completo”**.

## 1. Cadastrar segmentações sugeridas no nicho

1. Abra o nicho desejado e role até **Segmentações sugeridas**.
2. Use os campos de **Interesses**, **Cargos** e **Comportamentos** para adicionar termos relevantes (ex.: “Marketing digital”, “Diretor comercial”, “Pais de recém-nascidos”).
3. Cada item é salvo imediatamente e sincronizado com a lista manual do nicho.
4. O status inicial é **“Pendente Meta Ads”**.

> Dica: mantenha os termos objetivos. O worker só consegue encontrar IDs oficiais quando o texto é parecido com o que existe no catálogo da Meta.

## 2. Worker consulta IDs oficiais na Meta

1. O Facebook Ads Worker busca periodicamente todos os itens pendentes através do endpoint interno `/internal/targeting/elements/metaads-pending`.
2. Para cada termo ele chama a API `search` do Meta Ads, captura o `metaId`, o nome apresentado no gerenciador e o intervalo estimado de pessoas.
3. Assim que encontramos um ID, o card da lista muda para **“Meta Ads pronto”** e, logo abaixo do termo, aparecem:
   - `Meta ID`: exibido dentro de `<code>` para facilitar o copy/paste.
   - `Alcance estimado`: soma o intervalo inferior e superior retornado pelo Meta.
4. Se precisar rodar novamente (por exemplo após editar o termo), clique no botão de recarregar para agendar um novo enriquecimento.

## 3. Escolher o público dentro do experimento

1. Abra o experimento relacionado ao nicho e vá para a aba **Segmentação**.
2. A primeira seção exibe todos os elementos que já possuem ID oficial.
3. Marque quantos interesses, cargos e comportamentos desejar: eles são combinados com **lógica OR**, ou seja, cada item aumenta o alcance total.
4. O painel mostra o **alcance estimado combinado** somando os limites inferior e superior de todas as seleções.
5. Clique em **Salvar seleção** quando estiver satisfeito e, se quiser iniciar a validação automática dos IDs, use **Executar fluxo simples**.

> Não é obrigatório escolher um item de cada lista. Um único interesse já libera o diagnóstico.

## 4. Confirmar o status “Público completo”

1. Volte para o topo do experimento. O cartão “Público completo” agora exibirá o check verde e o botão “Ir para Segmentação” ficará desabilitado.
2. O diagnóstico geral também passa a considerar o público como “liberado”, permitindo o disparo automático dos conjuntos de anúncios.

### Resumo visual dos estados

| Local | Estado | Significado |
| --- | --- | --- |
| Nicho → Segmentações sugeridas | **Pendente Meta Ads** | O worker ainda não encontrou o ID oficial. |
| Nicho → Segmentações sugeridas | **Meta Ads pronto** | ID + alcance já registrados e visíveis logo abaixo do termo. |
| Experimento → Segmentação | Seleções marcadas + barra de alcance | Combinação OR dos itens escolhidos. |
| Experimento → Diagnóstico | **Público completo** | Pelo menos uma segmentação com ID da Meta foi salva. |

Seguindo essas quatro etapas o time garante que cada experimento tenha, no mínimo, um público válido e mensurável para ser usado pelo worker do Meta Ads.
