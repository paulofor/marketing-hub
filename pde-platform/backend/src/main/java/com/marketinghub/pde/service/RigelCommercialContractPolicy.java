package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse.DeliveryContractDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.DeliverySectionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.MissionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.ServiceScopeDto;
import java.util.List;
import java.util.Map;

/**
 * Responsabilidade: impedir que o contrato produtivo do Rigel libere uma entrega menor que o
 * escopo comercial congelado.
 */
final class RigelCommercialContractPolicy {
    private static final String PRODUCT_SLUG = "kit-whatsapp-pronto";
    private static final String EXPERIENCE_VERSION = "kit-whatsapp-pronto-pde-v2";
    private static final String FULL_DELIVERY_MISSION = "entrega-completa-48h";
    private static final Map<String, Integer> COMMERCIAL_COUNTS = Map.of(
            "responses", 15,
            "qualificationQuestions", 8,
            "followUps", 4);
    private static final List<String> CANONICAL_INCLUDED_ITEMS = List.of(
            "Briefing inicial guiado",
            "15 respostas personalizadas",
            "8 perguntas de qualificação",
            "4 follow-ups manuais",
            "Regras de escalonamento",
            "Guia, checklist, revisão humana e entrega",
            "Acesso aos materiais sem expiração enquanto o pagamento permanecer vigente");

    private RigelCommercialContractPolicy() {}

    /** Reforça o piso comercial somente na versão exata do Rigel e preserva os demais produtos. */
    static ProductExperienceResponse enforce(ProductExperienceResponse product) {
        if (product == null
                || !PRODUCT_SLUG.equals(product.slug())
                || !EXPERIENCE_VERSION.equals(product.experienceVersion())) {
            return product;
        }
        if (product.missions() == null || product.serviceScope() == null) {
            throw new IllegalArgumentException("Contrato comercial do Rigel sem jornada ou escopo pago");
        }
        boolean hasFullDelivery = product.missions().stream()
                .anyMatch(mission -> mission != null && FULL_DELIVERY_MISSION.equals(mission.id()));
        if (!hasFullDelivery) {
            throw new IllegalArgumentException("Contrato comercial do Rigel sem entrega completa estruturada");
        }
        List<MissionDto> missions = product.missions().stream()
                .map(RigelCommercialContractPolicy::enforceMission)
                .toList();
        ServiceScopeDto serviceScope = new ServiceScopeDto(
                CANONICAL_INCLUDED_ITEMS,
                product.serviceScope().excludedItems(),
                product.serviceScope().deadlineStartsWhen());
        return copyWithCommercialFloor(product, missions, serviceScope);
    }

    /** Eleva os mínimos da entrega completa sem reduzir máximos ou alterar outras missões. */
    private static MissionDto enforceMission(MissionDto mission) {
        if (mission == null || !FULL_DELIVERY_MISSION.equals(mission.id())) {
            return mission;
        }
        DeliveryContractDto contract = mission.deliveryContract();
        if (contract == null || contract.sections() == null) {
            throw new IllegalArgumentException("Entrega completa do Rigel sem seções verificáveis");
        }
        List<DeliverySectionDto> sections = contract.sections().stream()
                .map(RigelCommercialContractPolicy::enforceSection)
                .toList();
        for (String requiredSection : COMMERCIAL_COUNTS.keySet()) {
            if (sections.stream().noneMatch(section -> requiredSection.equals(section.id()))) {
                throw new IllegalArgumentException("Entrega completa do Rigel sem seção " + requiredSection);
            }
        }
        return new MissionDto(
                mission.id(),
                mission.day(),
                mission.title(),
                mission.principle(),
                mission.action(),
                mission.evidence(),
                mission.visualCue(),
                mission.completionRole(),
                new DeliveryContractDto(sections),
                mission.interaction());
    }

    /** Exige a quantidade comercial exata e falha fechado quando o contrato não a comporta. */
    private static DeliverySectionDto enforceSection(DeliverySectionDto section) {
        if (section == null) {
            throw new IllegalArgumentException("Entrega completa do Rigel contém seção nula");
        }
        Integer exactCount = COMMERCIAL_COUNTS.get(section.id());
        if (exactCount == null) {
            return section;
        }
        if (section.maxItems() < exactCount) {
            throw new IllegalArgumentException("Seção " + section.id() + " não comporta a quantidade comercial");
        }
        return new DeliverySectionDto(section.id(), section.title(), exactCount, exactCount);
    }

    /** Reconstrói o record preservando todo o contrato que não pertence ao piso comercial. */
    private static ProductExperienceResponse copyWithCommercialFloor(
            ProductExperienceResponse product,
            List<MissionDto> missions,
            ServiceScopeDto serviceScope) {
        return new ProductExperienceResponse(
                product.slug(),
                product.experienceVersion(),
                product.layoutKey(),
                product.funnelVersion(),
                product.name(),
                product.promise(),
                product.audience(),
                product.priceLabel(),
                product.theme(),
                product.diagnostic(),
                missions,
                product.supportMaterials(),
                product.heroVideos(),
                product.publicDiagnosticQuestions(),
                product.publicFirstFold(),
                product.scientificEvidencePack(),
                product.completionOffer(),
                serviceScope,
                product.publicProofs(),
                product.commercialProcess(),
                product.commercialBinding());
    }
}
