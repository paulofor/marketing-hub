package com.marketinghub.experimentstrategist.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: concentrar limites e destino S3 da memoria do Estrategista. */
@Component
@ConfigurationProperties(prefix = "experiment-strategist.memory")
public class ExperimentStrategistMemoryProperties {
  private String bucket = "";
  private String region = "us-east-1";
  private String endpoint = "";
  private String prefix = "experiment-strategist-memory/v1";
  private long maxBytes = 2 * 1024 * 1024;
  private int retentionDays = 365;

  /** Retorna o bucket privado. */
  public String getBucket() {
    return bucket;
  }

  /** Define o bucket privado. */
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  /** Retorna a regiao AWS. */
  public String getRegion() {
    return region;
  }

  /** Define a regiao AWS. */
  public void setRegion(String region) {
    this.region = region;
  }

  /** Retorna o endpoint S3 compativel opcional. */
  public String getEndpoint() {
    return endpoint;
  }

  /** Define o endpoint S3 compativel opcional. */
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  /** Retorna o prefixo isolado. */
  public String getPrefix() {
    return prefix;
  }

  /** Define o prefixo isolado. */
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  /** Retorna o tamanho maximo do texto anonimizado. */
  public long getMaxBytes() {
    return maxBytes;
  }

  /** Define o tamanho maximo do texto anonimizado. */
  public void setMaxBytes(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  /** Retorna a retencao em dias. */
  public int getRetentionDays() {
    return retentionDays;
  }

  /** Define a retencao em dias. */
  public void setRetentionDays(int retentionDays) {
    this.retentionDays = retentionDays;
  }
}
