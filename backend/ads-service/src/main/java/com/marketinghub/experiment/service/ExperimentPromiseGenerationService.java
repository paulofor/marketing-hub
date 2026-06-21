package com.marketinghub.experiment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        String prompt = buildPrompt(request, niche, hypothesis);
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

    /** Monta o contexto funcional do nicho, hipótese e campos já digitados na tela. */
    private String buildPrompt(GenerateExperimentPromiseOptionsRequest request, MarketNiche niche, Hypothesis hypothesis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contexto para gerar exatamente 3 opções diferentes de contrato de promessa única para um novo experimento.\n");
        appendNicheDetails(sb, niche);
        appendRichNicheDescription(sb, niche);
        appendHypothesisPipelineDetails(sb, hypothesis);
        appendIfPresent(sb, "Dor atual digitada", request.currentSinglePain());
        appendIfPresent(sb, "Recompensa atual digitada", request.currentFreeReward());
        appendIfPresent(sb, "Promessa atual digitada", request.currentFunnelPromise());
        appendIfPresent(sb, "CTA atual digitado", request.currentPrimaryCta());
        sb.append("\nAs três opções devem ser distintas: uma direta, uma emocional e uma operacional/prática.");
        return sb.toString();
    }

    /** Adiciona ao prompt os detalhes disponíveis do nicho selecionado. */
    private void appendNicheDetails(StringBuilder sb, MarketNiche niche) {
        sb.append("Nicho selecionado:\n");
        appendIfPresent(sb, "- Nome", niche.getName());
        appendIfPresent(sb, "- Descrição", niche.getDescription());
        appendIfPresent(sb, "- Categoria de interesse", niche.getInterestCategory());
        appendIfPresent(sb, "- Categoria de cargo", niche.getRoleCategory());
        appendIfPresent(sb, "- Segmentação base", niche.getBaseSegmentation());
        appendIfPresent(sb, "- Interesses", niche.getInterests());
        appendIfPresent(sb, "- Filtros demográficos", niche.getDemographicFilters());
        appendIfPresent(sb, "- Dicas extras", niche.getExtraTips());
        appendIfPresent(sb, "- Promessas validadas", niche.getPromises());
        appendIfPresent(sb, "- Ofertas validadas", niche.getOffers());
        appendIfPresent(sb, "- Volume de demanda", niche.getDemandVolume());
        appendIfPresent(sb, "- Modelo de hipóteses do nicho", niche.getHypothesisModel());
        appendListIfPresent(sb, "- Lista curada de interesses", niche.getInterestList());
        appendListIfPresent(sb, "- Lista curada de cargos", niche.getRoleList());
        appendListIfPresent(sb, "- Lista curada de comportamentos", niche.getBehaviorList());
    }

    /** Adiciona a descrição rica ativa do nicho quando ela já foi gerada pelo pipeline de IA. */
    private void appendRichNicheDescription(StringBuilder sb, MarketNiche niche) {
        if (niche.getHypothesisDetailedDescription() == null) {
            return;
        }
        var description = niche.getHypothesisDetailedDescription();
        sb.append("\nDescrição rica ativa do nicho:\n");
        appendIfPresent(sb, "- Título da descrição rica", description.getTitle());
        appendIfPresent(sb, "- Descrição rica", description.getDescription());
        appendIfPresent(sb, "- Dores detalhadas", description.getPains());
        appendIfPresent(sb, "- Desejos detalhados", description.getDesires());
        appendIfPresent(sb, "- Necessidades detalhadas", description.getNeeds());
        appendIfPresent(sb, "- Prompt que gerou a descrição rica", description.getPrompt());
        appendIfPresent(sb, "- Modelo da descrição rica", description.getModel());
    }

    /** Adiciona ao prompt o pipeline completo conhecido da hipótese selecionada. */
    private void appendHypothesisPipelineDetails(StringBuilder sb, Hypothesis hypothesis) {
        sb.append("\nHipótese selecionada e pipeline de hipótese:\n");
        appendIfPresent(sb, "- Título", hypothesis.getTitle());
        appendIfPresent(sb, "- Persona", hypothesis.getPersona());
        appendIfPresent(sb, "- Dor", hypothesis.getProblem());
        appendIfPresent(sb, "- Promessa", hypothesis.getPromise());
        appendIfPresent(sb, "- Mecanismo", hypothesis.getMechanism());
        appendIfPresent(sb, "- Mecanismo único", hypothesis.getUniqueMechanism());
        appendIfPresent(sb, "- Entrega", hypothesis.getEntrega());
        appendIfPresent(sb, "- Regra de sucesso", hypothesis.getSuccessRule());
        appendIfPresent(sb, "- Tipo de oferta", hypothesis.getOfferType() != null ? hypothesis.getOfferType().name() : null);
        appendIfPresent(sb, "- Preço", hypothesis.getPrice() != null ? hypothesis.getPrice().toPlainString() : null);
        appendIfPresent(sb, "- Meta de CPL", hypothesis.getKpiTargetCpl() != null ? hypothesis.getKpiTargetCpl().toPlainString() : null);
        appendIfPresent(sb, "- Status", hypothesis.getStatus() != null ? hypothesis.getStatus().name() : null);
        appendIfPresent(sb, "- Modelo da hipótese", hypothesis.getModel());
        appendIfPresent(sb, "- Prompt que gerou a hipótese", hypothesis.getPrompt());
        appendIfPresent(sb, "- Snapshot JSON do framework de hipótese", hypothesis.getFrameworkJson());
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
