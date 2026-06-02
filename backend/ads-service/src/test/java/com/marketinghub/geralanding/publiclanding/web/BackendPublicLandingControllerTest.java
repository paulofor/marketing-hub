package com.marketinghub.geralanding.publiclanding.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.geralanding.publiclanding.service.BackendPublicLandingService;
import com.marketinghub.geralanding.publiclanding.service.approveEndPublish.PublicLandingPublicationResponse;
import org.junit.jupiter.api.Test;

/** Valida o contrato HTTP do controller de publicação da landing pública do GeraLanding. */
class BackendPublicLandingControllerTest {

    /** Deve delegar a aprovação e publicação para o service de landing pública. */
    @Test
    void startShouldDelegateApproveEndPublishToService() {
        BackendPublicLandingService service = mock(BackendPublicLandingService.class);
        BackendPublicLandingController controller = new BackendPublicLandingController(service);
        PublicLandingPublicationResponse publication = new PublicLandingPublicationResponse(
                35L,
                null,
                "http://lead/api/public/flows/exp-35-landing-geralanding",
                "http://lead/api/flows/exp-35-landing-geralanding/page",
                "ok");
        when(service.start(35L)).thenReturn(publication);

        var response = controller.start(35L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(publication, response.getBody());
        verify(service).start(35L);
    }
}
