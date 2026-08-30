package com.marketinghub.pde.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.marketinghub.pde.service.MercadoPagoEntitlementAuthorizer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Valida que a autenticação financeira antecede qualquer leitura do payload do Rigel. */
class MercadoPagoEntitlementAuthenticationFilterTest {

    /** Rejeita até um corpo inválido antes que o Spring alcance desserialização ou validação. */
    @Test
    void rejectsMissingCredentialBeforeRequestBodyValidation() throws Exception {
        MercadoPagoEntitlementAuthenticationFilter filter = filter();
        MockHttpServletRequest request = request();
        request.setContent("{}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentAsString()).contains("não autorizado");
        verifyNoInteractions(chain);
    }

    /** Libera o payload para o controller somente quando o segredo Bearer é exato. */
    @Test
    void continuesWithExactCredential() throws Exception {
        MercadoPagoEntitlementAuthenticationFilter filter = filter();
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer payment-test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /** Mantém endpoints alheios fora da responsabilidade do filtro financeiro. */
    @Test
    void ignoresUnrelatedEndpoint() throws Exception {
        MercadoPagoEntitlementAuthenticationFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    /** Cria o filtro com uma credencial conhecida e isolada. */
    private MercadoPagoEntitlementAuthenticationFilter filter() {
        return new MercadoPagoEntitlementAuthenticationFilter(
                new MercadoPagoEntitlementAuthorizer("payment-test-secret"));
    }

    /** Cria a requisição exata cuja validação precisa ocorrer depois da autenticação. */
    private MockHttpServletRequest request() {
        return new MockHttpServletRequest(
                "POST", "/api/internal/pde/mercado-pago/entitlements");
    }
}
