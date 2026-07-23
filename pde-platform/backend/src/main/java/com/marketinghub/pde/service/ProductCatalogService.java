package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse.DiagnosticDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.MissionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.ScientificEvidencePackDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.ScientificReferenceDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.SupportMaterialDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.ThemeDto;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Mantém o catálogo configurável dos produtos experienciais disponíveis. */
@Service
public class ProductCatalogService {
    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);

    private final Map<String, ProductExperienceResponse> products = Map.of(
            "metodo-musa-7-dias", createMusaProduct());
    private final RestClient.Builder restClientBuilder;
    private final List<String> marketingHubBaseUrls;
    private final String experienceVersionOverride;

    /** Cria o catálogo com integração opcional ao Marketing Hub como fonte de verdade comercial. */
    @Autowired
    public ProductCatalogService(
            RestClient.Builder restClientBuilder,
            @Value("${pde.catalog.marketing-hub-base-url:}") String marketingHubBaseUrl,
            @Value("${pde.catalog.experience-version-override:}") String experienceVersionOverride) {
        this.restClientBuilder = restClientBuilder;
        this.marketingHubBaseUrls = parseMarketingHubBaseUrls(marketingHubBaseUrl);
        this.experienceVersionOverride = experienceVersionOverride;
    }

    /** Cria o catálogo em testes unitários sem dependência do Marketing Hub. */
    ProductCatalogService() {
        this(RestClient.builder(), "", "");
    }

    /** Retorna a experiência configurada para o produto informado. */
    public ProductExperienceResponse getProduct(String slug) {
        Optional<ProductExperienceResponse> marketingHubProduct = loadMarketingHubProduct(slug);
        if (marketingHubProduct.isPresent()) {
            return applyExperienceVersionOverride(marketingHubProduct.get());
        }
        ProductExperienceResponse product = products.get(slug);
        if (product == null) {
            throw new IllegalArgumentException("Produto PDE não encontrado: " + slug);
        }
        return applyExperienceVersionOverride(product);
    }

    /** Aplica override operacional de versão para publicar homologações sem alterar o contrato base. */
    private ProductExperienceResponse applyExperienceVersionOverride(ProductExperienceResponse product) {
        if (!StringUtils.hasText(experienceVersionOverride)) {
            return product;
        }
        return new ProductExperienceResponse(
                product.slug(),
                experienceVersionOverride.trim(),
                product.funnelVersion(),
                product.name(),
                product.promise(),
                product.audience(),
                product.priceLabel(),
                product.theme(),
                product.diagnostic(),
                product.missions(),
                product.supportMaterials(),
                product.scientificEvidencePack(),
                product.completionOffer());
    }

    /** Carrega o contrato PDE publicado pelo Marketing Hub quando a integração estiver configurada. */
    private Optional<ProductExperienceResponse> loadMarketingHubProduct(String slug) {
        if (marketingHubBaseUrls.isEmpty()) {
            return Optional.empty();
        }
        for (String baseUrl : marketingHubBaseUrls) {
            try {
                ProductExperienceResponse product = restClientBuilder.clone()
                        .baseUrl(baseUrl)
                        .build()
                        .get()
                        .uri("/api/products/public/{slug}/pde-experience", slug)
                        .retrieve()
                        .body(ProductExperienceResponse.class);
                return Optional.ofNullable(product);
            } catch (RuntimeException ex) {
                log.warn("Falha ao carregar experiência PDE do Marketing Hub; tentando fallback: slug={}, baseUrl={}",
                        slug, baseUrl, ex);
            }
        }
        log.warn("Experiência PDE do Marketing Hub indisponível em todas as bases configuradas; usando catálogo local: slug={}",
                slug);
        return Optional.empty();
    }

    /** Converte a configuração de URLs do Marketing Hub em uma lista ordenada de fallback. */
    private static List<String> parseMarketingHubBaseUrls(String marketingHubBaseUrl) {
        if (!StringUtils.hasText(marketingHubBaseUrl)) {
            return List.of();
        }
        return Arrays.stream(marketingHubBaseUrl.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /** Cria a configuração local do produto MUSA usada quando o Marketing Hub não entrega o contrato. */
    private static ProductExperienceResponse createMusaProduct() {
        return new ProductExperienceResponse(
                "metodo-musa-7-dias",
                "musa-pde-entry-v5-estrada-desejo",
                "musa-membership-funnel-v1",
                "Método MUSA - Experiência Guiada de 7 Dias",
                "Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro.",
                "Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.",
                "",
                new ThemeDto("#7a2444", "#d6a75c", "#fff8f3", "/assets/musa-cover.png"),
                new DiagnosticDto(
                        "Mapa de Presença MUSA",
                        "Comece pelo espelho: descubra o primeiro ajuste para sua imagem comunicar mais intenção hoje, usando o que você já tem.",
                        List.of("O que minha imagem comunica hoje?")),
                List.of(
                        new MissionDto(
                                "dia-1-ruido-visual",
                                1,
                                "Ler o sinal que sua imagem comunica",
                                "A presença cresce quando você identifica o sinal visual que mais distancia sua imagem da mulher que você quer transmitir.",
                                "Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe roupa, cabelo, pele, perfume e detalhe final, identifique o sinal que deixa sua imagem comum ou desalinhada e escolha uma microação para comunicar mais intenção.",
                                "Frase preenchida: hoje minha imagem comunica menos intenção quando...",
                                "Compare a sensação antes/depois de remover ruído visual ou reforçar um sinal de presença."),
                        new MissionDto(
                                "dia-2-assinatura",
                                2,
                                "Criar sua assinatura simples",
                                "Coerência repetida cria reconhecimento sem exigir roupa nova.",
                                "Defina 3 sinais que você quer repetir: acabamento do cabelo, cor-base, textura, perfume, acessório ou maquiagem leve.",
                                "Lista dos 3 sinais escolhidos.",
                                "Monte um pequeno painel com seus sinais recorrentes."),
                        new MissionDto(
                                "dia-3-base-acessivel",
                                3,
                                "Usar o que já existe melhor",
                                "A mudança fica mais viável quando começa pelo que já está no armário.",
                                "Separe 5 peças, 2 acessórios e 1 perfume que já podem sustentar a presença desejada.",
                                "Inventário simples dos itens reaproveitados.",
                                "Organize os itens em uma combinação para uma saída real."),
                        new MissionDto(
                                "dia-4-checklist-12-minutos",
                                4,
                                "Fazer o acabamento de 12 minutos",
                                "Rotinas curtas reduzem atrito e aumentam consistência.",
                                "Passe pelo checklist cabelo, pele, roupa, perfume, acessório e postura antes de sair.",
                                "Checklist marcado com o tempo gasto.",
                                "Use uma escala de 1 a 5 para medir coerência final."),
                        new MissionDto(
                                "dia-5-compra-inteligente",
                                5,
                                "Segurar a compra que não resolve",
                                "Compra boa é aquela que fortalece sua assinatura, não a que compensa insegurança momentânea.",
                                "Antes de comprar algo, responda se o item combina com seus 3 sinais de presença.",
                                "Decisão registrada: comprar, esperar ou descartar.",
                                "Compare desejo imediato com utilidade real na sua rotina."),
                        new MissionDto(
                                "dia-6-situacao-chave",
                                6,
                                "Preparar sua entrada",
                                "A presença fica mais forte quando é planejada para contextos reais.",
                                "Escolha uma ocasião e monte uma composição completa com intenção: roupa, cabelo, pele, perfume e detalhe final.",
                                "Plano da ocasião com roupa, cabelo, pele, perfume e detalhe final.",
                                "Visualize a entrada no ambiente e ajuste o que estiver incoerente."),
                        new MissionDto(
                                "dia-7-plano-pessoal",
                                7,
                                "Fechar seu antes e depois",
                                "A transformação continua quando vira padrão simples de repetição.",
                                "Monte seu plano de manutenção com sinais, checklist e regra anti-impulso.",
                                "Plano pessoal preenchido.",
                                "Escolha um ritual semanal de 15 minutos para manter sua presença.")),
                List.of(
                        new SupportMaterialDto(
                                "E-book Método MUSA",
                                "PDF",
                                "Guia de consulta para entender o método, ver exemplos e revisar sua semana.",
                                "/materials/metodo-musa-ebook.pdf"),
                        new SupportMaterialDto(
                                "Experiência Guiada MUSA",
                                "HTML",
                                "Versão navegável da experiência para consultar a ordem, o diagnóstico e as missões de 7 dias.",
                                "/materials/experiencia-guiada-musa.html"),
                        new SupportMaterialDto(
                                "Plano, Checklists e Templates",
                                "CSV",
                                "Planilha com a ordem de aplicação, critérios de conclusão e pontos de atenção de cada material.",
                                "/materials/plano-checklists-e-templates.csv"),
                        new SupportMaterialDto(
                                "Mapa Visual MUSA",
                                "Infográfico",
                                "Resumo visual do método: coerência, redução de ruído e assinatura pessoal.",
                                "/materials/mapa-visual-musa.png")),
                createMusaScientificEvidencePack(),
                "Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais de presença, estilo e autocuidado acessível.");
    }

    /** Cria o pacote científico operacional do MUSA usado pela Consultora MUSA. */
    private static ScientificEvidencePackDto createMusaScientificEvidencePack() {
        return new ScientificEvidencePackDto(
                "musa-evidence-pack-v1",
                List.of(
                        "A roupa pode influenciar a forma como a pessoa se percebe e se comporta em uma situação.",
                        "Escolhas de vestimenta, formalidade e acabamento participam da percepção social e dos primeiros julgamentos.",
                        "Coerência visual, intenção e repetição de sinais podem reduzir ruído percebido e facilitar reconhecimento pessoal."),
                List.of(
                        "Transformar princípios de cognição vestida e percepção social em microdecisões simples de roupa, cor, acabamento, postura e detalhe final.",
                        "Orientar a cliente a usar o que já possui antes de comprar novas peças.",
                        "Reforçar presença elegante como percepção e coerência, não como garantia universal de aprovação externa."),
                List.of(
                        "isso ajuda você a comunicar mais intenção",
                        "pode reduzir ruído visual",
                        "favorece uma presença mais coerente",
                        "ajuda você a se sentir mais alinhada com a imagem que quer transmitir"),
                List.of(
                        "garante elegância",
                        "muda como todos vão te ver",
                        "transforma sua personalidade",
                        "efeito comprovado em qualquer pessoa",
                        "substitui autoestima, terapia ou consultoria individual"),
                List.of(
                        new ScientificReferenceDto(
                                "Adam e Galinsky",
                                "2012",
                                "Enclothed cognition",
                                "Journal of Experimental Social Psychology",
                                "10.1016/j.jesp.2012.02.008"),
                        new ScientificReferenceDto(
                                "Slepian, Ferber, Gold e Rutchick",
                                "2015",
                                "The Cognitive Consequences of Formal Clothing",
                                "Social Psychological and Personality Science",
                                "10.1177/1948550615579462"),
                        new ScientificReferenceDto(
                                "Howlett, Pine, Orakcioglu e Fletcher",
                                "2013",
                                "The influence of clothing on first impressions",
                                "Journal of Fashion Marketing and Management",
                                "10.1108/13612021311305128"),
                        new ScientificReferenceDto(
                                "Hester e Hehman",
                                "2023",
                                "Dress is a Fundamental Component of Person Perception",
                                "Personality and Social Psychology Review",
                                "10.1177/10888683231157961")));
    }
}
