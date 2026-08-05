package com.marketinghub.microservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.marketinghub.microservice.VpsHostInventory;
import com.marketinghub.microservice.dto.DeploymentWorkflowInventoryDto;
import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import com.marketinghub.microservice.dto.UpdateVpsHostInventoryRequest;
import com.marketinghub.repository.jpa.microservice.VpsHostInventoryRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a descoberta de microserviços e inventário de deploy. */
@ExtendWith(MockitoExtension.class)
class MicroserviceDiscoveryServiceTest {
  @Mock private VpsHostInventoryRepository hostInventoryRepository;

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
            composeFile.toString(), "non-existent-workflows", "/healthz", hostInventoryRepository);

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
            "non-existent-compose.yml",
            "non-existent-workflows",
            "/health",
            hostInventoryRepository);

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
            "non-existent-compose.yml",
            workflowsPath.toString(),
            "/health",
            hostInventoryRepository);

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

  /** Deve usar o inventário fallback quando os workflows não estão disponíveis. */
  @Test
  void shouldUseFallbackDeploymentInventoryWhenWorkflowsAreUnavailable() {
    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            "non-existent-compose.yml",
            "non-existent-workflows",
            "/health",
            hostInventoryRepository);

    List<DeploymentWorkflowInventoryDto> deployments = service.discoverDeploymentsFromWorkflows();

    assertEquals(7, deployments.size());
    assertTrue(deployments.stream().anyMatch(dto -> dto.deployHost().equals("191.252.181.168")));
    assertTrue(deployments.stream().anyMatch(dto -> dto.deployHost().equals("177.153.62.107")));
    assertTrue(deployments.stream().anyMatch(dto -> dto.deployHost().equals("191.252.120.96")));
    assertTrue(deployments.stream().anyMatch(dto -> dto.deployHost().equals("191.252.210.83")));
    assertTrue(deployments.stream().anyMatch(dto -> dto.deployHost().equals("191.252.102.54")));
    assertTrue(deployments.stream().anyMatch(dto -> dto.deployHost().equals("163.245.200.7")));
  }

  /** Deve expor o cadastro físico e financeiro dos hosts VPS no inventário operacional. */
  @Test
  void shouldExposeFallbackVpsHostsInOperationalInventory() {
    when(hostInventoryRepository.findAllByOrderByHostAsc()).thenReturn(List.of());
    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            "non-existent-compose.yml",
            "non-existent-workflows",
            "/health",
            hostInventoryRepository);

    var inventory = service.discoverOperationalInventory();

    assertEquals(6, inventory.hosts().size());
    assertTrue(
        inventory.hosts().stream()
            .anyMatch(
                host ->
                    host.host().equals("191.252.102.54")
                        && host.providerName().equals("Locaweb Serviços de Internet S/A")
                        && host.physicalSpecsEvidence().contains("Pendente")));
    assertTrue(
        inventory.hosts().stream()
            .anyMatch(
                host ->
                    host.host().equals("191.252.210.83")
                        && host.notes().contains("docker_ops confirmou")));
  }

  /** Deve priorizar dados editados no banco sem perder hosts do inventário versionado. */
  @Test
  void shouldMergeEditableVpsHostsWithFallbackInventory() {
    VpsHostInventory edited = new VpsHostInventory();
    edited.setHost("191.252.210.83");
    edited.setProviderName("Provedor confirmado");
    edited.setCpu("4 vCPU");
    edited.setMemoryGb(8);
    edited.setMonthlyCostBrl(new BigDecimal("149.90"));
    when(hostInventoryRepository.findAllByOrderByHostAsc()).thenReturn(List.of(edited));
    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            "non-existent-compose.yml",
            "non-existent-workflows",
            "/health",
            hostInventoryRepository);

    var inventory = service.discoverOperationalInventory();

    assertEquals(6, inventory.hosts().size());
    assertTrue(
        inventory.hosts().stream()
            .anyMatch(
                host ->
                    host.host().equals("191.252.210.83")
                        && host.providerName().equals("Provedor confirmado")
                        && host.memoryGb().equals(8)));
  }

  /** Deve criar cadastro editável usando o fallback como base quando o host ainda não existe. */
  @Test
  void shouldUpdateFallbackHostIntoEditableInventory() {
    when(hostInventoryRepository.findByHost("191.252.102.54")).thenReturn(Optional.empty());
    when(hostInventoryRepository.save(org.mockito.ArgumentMatchers.any(VpsHostInventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    MicroserviceDiscoveryService service =
        new MicroserviceDiscoveryService(
            "non-existent-compose.yml",
            "non-existent-workflows",
            "/health",
            hostInventoryRepository);

    var updated =
        service.updateHostInventory(
            "191.252.102.54",
            new UpdateVpsHostInventoryRequest(
                "Locaweb",
                "Painel do provedor",
                "2 vCPU",
                4,
                80,
                "Ubuntu 24.04",
                new BigDecimal("99.90"),
                "mensal",
                "Fatura julho",
                "MCP vps_host_inventory",
                "Host comercial"));

    assertEquals("191.252.102.54", updated.host());
    assertEquals("2 vCPU", updated.cpu());
    assertEquals(4, updated.memoryGb());
    assertEquals(new BigDecimal("99.90"), updated.monthlyCostBrl());
  }
}
