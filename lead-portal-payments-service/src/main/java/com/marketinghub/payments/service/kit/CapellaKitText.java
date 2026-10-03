package com.marketinghub.payments.service.kit;

import com.marketinghub.payments.model.AgendaCheiaBriefing;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compõe os textos do kit conforme profissão e briefing, preservando as condições contratadas. */
public final class CapellaKitText {
    private static final List<String> FORBIDDEN = List.of("payload", "debug", "prompt", "localhost", "jobid");

    /** Cria dez legendas com chamada clara e sem garantia de resultado. */
    public List<String> captions(AgendaCheiaBriefing briefing, CapellaKitProfile profile) {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            result.add((index + 1) + ". " + profile.headlines().get(index) + "\n\n" + serviceName(briefing)
                    + " em " + publicText(briefing.getCityRegion())
                    + ". Quer consultar horários? Chame no WhatsApp: "
                    + publicText(briefing.getWhatsapp()) + ".");
        }
        return result;
    }

    /** Cria cinco respostas comerciais reutilizáveis no WhatsApp. */
    public String whatsappMessages(AgendaCheiaBriefing briefing) {
        return "1. Oi! Que bom receber sua mensagem. Qual serviço você deseja fazer?\n\n"
                + "2. Tenho opções de horário nesta semana. Qual período funciona melhor para você?\n\n"
                + "3. Trabalho com " + serviceName(briefing) + ". Posso te explicar as opções e valores.\n\n"
                + "4. Quer que eu reserve esse horário enquanto confirmamos os detalhes?\n\n"
                + "5. Posso enviar as próximas disponibilidades pelo WhatsApp?";
    }

    /** Cria o calendário funcional de sete dias. */
    public String calendar(CapellaKitProfile profile) {
        List<String> days = new ArrayList<>();
        for (int index = 0; index < profile.calendarDays().size(); index++) {
            days.add("Dia " + (index + 1) + " — " + profile.calendarDays().get(index));
        }
        return String.join("\n", days);
    }

    /** Entrega instruções, formatos e condições de atendimento junto dos arquivos comprados. */
    public String instructions(AgendaCheiaBriefing briefing, CapellaKitProfile profile) {
        return profile.productName().toUpperCase(Locale.ROOT) + " — KIT PERSONALIZADO\n\nProduzido para: "
                + publicText(briefing.getProfessionalName())
                + "\n\nUse uma arte por dia com a legenda correspondente. "
                + "O kit melhora sua apresentação e cria oportunidades de conversa; não garante clientes ou agendamentos."
                + "\n\nARQUIVOS E USO\n10 posts PNG 1080x1080, 10 stories PNG 1080x1920, "
                + "10 legendas, 5 mensagens de WhatsApp e calendário de 7 dias em texto. "
                + "As artes são prontas para publicar; não incluem arquivo editável Canva ou PSD. "
                + "Guarde sua cópia do ZIP. Use os arquivos na divulgação do próprio negócio; "
                + "não revenda nem redistribua o kit. As fotografias não são exclusivas."
                + "\n\nATENDIMENTO\ncontato@digicomdigital.com.br — primeira resposta em até 1 dia útil "
                + "(segunda a sexta, exceto feriados nacionais, horário de Brasília). "
                + "Suporte de uso por 7 dias corridos após a entrega, incluindo acesso, download "
                + "e correção de divergências em relação ao briefing. Não inclui gestão de redes sociais, "
                + "nova identidade visual ou revisões ilimitadas."
                + "\n\nREEMBOLSO\nVocê pode solicitar reembolso integral desde a compra até 7 dias corridos "
                + "após receber o kit, sem precisar justificar, pelo mesmo e-mail. Informe o e-mail da compra "
                + "e a identificação do pagamento, nunca dados do cartão. Solicitamos o estorno sem demora; "
                + "a compensação depende do meio de pagamento. Falhas de entrega, problemas nos arquivos "
                + "e outros direitos continuam sendo atendidos após o período de suporte de uso.";
    }

    /** Resolve um serviço curto para não poluir as artes. */
    public String serviceName(AgendaCheiaBriefing briefing) {
        String first = publicText(briefing.getServices().split("[,;\\n]")[0].trim());
        return first.length() > 60 ? first.substring(0, 60) : first;
    }

    /** Sanitiza dados do briefing antes de incorporá-los aos artefatos públicos. */
    public String publicText(String value) {
        String result = value == null ? "" : value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").trim();
        for (String forbidden : FORBIDDEN) {
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(forbidden), "");
        }
        return result.replaceAll("\\s+", " ");
    }

}
