package com.marketinghub.experimental.cmspages;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CmsPagesClientTest {

    @Test
    void createsLandingPage() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(template);
        CmsPagesClient client = new CmsPagesClient(template, "http://localhost:8080");

        server.expect(requestTo("http://localhost:8080/pages"))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withSuccess());

        client.createLandingPage(new LandingPage("Title", "<p>Body</p>"));

        server.verify();
    }
}
