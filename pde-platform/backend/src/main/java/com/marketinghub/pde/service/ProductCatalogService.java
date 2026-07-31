package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse.DiagnosticDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.HeroVideoDto;
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
    private static final String MUSA_PRICE_LABEL = "R$67";
    private static final String MUSA_V5_EXPERIENCE_VERSION = "musa-pde-entry-v5-video-explicativo";
    private static final String MUSA_V6_EXPERIENCE_VERSION = "musa-pde-entry-v6-video-motivacional";
    private static final String MUSA_V7_EXPERIENCE_VERSION = "musa-pde-entry-v7-espelho-antes-de-sair";
    private static final Map<String, String> MUSA_VERSIONED_HOST_EXPERIENCES = Map.of(
            "v1.clubemusa.com.br", MUSA_V5_EXPERIENCE_VERSION,
            "v2.clubemusa.com.br", MUSA_V5_EXPERIENCE_VERSION,
            "v5.clubemusa.com.br", MUSA_V5_EXPERIENCE_VERSION,
            "v6.clubemusa.com.br", MUSA_V6_EXPERIENCE_VERSION,
            "v7.clubemusa.com.br", MUSA_V7_EXPERIENCE_VERSION,
            "v8.clubemusa.com.br", MUSA_V7_EXPERIENCE_VERSION,
            "v9.clubemusa.com.br", MUSA_V7_EXPERIENCE_VERSION,
            "v10.clubemusa.com.br", MUSA_V7_EXPERIENCE_VERSION);

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
        return getProductForHost(slug, "");
    }

    /** Retorna a experiência configurada considerando o hostname versionado de produção. */
    public ProductExperienceResponse getProductForHost(String slug, String host) {
        return getProductForRequest(slug, host, "", "");
    }

    /** Retorna a experiência considerando host, slot e versão informados pela superfície pública. */
    public ProductExperienceResponse getProductForRequest(
            String slug,
            String host,
            String requestedSlotCode,
            String requestedExperienceVersion) {
        Optional<ProductExperienceResponse> marketingHubProduct =
                loadMarketingHubProduct(slug, host, requestedSlotCode, requestedExperienceVersion);
        if (marketingHubProduct.isPresent()) {
            return applyDefaultLayout(marketingHubProduct.get());
        }
        ProductExperienceResponse product = products.get(slug);
        if (product == null) {
            throw new IllegalArgumentException("Produto PDE não encontrado: " + slug);
        }
        return applyExperienceVersionOverride(product, host, requestedExperienceVersion);
    }

    /** Aplica override operacional ou versão derivada do hostname sem alterar o contrato base. */
    private ProductExperienceResponse applyExperienceVersionOverride(ProductExperienceResponse product, String host) {
        return applyExperienceVersionOverride(product, host, "");
    }

    /** Aplica versão solicitada, override operacional ou versão derivada do hostname sem alterar o contrato base. */
    private ProductExperienceResponse applyExperienceVersionOverride(
            ProductExperienceResponse product,
            String host,
            String requestedExperienceVersion) {
        String selectedExperienceVersion = resolveHostExperienceVersion(host);
        if (!StringUtils.hasText(selectedExperienceVersion)) {
            selectedExperienceVersion = requestedExperienceVersion;
        }
        if (!StringUtils.hasText(selectedExperienceVersion)) {
            selectedExperienceVersion = experienceVersionOverride;
        }
        if (!StringUtils.hasText(selectedExperienceVersion)) {
            return product;
        }
        if (MUSA_V7_EXPERIENCE_VERSION.equals(selectedExperienceVersion.trim())) {
            product = createMusaScientificV7Product();
        }
        return new ProductExperienceResponse(
                product.slug(),
                selectedExperienceVersion.trim(),
                product.layoutKey(),
                product.funnelVersion(),
                product.name(),
                product.promise(),
                product.audience(),
                product.priceLabel(),
                product.theme(),
                product.diagnostic(),
                product.missions(),
                product.supportMaterials(),
                product.heroVideos(),
                product.publicDiagnosticQuestions(),
                product.publicFirstFold(),
                product.scientificEvidencePack(),
                product.completionOffer());
    }

    /** Garante compatibilidade para contratos antigos do Hub que ainda não declaram layout. */
    private ProductExperienceResponse applyDefaultLayout(ProductExperienceResponse product) {
        if (StringUtils.hasText(product.layoutKey())) {
            return product;
        }
        return new ProductExperienceResponse(
                product.slug(),
                product.experienceVersion(),
                layoutKeyForExperienceVersion(product.experienceVersion()),
                product.funnelVersion(),
                product.name(),
                product.promise(),
                product.audience(),
                product.priceLabel(),
                product.theme(),
                product.diagnostic(),
                product.missions(),
                product.supportMaterials(),
                product.heroVideos(),
                product.publicDiagnosticQuestions(),
                product.publicFirstFold(),
                product.scientificEvidencePack(),
                product.completionOffer());
    }

    /** Deriva a chave de layout conhecida a partir da versão quando o contrato for legado. */
    private static String layoutKeyForExperienceVersion(String experienceVersion) {
        if (!StringUtils.hasText(experienceVersion)) {
            return "video-explicativo";
        }
        String normalized = experienceVersion.toLowerCase();
        if (normalized.contains("video-motivacional")) {
            return "video-motivacional";
        }
        if (normalized.contains("espelho-antes-de-sair")) {
            return "espelho-antes-de-sair";
        }
        if (normalized.contains("estrada-desejo")) {
            return "estrada-desejo";
        }
        return "video-explicativo";
    }

    /** Resolve a versão comercial esperada para subdomínios públicos versionados do MUSA. */
    private static String resolveHostExperienceVersion(String host) {
        if (!StringUtils.hasText(host)) {
            return "";
        }
        String normalizedHost = host.split(":", 2)[0].trim().toLowerCase();
        return MUSA_VERSIONED_HOST_EXPERIENCES.getOrDefault(normalizedHost, "");
    }

    /** Carrega o contrato PDE publicado pelo Marketing Hub quando a integração estiver configurada. */
    private Optional<ProductExperienceResponse> loadMarketingHubProduct(
            String slug,
            String host,
            String requestedSlotCode,
            String requestedExperienceVersion) {
        if (marketingHubBaseUrls.isEmpty()) {
            return Optional.empty();
        }
        String slotCode = StringUtils.hasText(requestedSlotCode) ? requestedSlotCode.trim() : resolveHostSlotCode(host);
        String experienceVersion = StringUtils.hasText(requestedExperienceVersion) ? requestedExperienceVersion.trim() : "";
        for (String baseUrl : marketingHubBaseUrls) {
            try {
                ProductExperienceResponse product = restClientBuilder.clone()
                        .baseUrl(baseUrl)
                        .build()
                        .get()
                        .uri(uriBuilder -> {
                            var builder = uriBuilder.path("/api/products/public/{slug}/pde-experience");
                            if (StringUtils.hasText(slotCode)) {
                                builder.queryParam("slotCode", slotCode);
                            } else if (StringUtils.hasText(experienceVersion)) {
                                builder.queryParam("experienceVersion", experienceVersion);
                            }
                            return builder.build(slug);
                        })
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

    /** Extrai o código do slot produtivo a partir do hostname público versionado. */
    private static String resolveHostSlotCode(String host) {
        if (!StringUtils.hasText(host)) {
            return "";
        }
        String normalizedHost = host.split(":", 2)[0].trim().toLowerCase();
        int dotIndex = normalizedHost.indexOf('.');
        String candidate = dotIndex > 0 ? normalizedHost.substring(0, dotIndex) : normalizedHost;
        return candidate.matches("v\\d+") ? candidate : "";
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
                MUSA_V5_EXPERIENCE_VERSION,
                "video-explicativo",
                "musa-membership-funnel-v1",
                "Método MUSA - Experiência Guiada de 7 Dias",
                "Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro.",
                "Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.",
                MUSA_PRICE_LABEL,
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
                List.of(
                        new HeroVideoDto(
                                MUSA_V6_EXPERIENCE_VERSION,
                                "public_diagnostic_initial_explainer",
                                "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
                                "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
                                null,
                                false,
                                false,
                                true,
                                false,
                                true,
                                "MARKETING_HUB_MANAGED_HLS",
                                1935L,
                                22L,
                                35L,
                                20462L,
                                "APPROVED",
                                "READY")),
                null,
                null,
                createMusaScientificEvidencePack(),
                "Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais de presença, estilo e autocuidado acessível.");
    }

    /** Cria o fallback local da v7 científica quando o Marketing Hub estiver indisponível. */
    private static ProductExperienceResponse createMusaScientificV7Product() {
        return new ProductExperienceResponse(
                "metodo-musa-7-dias",
                MUSA_V7_EXPERIENCE_VERSION,
                "espelho-antes-de-sair",
                "musa-membership-funnel-v1",
                "Método MUSA - Semana dos 7 Sinais de Presença",
                "Descubra o idioma silencioso da sua imagem e organize em 7 dias os sinais que fazem sua presença parecer mais elegante, intencional e coerente usando o que você já tem.",
                "Mulheres urbanas que querem elevar presença, cuidado percebido e segurança visual com ciência traduzida em microações acessíveis.",
                MUSA_PRICE_LABEL,
                new ThemeDto("#7a2444", "#d6a75c", "#fff8f3", "/assets/musa-cover.png"),
                new DiagnosticDto(
                        "Mapa Científico de Presença MUSA",
                        "Responda 4 escolhas rápidas. O MUSA identifica qual sinal visual pode estar enfraquecendo sua presença e mostra como a semana de 7 dias organiza isso com base nos artigos do produto.",
                        List.of(
                                "Que mensagem sua imagem comunica sem intenção?",
                                "Em qual cena sua primeira impressão importa mais agora?",
                                "Qual sinal de presença você quer reforçar nesta semana?")),
                List.of(
                        new MissionDto(
                                "dia-1-ruido-visual",
                                1,
                                "O espelho não vê roupa, vê mensagem",
                                "A vestimenta participa da percepção de pessoa: antes de explicar quem você é, sua imagem já envia sinais.",
                                "Vista uma combinação real e escreva a mensagem que ela parece transmitir hoje. Depois escolha um ajuste pequeno para aproximar essa mensagem da mulher que você quer comunicar.",
                                "Frase preenchida: hoje minha imagem parece dizer...",
                                "Observe roupa, cabelo, pele e detalhe final como um conjunto de sinais, não como peças soltas."),
                        new MissionDto(
                                "dia-2-assinatura",
                                2,
                                "A peça que muda seu estado interno",
                                "A cognição vestida sugere que uma peça ganha força quando carrega significado para a pessoa e para a situação.",
                                "Escolha uma peça-sinal que represente presença, cuidado ou elegância para você. Use essa peça em uma cena simples do dia e registre como ela muda sua postura diante do espelho.",
                                "Peça-sinal escolhida com o significado que ela carrega.",
                                "Priorize significado e intenção, não preço ou marca."),
                        new MissionDto(
                                "dia-3-base-acessivel",
                                3,
                                "Formalidade sem rigidez",
                                "Sinais de estrutura e formalidade podem alterar a leitura de prontidão, presença e adequação ao contexto.",
                                "Eleve uma combinação comum com um sinal de estrutura: terceira peça, tecido mais firme, sapato, cabelo alinhado ou acabamento melhor definido.",
                                "Antes/depois registrado com o detalhe que deixou o look mais intencional.",
                                "Procure o ponto em que o visual fica mais pronto sem parecer duro."),
                        new MissionDto(
                                "dia-4-checklist-12-minutos",
                                4,
                                "Primeiras impressões são leituras rápidas",
                                "Roupa e acabamento participam dos primeiros julgamentos sociais e profissionais, especialmente quando a pessoa ainda falou pouco.",
                                "Escolha uma situação real dos próximos dias e monte o primeiro sinal que você quer transmitir ao entrar: calma, cuidado, presença, feminilidade ou segurança.",
                                "Cena escolhida com o primeiro sinal planejado.",
                                "Imagine a primeira leitura do ambiente antes de escolher o detalhe final."),
                        new MissionDto(
                                "dia-5-compra-inteligente",
                                5,
                                "Cor como direção, não decoração",
                                "Cores carregam significado contextual e podem orientar percepção, sensação e coerência visual.",
                                "Escolha uma cor-base e uma cor-sinal para comunicar calma, presença, suavidade ou força sem exagerar.",
                                "Paleta de 2 cores registrada para uma ocasião real.",
                                "Use a cor como guia de intenção, não como enfeite isolado."),
                        new MissionDto(
                                "dia-6-situacao-chave",
                                6,
                                "Assinatura pessoal: ser reconhecida sem esforço",
                                "Moda e escolhas repetidas ajudam a organizar autoconceito, autoapresentação e identidade social.",
                                "Defina 3 sinais repetíveis da sua presença: cabelo, cor, acessório, perfume, textura, maquiagem leve ou acabamento.",
                                "Três sinais de assinatura MUSA definidos.",
                                "Elegância fica mais fácil quando vira repetição inteligente."),
                        new MissionDto(
                                "dia-7-plano-pessoal",
                                7,
                                "Seu algoritmo de presença elegante",
                                "Como escolhas de roupa dependem de pessoa, contexto e preferência, a orientação precisa ser personalizada e aplicável à rotina real.",
                                "Transforme suas respostas da semana em uma fórmula pessoal: sinais, ocasiões, regra anti-compra impulsiva e checklist antes de sair.",
                                "Fórmula MUSA pessoal preenchida para repetir por 30 dias.",
                                "Feche a semana com um jeito seu de se arrumar com menos dúvida.")),
                List.of(
                        new SupportMaterialDto(
                                "Mapa dos 7 Sinais MUSA",
                                "HTML",
                                "Resumo da semana científica com conceito, missão e aplicação prática de cada dia.",
                                "/materials/experiencia-guiada-musa.html"),
                        new SupportMaterialDto(
                                "Checklist Antes de Sair",
                                "PDF",
                                "Checklist para revisar mensagem visual, peça-sinal, estrutura, cor, acabamento e assinatura pessoal.",
                                "/materials/metodo-musa-ebook.pdf"),
                        new SupportMaterialDto(
                                "Fórmula MUSA Pessoal",
                                "CSV",
                                "Template para registrar sinais repetíveis, ocasiões e regra anti-compra impulsiva.",
                                "/materials/plano-checklists-e-templates.csv"),
                        new SupportMaterialDto(
                                "Mapa Visual MUSA",
                                "Infográfico",
                                "Resumo visual do método: coerência, redução de ruído e assinatura pessoal.",
                                "/materials/mapa-visual-musa.png")),
                List.of(),
                null,
                new ProductExperienceResponse.PublicFirstFoldDto(
                        "Sua roupa fala antes de você. Ela está falando com roteiro?",
                        "A nova Semana dos 7 Sinais transforma artigos científicos sobre roupa, percepção e autopercepção em microações simples para sua presença elegante.",
                        "Método MUSA em 7 dias",
                        "Veja como cada dia usa uma ideia dos artigos do MUSA para organizar sua imagem sem comprar um guarda-roupa novo.",
                        "Mensagem visual, peça-sinal, formalidade leve, primeira impressão, cor, assinatura pessoal e fórmula MUSA viram uma jornada prática de 7 dias.",
                        "Depois do vídeo, responda 4 escolhas rápidas para receber o primeiro sinal que pode estar apagando sua presença hoje.",
                        "Ver meu plano científico MUSA de 7 dias"),
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
