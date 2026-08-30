package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.model.AccessGrant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato exato e não reutilizável dos eventos comerciais do Rigel. */
class RigelCommercialEventContractTest {

    /** Enriquece o evento com a identidade congelada sem copiar o bearer para os metadados. */
    @Test
    void enrichesRigelMetadataWithIrreversibleAccessReference() {
        AccessGrant grant = mock(AccessGrant.class);
        when(grant.getProductSlug()).thenReturn(RigelPaidEntitlementService.PRODUCT_SLUG);
        when(grant.getToken()).thenReturn("Bearer-Com-Caixa");

        Map<String, Object> metadata = RigelCommercialEventContract.enrichAccessMetadata(
                grant, Map.of("accessToken", "nao-pode-persistir", "missionId", "entrada-guiada"));

        assertThat(metadata)
                .containsEntry("productSlug", RigelPaidEntitlementService.PRODUCT_SLUG)
                .containsEntry("experimentId", RigelCommercialEventContract.EXPERIMENT_ID)
                .containsEntry("experienceVersion", RigelPaidEntitlementService.EXPERIENCE_VERSION)
                .containsEntry(
                        "accessReferenceHash",
                        RigelCommercialEventContract.accessReferenceHash("Bearer-Com-Caixa"))
                .doesNotContainKey("accessToken");
        assertThat(String.valueOf(metadata.get("accessReferenceHash")))
                .hasSize(64)
                .doesNotContain("Bearer-Com-Caixa");
    }

    /** Aceita somente a compra com preço, moeda, experimento e identificadores canônicos. */
    @Test
    void acceptsExactRigelPurchaseContract() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productSlug", RigelPaidEntitlementService.PRODUCT_SLUG);
        metadata.put("experimentId", RigelCommercialEventContract.EXPERIMENT_ID);
        metadata.put("paymentId", "mp-rigel-276");
        metadata.put("amountBrl", new BigDecimal("349.00"));
        metadata.put("currency", "BRL");
        metadata.put("approvedAt", "2026-08-30T15:00:00Z");
        FunnelEventRequest request = event("PURCHASE_COMPLETED", null, metadata);

        RigelCommercialEventContract.requireComplete(request, "PURCHASE_COMPLETED");
    }

    /** Rejeita aliases legados para impedir aprovação comercial sem correlação verificável. */
    @Test
    void rejectsLegacyAliasesAndRawBearer() {
        Map<String, Object> aliases = new LinkedHashMap<>();
        aliases.put("productSlug", RigelPaidEntitlementService.PRODUCT_SLUG);
        aliases.put("experimentId", RigelCommercialEventContract.EXPERIMENT_ID);
        aliases.put("mercadoPagoPaymentId", "mp-rigel-276");
        aliases.put("amount", new BigDecimal("349.00"));
        aliases.put("currency", "BRL");
        aliases.put("approvedAt", "2026-08-30T15:00:00Z");
        assertThrows(
                IllegalArgumentException.class,
                () -> RigelCommercialEventContract.requireComplete(
                        event("PURCHASE_COMPLETED", null, aliases), "PURCHASE_COMPLETED"));

        Map<String, Object> rawBearer = new LinkedHashMap<>(aliases);
        rawBearer.put("paymentId", "mp-rigel-276");
        rawBearer.put("amountBrl", new BigDecimal("349.00"));
        rawBearer.put("accessToken", "bearer-nao-pode-persistir");
        assertThrows(
                IllegalArgumentException.class,
                () -> RigelCommercialEventContract.requireComplete(
                        event("PURCHASE_COMPLETED", null, rawBearer), "PURCHASE_COMPLETED"));
    }

    /** Monta uma requisição comercial mínima para exercitar o contrato determinístico. */
    private FunnelEventRequest event(String eventType, String accessToken, Map<String, Object> metadata) {
        return new FunnelEventRequest(
                RigelPaidEntitlementService.PRODUCT_SLUG,
                eventType,
                accessToken,
                "compradora@sandbox.local",
                RigelPaidEntitlementService.PAID_SOURCE,
                "contract-test",
                null,
                metadata);
    }
}
