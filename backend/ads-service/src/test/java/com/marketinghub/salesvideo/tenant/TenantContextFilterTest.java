package com.marketinghub.salesvideo.tenant;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextFilterTest {

    private TenantContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantContextFilter();
    }

    @Test
    void shouldSkipTenantValidationForOptionsPreflightRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/products/1/sales-videos/profiles");
        request.addHeader("Origin", "http://191.252.181.168:5173");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Mockito.verify(chain).doFilter(Mockito.any(), Mockito.any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRejectTenantProtectedEndpointsWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/1/sales-videos/profiles");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("TENANT_HEADER_REQUIRED");
    }

    /** Deve manter leitura legado de perfil sem tenant para o renderizador externo atual. */
    @Test
    void shouldAllowReadOnlyProfileCompatibilityWithoutTenantHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sales-videos/profiles/3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Mockito.verify(chain).doFilter(Mockito.any(), Mockito.any());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.getContext().systemRequest()).isTrue();
    }
}
