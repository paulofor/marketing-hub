package com.marketinghub.microservice.dto;

import java.time.Instant;
import lombok.Data;

/** Data transfer object for {@link com.marketinghub.microservice.Microservice}. */
@Data
public class MicroserviceDto {
  private Long id;
  private String name;
  private String description;
  private String baseUrl;
  private String category;
  private String status;
  private String owner;
  private String documentationUrl;
  private String healthCheckPath;
  private Instant createdAt;
  private Instant updatedAt;

  private Instant lastExceptionAt;
  private String lastExceptionMessage;
  private String lastExceptionSeverity;
  private Long exceptionCount;
}
