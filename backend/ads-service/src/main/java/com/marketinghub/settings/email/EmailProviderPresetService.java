package com.marketinghub.settings.email;

import com.marketinghub.settings.dto.EmailProviderPresetResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmailProviderPresetService {

    public List<EmailProviderPresetResponse> listPresets() {
        return List.of(
                amazonSes(),
                sendGrid(),
                mailtrap(),
                brevo(),
                mailjet(),
                resend()
        );
    }

    private EmailProviderPresetResponse amazonSes() {
        return new EmailProviderPresetResponse(
                "amazon-ses",
                "Amazon SES",
                "Pay as you go com infraestrutura AWS",
                "Serviço transacional da Amazon com integração nativa aos recursos da AWS e opções de IP dedicado.",
                "https://docs.aws.amazon.com/ses/latest/dg/smtp-connect.html",
                "https://aws.amazon.com/ses/pricing/",
                "US$ 0,10 por 1.000 emails enviados + cobrança por anexos (pague pelo uso).",
                "Times que já utilizam AWS e precisam de escala global",
                "3.000 emails/mês por 12 meses para contas novas que enviam via EC2 ou Lambda.",
                "email-smtp.sa-east-1.amazonaws.com",
                587,
                List.of(25, 465, 2465, 2587),
                true,
                true,
                true,
                "Crie credenciais SMTP no console do SES; usuário segue o padrão AKIA...",
                List.of(
                        "Suporta múltiplas regiões e monitoração via CloudWatch",
                        "Permite IPs dedicados e warm-up automático",
                        "Integra com SNS/SQS para feedback loops"
                ),
                "Substitua o host para a região do seu SES (ex.: email-smtp.us-east-1.amazonaws.com)."
        );
    }

    private EmailProviderPresetResponse sendGrid() {
        return new EmailProviderPresetResponse(
                "twilio-sendgrid",
                "Twilio SendGrid",
                "Plano Essentials a partir de US$ 19,95/mês",
                "Plataforma popular com SDKs, templates dinâmicos e monitoramento em tempo real.",
                "https://www.twilio.com/docs/sendgrid/for-developers/sending-email/integrating-with-the-smtp-api",
                "https://sendgrid.com/en-us/pricing",
                "Free Trial com 100 emails/dia; plano Essentials desde US$ 19,95/mês.",
                "Equipes que precisam de suporte a templates avançados e integrações prontas",
                "100 emails/dia durante o período de teste sem custos.",
                "smtp.sendgrid.net",
                587,
                List.of(25, 465),
                true,
                true,
                false,
                "Usuário deve ser sempre 'apikey' e a senha é o API Key gerado no painel.",
                List.of(
                        "SDKs oficiais em várias linguagens",
                        "Webhooks de eventos (abertura, clique, bounce)",
                        "Opção de IP dedicado nos planos Pro/Premier"
                ),
                "Reforce a autenticação SPF/DKIM dentro do painel SendGrid antes de iniciar os envios."
        );
    }

    private EmailProviderPresetResponse mailtrap() {
        return new EmailProviderPresetResponse(
                "mailtrap",
                "Mailtrap",
                "Entrega transacional com foco em observabilidade",
                "Plataforma com sandbox, logs detalhados e plano gratuito com 4.000 envios/mês.",
                "https://docs.mailtrap.io/email-api-smtp/setup/smtp-integration",
                "https://mailtrap.io/pricing",
                "Planos pagos a partir de US$ 15/mês para 10.000 emails.",
                "Startups que precisam visualizar logs completos e migrar gradualmente do ambiente de testes",
                "Plano Free com 4.000 emails/mês e limite diário de 150 mensagens.",
                "live.smtp.mailtrap.io",
                587,
                List.of(465, 2525),
                true,
                true,
                false,
                "Usuário padrão 'api' e senha é o token gerado para cada domínio/stream.",
                List.of(
                        "Modo sandbox e produção no mesmo painel",
                        "Logs com corpo da mensagem e eventos por 3 a 30 dias",
                        "Streams separados para transacional e marketing"
                ),
                "Cada domínio verificado possui credenciais diferentes; copie sempre do painel do domínio correto."
        );
    }

    private EmailProviderPresetResponse mailjet() {
        return new EmailProviderPresetResponse(
                "mailjet",
                "Mailjet",
                "Planos em dólar com 6.000 envios gratuitos",
                "SMTP relay europeu com editor drag-and-drop, automações e multiusuário integrado.",
                "https://dev.mailjet.com/smtp-relay/configuration/",
                "https://www.mailjet.com/pricing/",
                "Free mantém 6.000 emails/mês (200/dia); Essential começa em US$ 17/mês e Premium em US$ 27/mês para 15.000 envios.",
                "Equipes que precisam de campanhas e automações com editor visual",
                "Plano Free inclui editor, formulários e APIs sem custo.",
                "in-v3.mailjet.com",
                587,
                List.of(25, 80, 465, 588, 2525),
                true,
                true,
                false,
                "Login = API Key pública (MJAPIKEYPUBLIC) e senha = chave privada (MJAPIKEYPRIVATE).",
                List.of(
                        "Editor drag-and-drop com templates prontos",
                        "Automação e segmentação disponíveis já no Essential",
                        "Multiusuário e colaboração em tempo real nos planos Premium"
                ),
                "Autentique o domínio e escolha a porta 587 ou 465 conforme firewall local."
        );
    }

    private EmailProviderPresetResponse brevo() {
        return new EmailProviderPresetResponse(
                "brevo",
                "Brevo (ex-Sendinblue)",
                "Automação multicanal com planos a partir de US$ 9",
                "Inclui campanhas de marketing, SMS e fluxo transacional no mesmo painel.",
                "https://www.brevo.com/pricing/",
                "https://www.brevo.com/pricing/",
                "Starter parte de US$ 9/mês (5.000 emails) e Standard a partir de US$ 18/mês.",
                "Negócios que precisam disparar email + SMS + automações básicas",
                "Plano Free envia até 300 emails por dia com editor drag-and-drop.",
                "smtp-relay.brevo.com",
                587,
                List.of(465, 2525),
                true,
                true,
                false,
                "Usuário e senha são fornecidos na aba SMTP do painel Brevo; cada chave controla um stream.",
                List.of(
                        "Fluxos de automação e formulários nativos",
                        "Opção de suporte em português",
                        "Permite enviar WhatsApp e SMS a partir do mesmo contrato"
                ),
                "O domínio precisa estar autenticado (SPF/DKIM) para liberar o envio acima dos limites iniciais."
        );
    }

    private EmailProviderPresetResponse resend() {
        return new EmailProviderPresetResponse(
                "resend",
                "Resend",
                "Infra moderna com React Email e API amigável",
                "Serviço focado em desenvolvedores, com SDKs e suporte a recebimento (inbound).",
                "https://resend.com/docs/introduction",
                "https://resend.com/pricing",
                "Plano Pro começa em US$ 20/mês (50.000 emails) com excedente de US$ 0,90/1.000.",
                "Times que querem integrações via API e React Email dentro do código",
                "Plano Free oferece 3.000 emails/mês com retenção de logs por 1 dia.",
                "smtp.resend.com",
                587,
                List.of(465),
                true,
                true,
                false,
                "Use o token criado em API Keys como senha; o usuário é o próprio token.",
                List.of(
                        "Suporte a múltiplas regiões e inbound email",
                        "Templates React Email versionáveis",
                        "Webhook assinado para todos os eventos"
                ),
                "Gere tokens específicos para cada ambiente (produção, staging) para facilitar a rotação periódica."
        );
    }
}
