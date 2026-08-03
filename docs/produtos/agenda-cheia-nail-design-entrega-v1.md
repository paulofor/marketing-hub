# Agenda Cheia Nail Design — entrega personalizada v1

**STATUS: IMPLEMENTADO NA WORKTREE**

## Objetivo comercial

Entregar, após pagamento e briefing, um kit personalizado que ajude a nail designer a
apresentar seus serviços e criar mais oportunidades de conversa no WhatsApp. O produto não
garante clientes ou agenda cheia.

## Conteúdo contratado

- 10 posts em PNG, 1080 × 1080;
- 10 stories em PNG, 1080 × 1920;
- 10 legendas com chamada para WhatsApp;
- 5 mensagens de atendimento;
- calendário de publicação de 7 dias;
- instruções simples de uso.

## Personalização aplicada

As artes e textos usam nome profissional, cidade ou região, WhatsApp, serviço principal,
cores preferidas e objetivo semanal informados no briefing. A versão inicial usa composição
visual editorial gerada por código e templates reutilizáveis. Texto sempre é aplicado pelo
compositor, nunca incorporado por geração livre de imagem.

## Pipeline operacional

1. O pagamento aprovado libera o briefing.
2. O briefing é persistido como `BRIEFING_RECEBIDO`.
3. A composição cria posts, stories e textos em diretório privado.
4. O gate automático valida quantidades, dimensões, formato e integridade do ZIP.
5. Nota inferior a 90 bloqueia a entrega e persiste falha técnica.
6. O pacote aprovado recebe token opaco de download.
7. O link é enviado ao e-mail confirmado no briefing.
8. Somente após o envio o pedido recebe status `ENTREGUE`.

## Privacidade e segurança

- o caminho interno do arquivo não aparece no contrato público;
- o download usa token aleatório e não expõe pagamento, briefing ou dados pessoais;
- o manifesto público contém somente itens funcionais do kit;
- logs correlacionam pagamento e briefing, sem registrar o conteúdo pessoal completo;
- falha técnica não é convertida em sucesso funcional.

## Critério antes de tráfego comercial

O fluxo precisa passar pelo PR e deploy. Depois deve ser reprocessado com o pagamento de teste
existente e validado ponta a ponta: briefing, geração, gate, e-mail, download, conteúdo do ZIP e
leitura mobile dos posts e stories.
