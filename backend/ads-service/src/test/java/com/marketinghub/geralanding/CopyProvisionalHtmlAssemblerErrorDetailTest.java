package com.marketinghub.geralanding;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.copy.provisorio.CopyProvisionalHtmlAssembler;
import com.marketinghub.geralanding.copy.provisorio.CopyProvisionalHtmlPayloadResolver;
import com.marketinghub.geralanding.copy.provisorio.CopyProvisionalHtmlProcessor;
import org.junit.jupiter.api.Test;

class CopyProvisionalHtmlAssemblerErrorDetailTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final CopyProvisionalHtmlAssembler assembler =
      new CopyProvisionalHtmlAssembler(
          new CopyProvisionalHtmlPayloadResolver(objectMapper),
          new CopyProvisionalHtmlProcessor(),
          objectMapper);

  @Test
  void shouldIncludeDetailedContextWhenAssemblyFails() {
    String invalidCopyJson = "{invalid-json";
    String validWireframeJson =
        "{" + "\"landingPageWireframe\":{\"pagina\":{\"corpo\":{\"secoes\":[]}}}" + "}";

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> assembler.assemble(invalidCopyJson, validWireframeJson, "job-copy-erro-1"));

    assertTrue(exception.getMessage().contains("jobId=job-copy-erro-1"));
    assertTrue(exception.getMessage().contains("copyLength="));
    assertTrue(exception.getMessage().contains("wireframeLength="));
    assertTrue(exception.getMessage().contains("errorDetails="));
  }
}
