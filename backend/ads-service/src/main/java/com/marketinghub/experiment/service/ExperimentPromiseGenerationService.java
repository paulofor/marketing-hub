package com.marketinghub.experiment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequest;
import com.marketinghub.experiment.promise.ExperimentPromiseGenerationRequestStatus;
import com.marketinghub.experiment.service.generatepromise.ExperimentPromiseOptionDto;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsRequest;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsResponse;
import com.marketinghub.experiment.service.generatepromise.latestdraft.ExperimentPromiseOptionsDraftResponse;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentPromiseGenerationRequestRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: registrar solicitações de opções de promessa para processamento assíncrono pelo AI Worker. */
@Service
public class ExperimentPromiseGenerationService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPromiseGenerationService.class);
    private static final String DEFAULT_MODEL = "gpt-5.2";

    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final ExperimentPromiseGenerationRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com repositórios e serializador usados para registrar a solicitação. */
    public ExperimentPromiseGenerationService(MarketNicheRepository nicheRepository,
                                              HypothesisRepository hypothesisRepository,
                                              ExperimentPromiseGenerationRequestRepository requestRepository,
                                              ObjectMapper objectMapper) {
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    /** Registra uma solicitação pendente para o AI Worker gerar três opções de promessa. */
    @Transactional
    public GenerateExperimentPromiseOptionsResponse generate(GenerateExperimentPromiseOptionsRequest request) {
        if (request == null || request.nicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um nicho antes de gerar com IA.");
        }
        if (request.hypothesisId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma hipótese antes de gerar com IA.");
        }
        MarketNiche niche = nicheRepository.findById(request.nicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nicho não encontrado"));
        Hypothesis hypothesis = resolveHypothesis(request.hypothesisId());
        String prompt = buildPrompt(niche, hypothesis);
        ExperimentPromiseGenerationRequest entity = ExperimentPromiseGenerationRequest.builder()
                .niche(niche)
                .hypothesis(hypothesis)
                .status(ExperimentPromiseGenerationRequestStatus.PENDING)
                .model(DEFAULT_MODEL)
                .prompt(prompt)
                .currentSinglePain(trimToNull(request.currentSinglePain()))
                .currentFreeReward(trimToNull(request.currentFreeReward()))
                .currentFunnelPromise(trimToNull(request.currentFunnelPromise()))
                .currentPrimaryCta(trimToNull(request.currentPrimaryCta()))
                .build();
        ExperimentPromiseGenerationRequest saved = requestRepository.save(entity);
        return new GenerateExperimentPromiseOptionsResponse(saved.getId(), saved.getStatus().name(), saved.getPrompt(), List.of());
    }

    /** Retorna a solicitação mais recente ainda útil para retomada da criação do teste pela tela. */
    @Transactional(readOnly = true)
    public Optional<ExperimentPromiseOptionsDraftResponse> latestDraft() {
        return requestRepository.findFirstByStatusInOrderByCreatedAtDesc(List.of(
                        ExperimentPromiseGenerationRequestStatus.PENDING,
                        ExperimentPromiseGenerationRequestStatus.PROCESSING,
                        ExperimentPromiseGenerationRequestStatus.COMPLETED))
                .map(this::toDraftResponse);
    }

    /** Consulta uma solicitação específica para a tela acompanhar até a resposta final do AI Worker. */
    @Transactional(readOnly = true)
    public GenerateExperimentPromiseOptionsResponse get(Long id) {
        ExperimentPromiseGenerationRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        return toResponse(entity);
    }

    /** Lista solicitações pendentes para o AI Worker consumir pelo endpoint pending. */
    @Transactional(readOnly = true)
    public List<GenerateExperimentPromiseOptionsResponse> listPending(int limit) {
        return requestRepository.findByStatusOrderByCreatedAtAsc(
                        ExperimentPromiseGenerationRequestStatus.PENDING,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Marca a solicitação como processando para evitar execução duplicada por workers concorrentes. */
    @Transactional
    public GenerateExperimentPromiseOptionsResponse claim(Long id, String workerId) {
        ExperimentPromiseGenerationRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        if (entity.getStatus() != ExperimentPromiseGenerationRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação não está pendente");
        }
        entity.setStatus(ExperimentPromiseGenerationRequestStatus.PROCESSING);
        entity.setWorkerId(StringUtils.hasText(workerId) ? workerId.trim() : "unknown-worker");
        entity.setStartedAt(Instant.now());
        return toResponse(entity);
    }

    /** Conclui a solicitação com as opções geradas pelo AI Worker. */
    @Transactional
    public GenerateExperimentPromiseOptionsResponse complete(Long id, GenerateExperimentPromiseOptionsResponse response) {
        ExperimentPromiseGenerationRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        List<ExperimentPromiseOptionDto> options = response != null ? response.options() : List.of();
        if (options == null || options.size() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe exatamente três opções geradas.");
        }
        entity.setOptionsJson(writeOptions(options));
        entity.setStatus(ExperimentPromiseGenerationRequestStatus.COMPLETED);
        entity.setFinishedAt(Instant.now());
        entity.setErrorMessage(null);
        return toResponse(entity);
    }

    /** Descarta uma solicitação retomável depois que o teste foi salvo ou o usuário decidiu não continuar. */
    @Transactional
    public void dismiss(Long id) {
        ExperimentPromiseGenerationRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        entity.setStatus(ExperimentPromiseGenerationRequestStatus.DISMISSED);
        entity.setFinishedAt(Instant.now());
    }

    /** Marca a solicitação como falha quando o AI Worker não consegue gerar as opções. */
    @Transactional
    public void fail(Long id, String errorMessage) {
        ExperimentPromiseGenerationRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada"));
        entity.setStatus(ExperimentPromiseGenerationRequestStatus.FAILED);
        entity.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Falha desconhecida no AI Worker");
        entity.setFinishedAt(Instant.now());
    }

    /** Localiza a hipótese obrigatória usada como contexto da geração. */
    private Hypothesis resolveHypothesis(java.util.UUID hypothesisId) {
        return hypothesisRepository.findById(hypothesisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hipótese não encontrada"));
    }

    /** Monta um contexto comercial enxuto para evitar prompts grandes demais na geração de promessa. */
    private String buildPrompt(MarketNiche niche, Hypothesis hypothesis) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "Contexto enxuto para gerar exatamente 3 opções diferentes de contrato de promessa única para um novo experimento.\n");
        appendCompactNicheDetails(sb, niche);
        appendCompactHypothesisDetails(sb, hypothesis);
        sb.append("\nRegras da resposta:\n");
        sb.append("- Gere uma opção direta, uma emocional e uma operacional/prática.\n");
        sb.append(
                "- Cada opção deve conter uma dor única, uma recompensa gratuita concreta, uma promessa plausível e um CTA claro.\n");
        sb.append("- Não use campos digitados pelo usuário; a tela escolhe uma opção gerada pela IA.\n");
        return sb.toString();
    }

    /** Adiciona ao prompt apenas os sinais de nicho necessários para gerar promessa comercial. */
    private void appendCompactNicheDetails(StringBuilder sb, MarketNiche niche) {
        sb.append("Nicho selecionado:\n");
        appendIfPresent(sb, "- Nome", compact(niche.getName(), 300));
        appendIfPresent(sb, "- Descrição", compact(niche.getDescription(), 500));
        appendIfPresent(sb, "- Segmentação base", compact(niche.getBaseSegmentation(), 500));
        appendIfPresent(sb, "- Volume de demanda", compact(niche.getDemandVolume(), 300));
        appendListIfPresent(sb, "- Interesses principais", niche.getInterestList());
        appendListIfPresent(sb, "- Cargos principais", niche.getRoleList());
        appendRichNicheSummary(sb, niche);
    }

    /** Adiciona ao prompt um resumo da descrição rica sem incluir prompts ou evidências brutas. */
    private void appendRichNicheSummary(StringBuilder sb, MarketNiche niche) {
        if (niche.getHypothesisDetailedDescription() == null) {
            return;
        }
        var description = niche.getHypothesisDetailedDescription();
        sb.append("\nResumo do nicho:\n");
        appendIfPresent(sb, "- Dores", compact(description.getPains(), 900));
        appendIfPresent(sb, "- Desejos", compact(description.getDesires(), 700));
        appendIfPresent(sb, "- Necessidades", compact(description.getNeeds(), 700));
    }

    /** Adiciona ao prompt os campos decisivos da hipótese sem carregar JSON completo ou prompt original. */
    private void appendCompactHypothesisDetails(StringBuilder sb, Hypothesis hypothesis) {
        sb.append("\nHipótese selecionada:\n");
        appendIfPresent(sb, "- Título", compact(hypothesis.getTitle(), 300));
        appendIfPresent(sb, "- Persona", compact(hypothesis.getPersona(), 500));
        appendStructuredText(sb, "- Dor", hypothesis.getProblem(), 1200);
        appendStructuredText(sb, "- Promessa", hypothesis.getPromise(), 900);
        appendStructuredText(
                sb, "- Mecanismo", firstText(hypothesis.getMechanism(), hypothesis.getUniqueMechanism()), 1000);
        appendStructuredText(sb, "- Entrega", hypothesis.getEntrega(), 1200);
        appendStructuredText(sb, "- Prova", hypothesis.getSuccessRule(), 900);
        appendIfPresent(
                sb, "- Tipo de oferta", hypothesis.getOfferType() != null ? hypothesis.getOfferType().name() : null);
        appendIfPresent(sb, "- Preço", hypothesis.getPrice() != null ? hypothesis.getPrice().toPlainString() : null);
    }

    /** Adiciona texto estruturado priorizando summaries quando o campo estiver em JSON. */
    private void appendStructuredText(StringBuilder sb, String label, String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        appendIfPresent(sb, label, compact(extractUsefulText(value), maxLength));
    }

    /** Extrai o resumo funcional de JSONs de pipeline, evitando enviar evidências brutas para a IA. */
    private String extractUsefulText(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            StringBuilder sb = new StringBuilder();
            appendJsonField(sb, root, "summary");
            appendJsonField(sb, root, "surface");
            appendJsonField(sb, root, "root");
            appendJsonField(sb, root, "entryPromise");
            appendJsonField(sb, root, "coreOffer");
            appendJsonField(sb, root, "proofMessage");
            appendJsonField(sb, root, "mechanism");
            return sb.length() > 0 ? sb.toString().trim() : trimmed;
        } catch (JsonProcessingException ex) {
            return trimmed;
        }
    }

    /** Copia um campo textual de JSON para o resumo enviado ao modelo. */
    private void appendJsonField(StringBuilder sb, JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isTextual() && StringUtils.hasText(node.asText())) {
            sb.append(fieldName).append(": ").append(node.asText().trim()).append("\n");
        }
    }

    /** Retorna o primeiro texto preenchido entre duas opções equivalentes. */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /** Limita textos longos preservando o começo do contexto de negócio. */
    private String compact(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim() + "...";
    }

    /** Adiciona texto ao prompt quando o valor existe. */
    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    /** Adiciona lista ao prompt quando há valores úteis. */
    private void appendListIfPresent(StringBuilder sb, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            sb.append(label).append(": ").append(String.join(", ", values)).append("\n");
        }
    }

    /** Converte a entidade persistida para o contrato de rascunho retomável pela tela. */
    private ExperimentPromiseOptionsDraftResponse toDraftResponse(ExperimentPromiseGenerationRequest entity) {
        return new ExperimentPromiseOptionsDraftResponse(
                entity.getId(),
                entity.getStatus().name(),
                entity.getNiche().getId(),
                entity.getHypothesis().getId(),
                entity.getCurrentSinglePain(),
                entity.getCurrentFreeReward(),
                entity.getCurrentFunnelPromise(),
                entity.getCurrentPrimaryCta(),
                readOptions(entity.getOptionsJson()));
    }

    /** Converte a entidade persistida para contrato de API. */
    private GenerateExperimentPromiseOptionsResponse toResponse(ExperimentPromiseGenerationRequest entity) {
        return new GenerateExperimentPromiseOptionsResponse(entity.getId(), entity.getStatus().name(), entity.getPrompt(), readOptions(entity.getOptionsJson()));
    }

    /** Serializa as opções recebidas do AI Worker para auditoria persistida. */
    private String writeOptions(List<ExperimentPromiseOptionDto> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            log.error("Falha ao serializar opções de promessa; operation=experiment-promise-options-serialize", ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opções geradas inválidas.", ex);
        }
    }

    /** Lê opções já persistidas, retornando lista vazia para solicitações ainda pendentes. */
    private List<ExperimentPromiseOptionDto> readOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(ExperimentPromiseOptionDto.class).readValue(optionsJson);
        } catch (Exception ex) {
            log.error("Falha ao ler opções de promessa persistidas; operation=experiment-promise-options-read", ex);
            return List.of();
        }
    }

    /** Normaliza textos opcionais antes da persistência. */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
