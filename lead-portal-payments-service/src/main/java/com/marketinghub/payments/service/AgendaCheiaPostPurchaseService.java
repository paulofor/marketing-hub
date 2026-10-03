package com.marketinghub.payments.service;

import com.marketinghub.payments.dto.AgendaCheiaBriefingRequest;
import com.marketinghub.payments.dto.AgendaCheiaBriefingResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.repository.AgendaCheiaBriefingRepository;
import com.marketinghub.payments.service.kit.CapellaKitCatalog;
import com.marketinghub.payments.service.kit.BriefingKitProfile;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Valida o pagamento e registra o briefing que inicia a produção do Agenda Cheia. */
@Service
public class AgendaCheiaPostPurchaseService {
    private static final Logger log = LoggerFactory.getLogger(AgendaCheiaPostPurchaseService.class);

    private final CheckoutService checkoutService;
    private final AgendaCheiaBriefingRepository repository;
    private final DigitalProductPostPurchaseEmailService emailService;
    private final AgendaCheiaKitProductionService productionService;

    /** Configura a consulta de pagamento e a persistência do briefing. */
    public AgendaCheiaPostPurchaseService(
            CheckoutService checkoutService,
            AgendaCheiaBriefingRepository repository,
            DigitalProductPostPurchaseEmailService emailService,
            AgendaCheiaKitProductionService productionService) {
        this.checkoutService = checkoutService;
        this.repository = repository;
        this.emailService = emailService;
        this.productionService = productionService;
    }

    /** Confirma se o pagamento pertence ao produto e está aprovado. */
    public AgendaCheiaBriefingResponse paymentStatus(String paymentId) {
        MercadoPagoPaymentDetails payment = approvedPayment(paymentId);
        BriefingKitProfile profile = profile(payment);
        return repository.findByPaymentId(payment.id())
                .map(briefing -> toResponse(briefing, profile))
                .orElse(new AgendaCheiaBriefingResponse(null, payment.id(), "AGUARDANDO_BRIEFING", null, profile));
    }

    /** Confere a profissão comprada, salva o briefing e preserva sem reenvio uma entrega concluída. */
    public AgendaCheiaBriefingResponse submit(AgendaCheiaBriefingRequest request) {
        MercadoPagoPaymentDetails payment = approvedPayment(request.paymentId());
        BriefingKitProfile profile = profile(payment);
        if (request.professionCode() != null && !request.professionCode().equals(profile.code())) {
            throw new IllegalArgumentException("A profissão do briefing difere do kit comprado");
        }
        Optional<AgendaCheiaBriefing> existing = repository.findByPaymentId(payment.id());
        if (existing.isPresent() && "ENTREGUE".equals(existing.get().getStatus())) {
            return toResponse(existing.get(), profile);
        }
        AgendaCheiaBriefing briefing = existing.orElseGet(AgendaCheiaBriefing::new);
        briefing.setPaymentId(payment.id());
        briefing.setBuyerEmail(request.buyerEmail().trim());
        briefing.setProfessionalName(request.professionalName().trim());
        briefing.setCityRegion(request.cityRegion().trim());
        briefing.setWhatsapp(request.whatsapp().trim());
        briefing.setServices(request.services().trim());
        briefing.setVisualStyle(request.visualStyle().trim());
        briefing.setPreferredColors(trimToNull(request.preferredColors()));
        briefing.setWeeklyGoal(request.weeklyGoal().trim());
        briefing.setNotes(trimToNull(request.notes()));
        briefing.setStatus("BRIEFING_RECEBIDO");
        briefing.setSubmittedAt(Instant.now());
        AgendaCheiaBriefing saved = repository.save(briefing);
        log.info("Briefing Agenda Cheia recebido. paymentId={}, briefingId={}", payment.id(), saved.getId());
        sendBriefingConfirmation(payment, request);
        productionService.produceAndDeliver(saved, payment);
        saved.setStatus("ENTREGUE");
        repository.save(saved);
        return toResponse(saved, profile);
    }

    /** Envia ao endereço confirmado no briefing o link seguro do pós-compra. */
    private void sendBriefingConfirmation(
            MercadoPagoPaymentDetails payment, AgendaCheiaBriefingRequest request) {
        try {
            emailService.sendToRecipient(payment, request.buyerEmail().trim(), request.professionalName().trim());
        } catch (Exception ex) {
            log.error(
                    "Falha ao enviar confirmação do briefing Agenda Cheia (paymentId={}, professionalName={})",
                    payment.id(),
                    request.professionalName(),
                    ex);
        }
    }

    /** Carrega e valida o pagamento diretamente na fonte autoritativa. */
    private MercadoPagoPaymentDetails approvedPayment(String paymentId) {
        MercadoPagoPaymentDetails payment = checkoutService.fetchPayment(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));
        if (!"approved".equalsIgnoreCase(payment.status())) {
            throw new IllegalStateException("Pagamento ainda não foi aprovado");
        }
        CapellaKitCatalog.forPaymentReference(payment.externalReference());
        return payment;
    }

    /** Converte a entidade para o contrato público sem expor dados pessoais. */
    private AgendaCheiaBriefingResponse toResponse(AgendaCheiaBriefing briefing, BriefingKitProfile profile) {
        return new AgendaCheiaBriefingResponse(
                briefing.getId(), briefing.getPaymentId(), briefing.getStatus(), briefing.getSubmittedAt(), profile);
    }

    /** Resolve a profissão no contrato autoritativo do pagamento, nunca no texto livre do briefing. */
    private BriefingKitProfile profile(MercadoPagoPaymentDetails payment) {
        return BriefingKitProfile.from(CapellaKitCatalog.forPaymentReference(payment.externalReference()));
    }

    /** Normaliza campos opcionais vazios. */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
