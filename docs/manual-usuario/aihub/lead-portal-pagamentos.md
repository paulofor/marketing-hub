# Pagamentos do Lead Portal

Este guia mostra como configurar e acompanhar os checkouts do Mercado Pago
gerados automaticamente pelo Lead Portal. Depois dos ajustes recentes, o valor
dos pacotes passou a respeitar o **Preço unitário (R$)** definido no
experimento responsável pelo fluxo, sem necessidade de ajustes manuais.

## 1. Defina o preço direto no experimento

1. Acesse **Experimentos** no menu lateral e abra o experimento desejado.
2. Clique em **Editar** e preencha o campo **Preço unitário (R$)** com o valor
   que deve ser cobrado por pacote.
3. Salve as alterações. Esse valor será aplicado em todos os novos pacotes
   gerados a partir desse experimento.

> **Importante:** caso o campo fique vazio, o sistema volta a usar o valor
> padrão (R$ 49,90). Sempre mantenha o preço unitário atualizado antes de
> liberar um fluxo para tráfego.

## 2. Vincule o fluxo correto ao experimento

1. Ainda dentro do experimento, abra a aba **Lead Portal**.
2. Use o seletor **Fluxo publicado** para escolher qual formulário/fluxo será
   usado pelo experimento.
3. Salve. O vínculo garante que os pacotes oriundos desse fluxo herdem o preço
   configurado no passo anterior.

Se você gerar versões personalizadas do formulário pelo módulo **Lead Portal**,
lembre-se de voltar ao experimento e atualizar o fluxo vinculado. O preço só é
aplicado quando o fluxo está associado ao experimento.

## 3. Gere e monitore os checkouts no front-end

1. Acompanhe os pacotes prontos em **Lead Portal → Pacotes de imagens**.
2. Ao selecionar um pacote concluído, use a ação **Gerar link do Mercado Pago**
   (ou reabrir checkout) para que o lead receba o link atualizado.
3. Para ver todas as preferências criadas, acesse **Financeiro → Pagamentos**.
   - Use os filtros para localizar o comprador.
   - Clique em um pagamento para abrir a página detalhada. Ali é possível
     copiar o link de checkout, reenviar e visualizar os webhooks recebidos.

Todo checkout é criado com prazo padrão de 72 horas. Se expirar, basta gerar um
novo link pela tela de pagamentos; o valor continuará sincronizado com o
experimento.

## 4. Boas práticas

- Revise sempre o preço unitário antes de ativar campanhas.
- Vincule apenas um fluxo ativo por experimento para evitar cobranças
  divergentes.
- Use a página de detalhes do pagamento para checar o status retornado pelo
  Mercado Pago e, se necessário, reenviar e-mails pelo botão dedicado.
- Em templates de e-mail (menu **Lead Portal → Modelos de e-mail**), utilize o
  token `{{link_pagamento}}` para inserir automaticamente o checkout correto
  na comunicação enviada ao lead.

Seguindo esses passos, o valor exibido no Mercado Pago será exatamente o que
você definiu no experimento, mantendo o processo 100% automático.
