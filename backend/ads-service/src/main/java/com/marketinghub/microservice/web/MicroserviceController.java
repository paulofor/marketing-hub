package com.marketinghub.microservice.web;

import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.dto.CreateMicroserviceRequest;
import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import com.marketinghub.microservice.dto.MicroserviceDto;
import com.marketinghub.microservice.dto.OperationalInventoryDto;
import com.marketinghub.microservice.dto.UpdateVpsHostInventoryRequest;
import com.marketinghub.microservice.dto.VpsHostInventoryDto;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionSummary;
import com.marketinghub.microservice.exception.service.MicroserviceExceptionService;
import com.marketinghub.microservice.mapper.MicroserviceMapper;
import com.marketinghub.microservice.service.MicroserviceDiscoveryService;
import com.marketinghub.microservice.service.MicroserviceService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o cadastro e o inventário operacional de microserviços. */
@RestController
@RequestMapping("/api/microservices")
public class MicroserviceController {
  private final MicroserviceService service;
  private final MicroserviceMapper mapper;
  private final MicroserviceExceptionService exceptionService;
  private final MicroserviceDiscoveryService discoveryService;

  public MicroserviceController(
      MicroserviceService service,
      MicroserviceMapper mapper,
      MicroserviceExceptionService exceptionService,
      MicroserviceDiscoveryService discoveryService) {
    this.service = service;
    this.mapper = mapper;
    this.exceptionService = exceptionService;
    this.discoveryService = discoveryService;
  }

  /** Cria um microserviço no cadastro administrativo. */
  @PostMapping
  public MicroserviceDto create(@RequestBody CreateMicroserviceRequest request) {
    Microservice created = service.create(request);
    MicroserviceExceptionSummary summary =
        exceptionService.summarizeByMicroservices(List.of(created)).get(created.getId());
    return mapper.toDto(created, summary);
  }

  /** Lista os microserviços cadastrados com resumo de falhas recentes. */
  @GetMapping
  public List<MicroserviceDto> list() {
    List<Microservice> microservices = service.list();
    Map<Long, MicroserviceExceptionSummary> summaries =
        exceptionService.summarizeByMicroservices(microservices);
    return microservices.stream().map(ms -> mapper.toDto(ms, summaries.get(ms.getId()))).toList();
  }

  /** Busca um microserviço cadastrado por identificador. */
  @GetMapping("/{id}")
  public MicroserviceDto get(@PathVariable Long id) {
    Microservice microservice = service.get(id);
    MicroserviceExceptionSummary summary =
        exceptionService.summarizeByMicroservices(List.of(microservice)).get(microservice.getId());
    return mapper.toDto(microservice, summary);
  }

  /** Lista serviços descobertos no docker-compose configurado. */
  @GetMapping("/discover")
  public List<DiscoveredMicroserviceDto> discoverFromCompose() {
    return discoveryService.discoverFromCompose();
  }

  /** Lista o inventário versionado de portas, hosts e secrets de deploy. */
  @GetMapping("/operational-inventory")
  public OperationalInventoryDto operationalInventory() {
    return discoveryService.discoverOperationalInventory();
  }

  /** Busca os dados editáveis de um host VPS. */
  @GetMapping("/operational-inventory/hosts/{host}")
  public VpsHostInventoryDto getVpsHost(@PathVariable String host) {
    return discoveryService.getHostInventory(host);
  }

  /** Atualiza os dados físicos, financeiros e operacionais de um host VPS. */
  @PutMapping("/operational-inventory/hosts/{host}")
  public VpsHostInventoryDto updateVpsHost(
      @PathVariable String host, @RequestBody UpdateVpsHostInventoryRequest request) {
    return discoveryService.updateHostInventory(host, request);
  }

  /** Atualiza um microserviço cadastrado. */
  @PutMapping("/{id}")
  public MicroserviceDto update(
      @PathVariable Long id, @RequestBody CreateMicroserviceRequest request) {
    Microservice microservice = service.update(id, request);
    MicroserviceExceptionSummary summary =
        exceptionService.summarizeByMicroservices(List.of(microservice)).get(microservice.getId());
    return mapper.toDto(microservice, summary);
  }

  /** Remove um microserviço do cadastro administrativo. */
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
