package com.marketinghub.pde.service;

import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar versões produtivas PDE vinculadas ao produto. */
@Service
public class PdeProductionSlotService {

    private static final String DEFAULT_PDE_PRODUCT_SLUG = "metodo-musa-7-dias";

    private final PdeProductionSlotRepository repository;

    /** Inicializa o serviço com o repositório canônico de slots PDE. */
    public PdeProductionSlotService(PdeProductionSlotRepository repository) {
        this.repository = repository;
    }

    /** Resolve o produto PDE padrão quando a tela não informa um slug específico. */
    public String resolveProductSlug(String productSlug) {
        return StringUtils.hasText(productSlug) ? productSlug.trim() : DEFAULT_PDE_PRODUCT_SLUG;
    }

    /** Lista os slots produtivos persistidos para o produto PDE informado. */
    public List<PostDeployPdeProductionSlotDto> listProductionSlotsForProduct(String productSlug) {
        return repository.findByProductSlugOrderBySlotCodeAsc(resolveProductSlug(productSlug)).stream()
                .map(this::toProductionSlotDto)
                .toList();
    }

    /** Cria ou atualiza um slot produtivo versionado para manter hipóteses PDE em URLs paralelas. */
    public PostDeployPdeProductionSlotDto saveProductionSlot(
            String productSlug,
            Long defaultSourceExperimentId,
            PostDeployPdeProductionSlotRequestDto request) {
        String resolvedProductSlug = resolveProductSlug(productSlug);
        String slotCode = normalizeRequired(request.slotCode(), "Código do slot PDE obrigatório").toLowerCase(Locale.ROOT);
        String domain = normalizeDomain(request.domain());
        String publicUrl = StringUtils.hasText(request.publicUrl())
                ? request.publicUrl().trim()
                : "https://" + domain;
        PdeProductionSlot slot = repository.findByProductSlugAndSlotCode(resolvedProductSlug, slotCode)
                .orElseGet(PdeProductionSlot::new);
        slot.setSlotCode(slotCode);
        slot.setProductSlug(resolvedProductSlug);
        slot.setDomain(domain);
        slot.setPublicUrl(publicUrl);
        slot.setBackendUrl(StringUtils.hasText(request.backendUrl()) ? request.backendUrl().trim() : null);
        slot.setExperienceVersion(normalizeRequired(request.experienceVersion(), "Versão PDE obrigatória"));
        slot.setTargetEnvironment(StringUtils.hasText(request.targetEnvironment())
                ? request.targetEnvironment().trim()
                : "production-" + slotCode);
        slot.setStatus(request.status() != null ? request.status() : PdeProductionSlotStatus.PLANNED);
        slot.setSourceExperimentId(request.sourceExperimentId() != null
                ? request.sourceExperimentId()
                : defaultSourceExperimentId);
        slot.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
        return toProductionSlotDto(repository.save(slot));
    }

    /** Converte o slot persistido em contrato administrativo do painel. */
    private PostDeployPdeProductionSlotDto toProductionSlotDto(PdeProductionSlot slot) {
        return new PostDeployPdeProductionSlotDto(
                slot.getId(),
                slot.getSlotCode(),
                slot.getProductSlug(),
                slot.getDomain(),
                slot.getPublicUrl(),
                slot.getBackendUrl(),
                slot.getExperienceVersion(),
                slot.getTargetEnvironment(),
                slot.getStatus(),
                slot.getSourceExperimentId(),
                slot.getNotes(),
                slot.getCreatedAt(),
                slot.getUpdatedAt());
    }

    /** Normaliza um campo obrigatório textual antes de persistir contrato de publicação. */
    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    /** Normaliza domínio removendo protocolo e barra final para evitar duplicidade operacional. */
    private String normalizeDomain(String value) {
        String domain = normalizeRequired(value, "Domínio do slot PDE obrigatório")
                .replaceFirst("^https?://", "")
                .replaceAll("/+$", "")
                .toLowerCase(Locale.ROOT);
        if (!domain.endsWith("clubemusa.com.br")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Slot PDE MUSA deve usar subdomínio de clubemusa.com.br");
        }
        return domain;
    }
}
