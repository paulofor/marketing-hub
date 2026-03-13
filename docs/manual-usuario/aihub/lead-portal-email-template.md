# Template do e-mail de entrega das imagens

Esta funcionalidade permite personalizar o HTML do e-mail enviado automaticamente aos leads quando um pacote de imagens com marca d'água é concluído. O mesmo template é utilizado por todos os experimentos e já injeta o pixel de rastreamento e o rodapé com os dados do pacote.

## Acesso rápido

1. Entre no painel do Marketing Hub.
2. No menu lateral, abra a seção **Campanhas**.
3. Clique em **Template do e-mail do lead**.

## Como editar o HTML

1. A página exibe os tokens disponíveis no painel da esquerda. Clique em **Inserir** para colar o token no ponto atual do texto.
2. No painel da direita, escreva o HTML completo do e-mail (textos, títulos, botões, imagens inline etc.).
3. Use o botão **Salvar alterações** para aplicar o novo template. As mudanças entram em efeito imediatamente para os próximos envios, sem necessidade de qualquer etapa manual de deploy.
4. Caso queira desfazer edições feitas localmente antes de salvar, clique em **Descartar alterações**.

## Variáveis suportadas

| Token | Conteúdo | Observações |
| --- | --- | --- |
| `{{nome_cliente}}` | Nome informado pelo lead no formulário | Ideal para personalizar a saudação inicial. |
| `{{link_pagamento}}` | URL do checkout no Mercado Pago | Utilize em links ou botões sobre a oferta. |
| `{{imagem_previa_1}}` | Primeira imagem com marca d'água | Use em tags `<img>` para destacar os samples. |
| `{{imagem_previa_2}}` | Segunda imagem (quando existir) | Opcional; substituído por vazio se não houver. |
| `{{imagem_previa_3}}` | Terceira imagem (quando existir) | Opcional; substituído por vazio se não houver. |

A plataforma continuará anexando o arquivo `.zip` com todas as imagens e adicionará automaticamente o pixel de abertura e o link de visualização online no final do e-mail.

## Boas práticas

- Construa um bloco de destaque para o botão de pagamento usando `{{link_pagamento}}` e deixe claro o valor/benefícios.
- Utilize `<img src="{{imagem_previa_1}}" />` (e as demais imagens) para mostrar os exemplos com marca d'água antes da chamada para compra.
- Inclua `{{nome_cliente}}` na saudação para reforçar a personalização.
- Evite copiar/colar HTML com estilos externos; prefira CSS inline simples para garantir compatibilidade com ferramentas de e-mail.
- Sempre revise o texto após salvar. O painel indica a data e hora da última atualização para facilitar auditorias.

## Processo automático

Ao salvar, o template é registrado na configuração global do sistema. Os workers responsáveis por disparar os e-mails passam a utilizar o novo HTML automaticamente, mantendo o fluxo de deploy 100% automatizado e sem necessidade de intervenções manuais.
