package com.marketinghub.pde.infrastructure.service;

import com.marketinghub.pde.infrastructure.PdeVpsServer;
import com.marketinghub.pde.infrastructure.PdeVpsStatus;
import com.marketinghub.pde.infrastructure.service.listVps.PdeVpsServerResponse;
import com.marketinghub.pde.infrastructure.service.listVps.PdeVpsSummaryResponse;
import com.marketinghub.pde.infrastructure.service.saveVps.SavePdeVpsServerRequest;
import com.marketinghub.repository.jpa.pde.PdeVpsServerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar VPS de PDE e consolidar custo fixo de infraestrutura. */
@Service
public class PdeVpsInfrastructureService {

  private static final Set<PdeVpsStatus> COST_ACTIVE_STATUSES =
      EnumSet.of(PdeVpsStatus.ACTIVE, PdeVpsStatus.STAGING);

  private final PdeVpsServerRepository repository;

  /** Inicializa o serviço com o repositório canônico de VPS dos PDEs. */
  public PdeVpsInfrastructureService(PdeVpsServerRepository repository) {
    this.repository = repository;
  }

  /** Lista as VPS cadastradas e o custo mensal ativo consolidado. */
  @Transactional(readOnly = true)
  public PdeVpsSummaryResponse listServers() {
    List<PdeVpsServerResponse> servers =
        repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    BigDecimal totalMonthlyCost =
        servers.stream()
            .filter((server) -> COST_ACTIVE_STATUSES.contains(server.status()))
            .map(PdeVpsServerResponse::monthlyCostBrl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    int activeServers =
        (int)
            servers.stream()
                .filter((server) -> COST_ACTIVE_STATUSES.contains(server.status()))
                .count();
    return new PdeVpsSummaryResponse(totalMonthlyCost, servers.size(), activeServers, servers);
  }

  /** Cria uma VPS nova para gestão de infraestrutura e custo fixo de PDE. */
  @Transactional
  public PdeVpsServerResponse createServer(SavePdeVpsServerRequest request) {
    PdeVpsServer server = new PdeVpsServer();
    applyRequest(server, request);
    return toResponse(repository.save(server));
  }

  /** Atualiza uma VPS existente mantendo seu identificador operacional. */
  @Transactional
  public PdeVpsServerResponse updateServer(Long id, SavePdeVpsServerRequest request) {
    PdeVpsServer server =
        repository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VPS PDE não encontrada"));
    applyRequest(server, request);
    return toResponse(repository.save(server));
  }

  /** Remove o cadastro de uma VPS que não deve mais compor a operação dos PDEs. */
  @Transactional
  public void deleteServer(Long id) {
    if (!repository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VPS PDE não encontrada");
    }
    repository.deleteById(id);
  }

  /** Soma o custo mensal de VPS ativas vinculado ao produto informado. */
  @Transactional(readOnly = true)
  public BigDecimal monthlyCostForProduct(String productSlug) {
    if (!StringUtils.hasText(productSlug)) {
      return BigDecimal.ZERO;
    }
    return repository
        .findByProductSlugAndStatusIn(productSlug.trim(), COST_ACTIVE_STATUSES)
        .stream()
        .map(PdeVpsServer::getMonthlyCostBrl)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Aplica e normaliza os dados enviados pela tela administrativa. */
  private void applyRequest(PdeVpsServer server, SavePdeVpsServerRequest request) {
    server.setName(normalizeRequired(request.name(), "Nome da VPS obrigatório"));
    server.setProvider(normalizeRequired(request.provider(), "Provedor da VPS obrigatório"));
    server.setIpAddress(normalizeRequired(request.ipAddress(), "IP da VPS obrigatório"));
    server.setPlanName(normalizeOptional(request.planName()));
    server.setRegion(normalizeOptional(request.region()));
    server.setVcpuCount(request.vcpuCount());
    server.setRamGb(request.ramGb());
    server.setStorageGb(request.storageGb());
    server.setMonthlyCostBrl(normalizeMonthlyCost(request.monthlyCostBrl()));
    server.setProductSlug(normalizeOptionalLower(request.productSlug()));
    server.setEnvironment(
        StringUtils.hasText(request.environment()) ? request.environment().trim() : "production");
    server.setDomains(normalizeOptional(request.domains()));
    server.setStatus(request.status() != null ? request.status() : PdeVpsStatus.PLANNED);
    server.setNotes(normalizeOptional(request.notes()));
  }

  /** Converte a entidade persistida em contrato administrativo. */
  private PdeVpsServerResponse toResponse(PdeVpsServer server) {
    return new PdeVpsServerResponse(
        server.getId(),
        server.getName(),
        server.getProvider(),
        server.getIpAddress(),
        server.getPlanName(),
        server.getRegion(),
        server.getVcpuCount(),
        server.getRamGb(),
        server.getStorageGb(),
        server.getMonthlyCostBrl(),
        server.getProductSlug(),
        server.getEnvironment(),
        server.getDomains(),
        server.getStatus(),
        server.getNotes(),
        server.getCreatedAt(),
        server.getUpdatedAt());
  }

  /** Normaliza texto obrigatório e devolve erro de contrato quando ele estiver vazio. */
  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return value.trim();
  }

  /** Normaliza o custo mensal e bloqueia valores negativos que distorcem margem. */
  private BigDecimal normalizeMonthlyCost(BigDecimal value) {
    BigDecimal cost = value != null ? value : BigDecimal.ZERO;
    if (cost.signum() < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Custo mensal da VPS não pode ser negativo");
    }
    return cost.setScale(2, RoundingMode.HALF_UP);
  }

  /** Normaliza texto opcional removendo espaços externos e mantendo nulo quando vazio. */
  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Normaliza slug opcional em caixa baixa para facilitar vinculo com produtos. */
  private String normalizeOptionalLower(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
  }
}
