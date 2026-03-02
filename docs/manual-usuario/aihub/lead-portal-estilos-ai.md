# Geração de estilos do formulário simples (Lead Portal)

Esta funcionalidade permite produzir temas visuais completos para os formulários simples do Lead Portal a partir de um único prompt. O sistema combina o contexto informado com instruções internas e envia a solicitação para o modelo OpenAI configurado em modo **batch**, registrando automaticamente o prompt, o modelo e o custo da geração.

## Onde acessar

1. Abra o painel "AI Hub".
2. Navegue até **Lead Portal → Estilos do formulário simples**.
3. A página apresenta o formulário de criação à esquerda e a lista de estilos gerados à direita.

## Gerando um novo estilo

1. **Nome e slug:** defina como o estilo aparecerá na biblioteca. O slug é usado para vincular o estilo ao fluxo simples.
2. **Descrição opcional:** ajude a IA com o objetivo do fluxo (ex.: "captar leads para consultoria fitness premium").
3. **Modelo OpenAI:** escolha um dos modelos cadastrados em *Configurações → Modelos OpenAI*. Esse código determina qual preço será aplicado ao cálculo do custo.
4. **Prompt criativo:** descreva o clima visual desejado (cores, texturas, referências de marca, público-alvo etc.). Quanto mais referências concretas, melhor o resultado.
5. **Imagem de prévia (opcional):** informe um URL caso deseje exibir uma composição específica no hero.
6. Clique em **Gerar estilo**. O back-end enviará a solicitação via batch, salvará a definição completa e registrará o custo em USD.

> **Dica:** mantenha prompts curtos porém específicos. Exemplo: *"Tema minimalista inspirado em studios de pilates, usar fundo lilás claro com degradê, botões pill brancos com sombra suave, hero com foto artística preto e branco."*

## Editando ou regerando um estilo existente

1. Clique em **Editar estilo** na lista à direita.
2. Ajuste nome, descrição, modelo ou prompt.
3. Caso queira apenas atualizar metadados (por exemplo, a URL da imagem), deixe a opção "Gerar uma nova variação agora" desmarcada.
4. Para forçar uma nova versão mantendo o mesmo prompt/modelo, marque **Gerar uma nova variação agora** antes de salvar.

Sempre que o estilo for regenerado:
- O prompt e o modelo utilizados serão armazenados nos campos `textPrompt` e `textModel`.
- O campo `generationCostUsd` exibido no cartão será atualizado com o custo calculado.
- O histórico (prompt renderizado + resposta bruta) fica disponível em `textParameters` para auditoria.

## Como conectar o estilo em um formulário de experimento

Essa funcionalidade **já está implementada** no fluxo de criação de formulário simples dentro de Experimentos/Nicho.

1. Depois de gerar (ou editar) o estilo no AI Hub, acesse a tela do nicho onde você cria formulários simples.
2. No card **Criar formulário simples (sem imagem)**, localize o campo **Estilo visual do formulário**.
3. Selecione o estilo desejado pelo nome.
4. Conclua a criação do formulário normalmente.

O sistema envia o `simpleFormStyleId` junto da criação do fluxo. Na publicação, o Lead Portal aplica automaticamente as cores, botões e hero definidos no estilo selecionado.

> Se o seletor estiver vazio, primeiro cadastre ao menos um estilo em **Campanhas → Estilos do formulário simples**.

## Onde validar se o estilo foi aplicado

1. Abra o fluxo criado na lista de Experimentos (aba Lead Portal).
2. Acesse a **URL pública** do formulário.
3. Confira se o tema visual (background, botão e hero) corresponde ao estilo escolhido.

## Boas práticas de prompt

- Cite referências de cor usando hex ou descrições claras ("verde esmeralda", "degradê sunset").
- Indique o tipo de hero desejado: imagem lateral, ilustração, fotos reais etc.
- Mencione o público-alvo ou a proposta de valor quando relevante.
- Evite pedir múltiplos estilos no mesmo prompt; gere variações separadas para comparar resultados.

## Monitoramento de custos

Cada cartão exibe o valor monetário da geração (campo **Custo US$**). Esse número é calculado usando o preço batch configurado para o modelo escolhido e os tokens retornados pela API.

Use essa informação para comparar quais prompts/modelos entregam o melhor equilíbrio entre estética e custo operacional.
