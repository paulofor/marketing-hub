package com.marketinghub.microservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketinghub.microservice.dto.DeploymentWorkflowInventoryDto;
import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MicroserviceDiscoveryServiceTest {

  @Test
  void shouldDiscoverServicesFromComposeFile() throws IOException {
    Path composeFile = Files.createTempFile("compose", ".yml");
    Files.writeString(
        composeFile,
        """
                version: '3.9'
                services:
                  api-service:
                    image: sample/api:latest
                    ports:
                      - "8081:8080"
                  worker:
                    image: sample/worker:latest
                    ports:
                      - "9090"
                """);

    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            composeFile.toString(), "non-existent-workflows", "/healthz");

    List<DiscoveredMicroserviceDto> discovered = service.discoverFromCompose();

    assertEquals(2, discovered.size());
    DiscoveredMicroserviceDto apiService =
        discovered.stream()
            .filter(dto -> dto.serviceName().equals("api-service"))
            .findFirst()
            .orElseThrow();

    assertEquals("http://localhost:8081", apiService.baseUrl());
    assertEquals("/healthz", apiService.healthCheckPath());
    assertEquals(8081, apiService.hostPort());
    assertEquals(8080, apiService.containerPort());

    DiscoveredMicroserviceDto worker =
        discovered.stream()
            .filter(dto -> dto.serviceName().equals("worker"))
            .findFirst()
            .orElseThrow();

    assertEquals("http://worker:9090", worker.baseUrl());
    assertEquals("/healthz", worker.healthCheckPath());
    assertEquals(9090, worker.hostPort());
    assertEquals(9090, worker.containerPort());

    Files.deleteIfExists(composeFile);
  }

  @Test
  void shouldReturnEmptyListWhenFileDoesNotExist() {
    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            "non-existent-compose.yml", "non-existent-workflows", "/health");

    List<DiscoveredMicroserviceDto> discovered = service.discoverFromCompose();

    assertTrue(discovered.isEmpty());
  }

  @Test
  void shouldDiscoverDeploymentInventoryFromWorkflows() throws IOException {
    Path workflowsPath = Files.createTempDirectory("workflows");
    Path workflowFile = workflowsPath.resolve("lead-portal-ci.yml");
    Files.writeString(
        workflowFile,
        """
                name: CI - Lead Portal
                on:
                  push:
                    branches: [ main ]
                  workflow_dispatch:
                jobs:
                  deploy:
                    if: github.ref == 'refs/heads/main' && github.event_name == 'workflow_dispatch'
                    runs-on: ubuntu-latest
                    env:
                      DEPLOY_HOST: 191.252.120.96
                      DEPLOY_USER: root
                      REMOTE_PATH: ${{ secrets.LEAD_PORTAL_REMOTE_PATH || '/root/lead-portal' }}
                    steps:
                      - name: Configure SSH
                        env:
                          VPS_SSH_KEY: ${{ secrets.VPS_SSH_CHAVE != '' && secrets.VPS_SSH_CHAVE || secrets.SSH_PRIVATE_KEY }}
                        run: echo ok
                """);

    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            "non-existent-compose.yml", workflowsPath.toString(), "/health");

    List<DeploymentWorkflowInventoryDto> deployments = service.discoverDeploymentsFromWorkflows();

    assertEquals(1, deployments.size());
    DeploymentWorkflowInventoryDto deployment = deployments.getFirst();
    assertEquals("lead-portal-ci.yml", deployment.workflowFile());
    assertEquals("CI - Lead Portal", deployment.workflowName());
    assertEquals("deploy", deployment.jobName());
    assertEquals("191.252.120.96", deployment.deployHost());
    assertEquals("root", deployment.deployUser());
    assertEquals(
        "${{ secrets.LEAD_PORTAL_REMOTE_PATH || '/root/lead-portal' }}", deployment.remotePath());
    assertTrue(deployment.secretReferences().contains("VPS_SSH_CHAVE"));
    assertTrue(deployment.secretReferences().contains("SSH_PRIVATE_KEY"));
    assertEquals("manual", deployment.triggerMode());
  }
}
