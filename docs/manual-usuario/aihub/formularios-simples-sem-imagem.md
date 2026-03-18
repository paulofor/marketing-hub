# Formulários simples (sem upload obrigatório de leads)

Este guia mostra como criar, atualizar e validar os formulários simples do Lead Portal diretamente pelo AI Hub.

## Visão geral

- O construtor "Criar formulário simples (sem imagem)" fica na ficha do nicho (seção **Formulários simples do nicho**).
- Ele gera um fluxo completo com perguntas de contato, múltipla escolha e os textos que alimentam a landing page pública.
- As imagens dos cards são armazenadas automaticamente e agora aparecem tanto no painel quanto na página pública.
- Formulários **não vinculados a experimentos** podem ser editados posteriormente.

## Criando um novo formulário

1. Abra **Nicho → Formulários simples do nicho** e clique em **Novo formulário simples**.
2. Preencha:
   - **Nome/Slug/Descrição** (aparecem na listagem e no portal público).
   - **Estilo visual** (usa as cores e hero configurados em "Estilos do formulário simples").
3. Revise as perguntas variáveis (ex.: título do cabeçalho, textos dos exemplos reais e bullets).
4. Em **Configuração de imagens dos subcards**:
   - Envie 3 arquivos `.png` ou `.jpg` com pelo menos 600px de largura.
   - Cada upload exibe uma prévia e pode ser refeito quantas vezes quiser.
5. Clique em **Criar formulário**. O cartão será salvo, aparecerá na lista e ficará disponível para aprovação/publicação.

## Editando um formulário existente

1. Na lista "Formulários disponíveis", localize o item desejado.
2. Se ele **não estiver ligado a um experimento**, o botão **Editar formulário** ficará visível.
3. Ao clicar, o construtor é aberto em modo de edição (a badge "Em edição" aparece na listagem):
   - Todos os campos e imagens são carregados automaticamente.
   - Ajuste os textos, troque as imagens ou altere o estilo visual.
4. Clique em **Salvar alterações**. O painel fecha e o formulário atualizado volta para a lista.
5. Para descartar a edição, use **Cancelar edição** no topo do cartão.

> ⚠️ Formulários já vinculados a experimentos exibem a mensagem "Vinculado ao experimento #ID" e não podem ser editados (evita inconsistências em campanhas em andamento).

## HTML personalizado da página

- O campo opcional permite substituir toda a landing page pública. O HTML informado é renderizado em um iframe dedicado, sem aplicar os blocos ou estilos padrão do fluxo simples.
- É necessário incluir o seu próprio `<form>` e enviar um POST `multipart/form-data` para `{{url}}`, usando o mesmo formato do portal (campo `payload` em JSON e, se necessário, o arquivo `image`).
- As únicas variáveis disponibilizadas pelo portal são as três imagens configuradas nos subcards e a URL de submissão.
- O evento de renderização utilizado no funil do experimento continua sendo disparado automaticamente pelo portal.

### Tokens principais

| Token | Descrição |
| --- | --- |
| `{{imagem1}}` | URL absoluta da primeira imagem configurada nos subcards. |
| `{{imagem2}}` | URL absoluta da segunda imagem configurada nos subcards. |
| `{{imagem3}}` | URL absoluta da terceira imagem configurada nos subcards. |
| `{{url}}` | URL absoluta do POST `/api/flows/:slug/submissions` para ser utilizada no seu formulário. |

## Como as imagens aparecem para o lead

- A página pública agora lê os campos internos `exemplo_real_card_*` e renderiza os mesmos PNG enviados no construtor.
- Caso nenhuma imagem seja encontrada, o portal usa os exemplos padrão (gradientes ilustrativos), garantindo consistência visual.
- Os textos digitados no construtor alimentam automaticamente as seções "Exemplos reais" e "Por que confiar".

## Publicação e aprovação

1. Gere o formulário.
2. Volte para o experimento e aprove o fluxo quando estiver satisfeito.
3. Após aprovado, o Lead Portal publica automaticamente o slug e as cores configuradas.

Seguindo esses passos você garante que o pipeline continue 100% automático (sem subir arquivos manualmente no deploy) e que os leads vejam exatamente as mesmas imagens configuradas no painel.
