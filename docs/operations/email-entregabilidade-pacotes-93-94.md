# Diagnóstico de entrega de e-mails – pacotes 93 e 94

## Contexto
- Pipeline de imagem processou os pacotes `93` e `94` do Lead Portal e os marcou como `COMPLETED`.
- Usuário final não recebeu os e-mails de amostras, embora o serviço `email-service` não registre erros.
- Envio manual pela tela **Settings › Email Service** funciona, indicando que as credenciais SMTP são válidas.

## Evidências coletadas
1. **Banco de dados (`flow_submission_image_package`)**

   | pacote | status | tentativas | última tentativa (UTC-3) | `notified_at` | anexo |
   | --- | --- | --- | --- | --- | --- |
   | 93 | COMPLETED | 1 | 2026-03-11 13:36:12 | 2026-03-11 13:36:12 | `archives/lead-portal/lead-portal-package-93.zip` |
   | 94 | COMPLETED | 1 | 2026-03-11 14:08:08 | 2026-03-11 14:08:08 | `archives/lead-portal/lead-portal-package-94.zip` |

   👉 ambos os registros foram marcados como notificados.

2. **Tabela `email_log`**

   | id | destinatário | assunto | status | enviado em |
   | --- | --- | --- | --- | --- |
   | 32 | `paforest1970@gmail.com` | `PAULO ALEXANDRE...` | SENT | 2026-03-11 13:36:13 |
   | 33 | `paforest1970@gmail.com` | `Possibilidades de Negocios...` | SENT | 2026-03-11 14:08:08 |

3. **Logs do `email-service`**

   ```text
   2026-03-11T14:07:49Z INFO LeadPortalPaymentsClient : Solicitando checkout para o pacote 94...
   2026-03-11T14:08:03Z INFO LeadPortalEmailDispatchService : Enviando pacote 94 para paforest1970@gmail.com (arquivo='imagens-watermark-94.zip')
   2026-03-11T14:08:07Z INFO EmailSenderService : E-mail enviado com sucesso para paforest1970@gmail.com (resposta SMTP: 250 Ok 0100019cdd39ed2b-9d2a219c-6ec6-4ab3-ae1f-8902497e5831-000000)
   2026-03-11T14:08:08Z INFO LeadPortalEmailDispatchService : Pacote 94 enviado para paforest1970@gmail.com
   ```

   ✔️ Provador SMTP (Amazon SES) aceitou a mensagem.

## Teste independente do SMTP
Enviei manualmente o mesmo anexo `lead-portal-package-94.zip` para `marketinghubtest@mailinator.com` usando as credenciais configuradas (`email-smtp.us-east-1.amazonaws.com`).

- Mensagem registrada pela Mailinator com ID `marketinghubtest-1773241504-01215518529012` e DKIM válido para `vitrineproduto.online`.
- Confirma que o provedor SMTP está aceitando e distribuindo mensagens para domínios externos.

## Diagnóstico
- O domínio de envio configurado atualmente é **imagens@vitrineproduto.online**.
- Consulta DNS (`https://dns.google/resolve?name=vitrineproduto.online&type=TXT`) mostra que o domínio **não possui SPF** publicado. Sem SPF/DMARC o Gmail tende a reter ou descartar mensagens que contenham anexos e links de pagamento.
- Como o problema ocorre apenas em Gmail/contatos reais (não em Mailinator nem no envio de teste), o comportamento está alinhado com uma política de segurança do destinatário e **não com falha do código**.

## Próximos passos (dependem de acesso a DNS / SES)
1. **Publicar SPF para `vitrineproduto.online`:**
   - Registrar TXT na zona raiz com, por exemplo: `v=spf1 include:amazonses.com ~all` (ou política `-all` se desejar bloqueio rígido).
2. **Confirmar se os CNAMEs de DKIM gerados pelo SES continuam ativos (já existe o selector `sc5cg7glsz6qrrd4wgyn7pndmnfuxwax`).**
3. **Opcional:** criar registro DMARC mínimo (`v=DMARC1; p=none; rua=mailto:postmaster@vitrineproduto.online`).
4. Após propagação (até 24h), reexecutar o pipeline e validar abertura via Gmail. Enquanto isso, considerar voltar o remetente para `imagens@oportunidadebrasil.shop` (domínio que já possui SPF Hostinger) através da tela de configurações.

## Itens fora do alcance deste repositório
- Alterações de DNS / verificação de domínio no Amazon SES dependem do acesso ao provedor de domínio. Não consigo executá-las a partir do sandbox.

## Referências úteis
- [Amazon SES – Authenticating Email](https://docs.aws.amazon.com/ses/latest/dg/authenticate-domain.html)
- [Registro do teste via Mailinator](https://www.mailinator.com/api/v2/domains/public/inboxes/marketinghubtest/messages/marketinghubtest-1773241504-01215518529012)
- [Consulta DNS sem SPF](https://dns.google/resolve?name=vitrineproduto.online&type=TXT)
