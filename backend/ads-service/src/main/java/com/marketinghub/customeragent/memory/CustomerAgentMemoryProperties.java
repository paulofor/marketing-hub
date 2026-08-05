package com.marketinghub.customeragent.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: concentrar limites e destino S3 da memoria pesada do Agente Cliente. */
@Component
@ConfigurationProperties(prefix = "customer-agent.memory")
public class CustomerAgentMemoryProperties {
  private String bucket = "";
  private String region = "us-east-1";
  private String endpoint = "";
  private String prefix = "customer-agent-memory/v1";
  private long maxBytes = 25 * 1024 * 1024;
  private int retentionDays = 365;

  /** Retorna o bucket privado dedicado. */
  public String getBucket() {
    return bucket;
  }

  /** Define o bucket privado dedicado. */
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

  /** Retorna endpoint S3 compativel opcional. */
  public String getEndpoint() {
    return endpoint;
  }

  /** Define endpoint S3 compativel opcional. */
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  /** Retorna o prefixo isolado dos objetos. */
  public String getPrefix() {
    return prefix;
  }

  /** Define o prefixo isolado dos objetos. */
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  /** Retorna o tamanho maximo aceito. */
  public long getMaxBytes() {
    return maxBytes;
  }

  /** Define o tamanho maximo aceito. */
  public void setMaxBytes(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  /** Retorna a retencao operacional em dias. */
  public int getRetentionDays() {
    return retentionDays;
  }

  /** Define a retencao operacional em dias. */
  public void setRetentionDays(int retentionDays) {
    this.retentionDays = retentionDays;
  }
}
