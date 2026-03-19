package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.dto.LeadPortalFlowDto;
import com.marketinghub.leadportal.dto.LeadPortalImagePromptMetadataDto;
import com.marketinghub.leadportal.dto.LeadPortalImagePromptPlaceholderDto;
import com.marketinghub.leadportal.dto.UpdateLeadPortalImagePromptRequest;
import com.marketinghub.leadportal.mapper.LeadPortalFlowMapper;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints relacionados à configuração do prompt de geração de imagens do Lead Portal.
 */
@RestController
@RequestMapping("/api/lead-portal/image-prompts")
public class LeadPortalImagePromptController {

    private static final String DEFAULT_IMAGE_MODEL = "gpt-image-1";
    private static final int DEFAULT_BATCH_SIZE = 6;
    private static final String DEFAULT_TEMPLATE = """
Gere materiais de divulgação premium em português para {{profissional}}, um(a) {{atividade}} que atua em {{local}}.
Requisitos obrigatórios:
1. Visual bonito, atraente e com atmosfera profissional, destacando o universo de {{atividade}}.
2. Valorize os serviços principais ({{servicos}}) com chamadas claras, pensadas para redes sociais.
3. Mostre formas de contato visíveis adicionando {{contato}} no design.
4. Use cores vivas, iluminação moderna e elementos que façam referência ao ambiente de estúdio ou atendimento personalizado.
5. Entregue um pacote em lote (batch) com pelo menos {{batch_size}} variações quadradas (1:1), prontas para feed e fáceis de adaptar para stories.

Dados coletados no formulário. Use-os para definir copy, cenário, elementos visuais e público-alvo:
{{dados_json}}
""";

    private final LeadPortalFlowService flowService;
    private final LeadPortalFlowMapper mapper;
    private final LeadPortalPublicUrlResolver publicUrlResolver;

    public LeadPortalImagePromptController(
            LeadPortalFlowService flowService,
            LeadPortalFlowMapper mapper,
            LeadPortalPublicUrlResolver publicUrlResolver) {
        this.flowService = flowService;
        this.mapper = mapper;
        this.publicUrlResolver = publicUrlResolver;
    }

    @GetMapping("/metadata")
    public LeadPortalImagePromptMetadataDto metadata() {
        return new LeadPortalImagePromptMetadataDto(
                DEFAULT_TEMPLATE,
                DEFAULT_IMAGE_MODEL,
                DEFAULT_BATCH_SIZE,
                placeholderCatalog());
    }

    @PutMapping("/{flowId}")
    public LeadPortalFlowDto update(
            @PathVariable("flowId") Long flowId,
            @RequestBody UpdateLeadPortalImagePromptRequest request) {
        LeadPortalFlow updated = flowService.updateImagePrompt(flowId, request);
        LeadPortalFlowDto dto = mapper.toDto(updated);
        dto.setPublicUrl(publicUrlResolver.resolve(updated));
        return dto;
    }

    private List<LeadPortalImagePromptPlaceholderDto> placeholderCatalog() {
        return List.of(
                new LeadPortalImagePromptPlaceholderDto("{{profissional}}", "Nome informado no formulário.", "Pablito"),
                new LeadPortalImagePromptPlaceholderDto("{{nome}}", "Alias direto para o campo de nome do formulário.", "Pablito"),
                new LeadPortalImagePromptPlaceholderDto("{{atividade}}", "Profissão derivada do slug do fluxo simples.", "personal trainer"),
                new LeadPortalImagePromptPlaceholderDto("{{studio}}", "Nome do estúdio ou empresa.", "Studio Pablito"),
                new LeadPortalImagePromptPlaceholderDto("{{local}}", "Cidade/bairro onde o profissional atende.", "Niterói - RJ"),
                new LeadPortalImagePromptPlaceholderDto("{{contato}}", "Resumo do melhor canal de contato.", "WhatsApp: (21) 99290-2732"),
                new LeadPortalImagePromptPlaceholderDto("{{servicos}}", "Lista resumida dos serviços principais.", "Treinos funcionais, HIIT"),
                new LeadPortalImagePromptPlaceholderDto("{{batch_size}}", "Quantidade final de imagens que será solicitada ao worker.", String.valueOf(DEFAULT_BATCH_SIZE)),
                new LeadPortalImagePromptPlaceholderDto("{{dados_json}}", "Bloco JSON com todas as respostas saneadas.", "{\\n  \"atividade\": \"personal trainer\"... }"),
                new LeadPortalImagePromptPlaceholderDto("{{respostas.whatsapp}}", "Qualquer resposta individual usando o prefixo respostas.", "(21) 99290-2732"),
                new LeadPortalImagePromptPlaceholderDto("{{resposta.instagram}}", "Respostas enviadas com o prefixo resposta.", "@meuteste"),
                new LeadPortalImagePromptPlaceholderDto("{{resposta.especialidade}}", "Outro exemplo de campo personalizado com prefixo resposta.", "Alongamento")
        );
    }
}
