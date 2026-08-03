package com.marketinghub.payments.service;

import com.marketinghub.payments.dto.AgendaCheiaBriefingRequest;
import com.marketinghub.payments.dto.AgendaCheiaBriefingResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.repository.AgendaCheiaBriefingRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valida o pagamento e registra o briefing que inicia a produção do Agenda Cheia. */
@Service
public class AgendaCheiaPostPurchaseService {
    private static final Logger log = LoggerFactory.getLogger(AgendaCheiaPostPurchaseService.class);
    private static final String PRODUCT_KEY = "agenda-cheia-nail-design";

    private final CheckoutService checkoutService;
    private final AgendaCheiaBriefingRepository repository;

    /** Configura a consulta de pagamento e a persistência do briefing. */
    public AgendaCheiaPostPurchaseService(
            CheckoutService checkoutService, AgendaCheiaBriefingRepository repository) {
        this.checkoutService = checkoutService;
        this.repository = repository;
    }

    /** Confirma se o pagamento pertence ao produto e está aprovado. */
    public AgendaCheiaBriefingResponse paymentStatus(String paymentId) {
        MercadoPagoPaymentDetails payment = approvedPayment(paymentId);
        return repository.findByPaymentId(payment.id())
                .map(this::toResponse)
                .orElse(new AgendaCheiaBriefingResponse(null, payment.id(), "AGUARDANDO_BRIEFING", null));
    }

    /** Salva o briefing uma única vez e o deixa pronto para a produção. */
    @Transactional
    public AgendaCheiaBriefingResponse submit(AgendaCheiaBriefingRequest request) {
        MercadoPagoPaymentDetails payment = approvedPayment(request.paymentId());
        AgendaCheiaBriefing briefing = repository.findByPaymentId(payment.id())
                .orElseGet(AgendaCheiaBriefing::new);
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
        return toResponse(saved);
    }

    /** Carrega e valida o pagamento diretamente na fonte autoritativa. */
    private MercadoPagoPaymentDetails approvedPayment(String paymentId) {
        MercadoPagoPaymentDetails payment = checkoutService.fetchPayment(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));
        if (!"approved".equalsIgnoreCase(payment.status())) {
            throw new IllegalStateException("Pagamento ainda não foi aprovado");
        }
        if (!PRODUCT_KEY.equalsIgnoreCase(payment.externalReference())) {
            throw new IllegalArgumentException("Pagamento não pertence ao Agenda Cheia Nail Design");
        }
        return payment;
    }

    /** Converte a entidade para o contrato público sem expor dados pessoais. */
    private AgendaCheiaBriefingResponse toResponse(AgendaCheiaBriefing briefing) {
        return new AgendaCheiaBriefingResponse(
                briefing.getId(), briefing.getPaymentId(), briefing.getStatus(), briefing.getSubmittedAt());
    }

    /** Normaliza campos opcionais vazios. */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
