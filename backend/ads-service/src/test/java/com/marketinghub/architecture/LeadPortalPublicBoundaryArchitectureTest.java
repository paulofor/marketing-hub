package com.marketinghub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Garante que o backend principal não volte a expor bordas públicas do Lead Portal. */
@AnalyzeClasses(packages = "com.marketinghub")
class LeadPortalPublicBoundaryArchitectureTest {

    /** Bloqueia controllers de cliente/lead no pacote Lead Portal do ads-service. */
    @ArchTest
    static final ArchRule leadPortalPublicEndpointsMustStayOutOfAdsService = classes()
            .that()
            .resideInAPackage("com.marketinghub.leadportal.web..")
            .and()
            .areAnnotatedWith(RestController.class)
            .should(notExposeLeadPortalCustomerRequestMapping())
            .because("[ARQUITETURA] endpoints públicos de lead/cliente pertencem ao lead-portal ou "
                    + "lead-portal-payments-service; o ads-service deve manter somente contratos administrativos "
                    + "ou callbacks internos");

    /** Cria a condição ArchUnit que inspeciona o @RequestMapping da classe. */
    private static ArchCondition<JavaClass> notExposeLeadPortalCustomerRequestMapping() {
        return new ArchCondition<>("[ARQUITETURA] não expor borda de cliente do Lead Portal no ads-service") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                RequestMapping requestMapping = item.reflect().getAnnotation(RequestMapping.class);
                if (requestMapping == null) {
                    return;
                }
                if (containsLeadPortalCustomerMapping(requestMapping.value())
                        || containsLeadPortalCustomerMapping(requestMapping.path())) {
                    events.add(SimpleConditionEvent.violated(item,
                            "[ARQUITETURA] [LeadPortal] " + item.getName()
                                    + " expõe endpoint de lead/cliente no ads-service; mova a borda para "
                                    + "lead-portal ou lead-portal-payments-service e mantenha no backend principal "
                                    + "apenas callback interno."));
                }
            }
        };
    }

    /** Verifica se algum mapping aponta para a borda de cliente do Lead Portal. */
    private static boolean containsLeadPortalCustomerMapping(String[] mappings) {
        for (String mapping : mappings) {
            if (mapping != null
                    && (mapping.startsWith("/api/public/lead-portal") || mapping.equals("/api/flows"))) {
                return true;
            }
        }
        return false;
    }
}
