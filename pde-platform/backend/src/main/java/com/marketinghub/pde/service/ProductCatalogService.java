package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse.DiagnosticDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.MissionDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.SupportMaterialDto;
import com.marketinghub.pde.dto.ProductExperienceResponse.ThemeDto;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Mantém o catálogo configurável dos produtos experienciais disponíveis. */
@Service
public class ProductCatalogService {

    private final Map<String, ProductExperienceResponse> products = Map.of(
            "metodo-musa-7-dias", createMusaProduct());

    /** Retorna a experiência configurada para o produto informado. */
    public ProductExperienceResponse getProduct(String slug) {
        ProductExperienceResponse product = products.get(slug);
        if (product == null) {
            throw new IllegalArgumentException("Produto PDE nao encontrado: " + slug);
        }
        return product;
    }

    /** Cria a primeira configuração do produto MUSA para o experimento 66. */
    private static ProductExperienceResponse createMusaProduct() {
        return new ProductExperienceResponse(
                "metodo-musa-7-dias",
                "Método MUSA - Experiência Guiada de 7 Dias",
                "Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.",
                "Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessíveis.",
                "",
                new ThemeDto("#7a2444", "#d6a75c", "#fff8f3", "/assets/musa-cover.png"),
                new DiagnosticDto(
                        "Diagnóstico MUSA",
                        "Comece pelo momento do espelho: quando você está pronta, mas sente que ainda falta presença, acabamento ou intenção.",
                        List.of(
                                "Quando você se vê pronta, o que faz pensar: está ok, mas ainda não está marcante?",
                                "Seu cabelo, pele, roupa, perfume e acessórios parecem conversar entre si?",
                                "Qual compra você está quase fazendo para tentar compensar essa sensação?",
                                "Em qual situação dos próximos 7 dias você quer entrar com mais presença?")),
                List.of(
                        new MissionDto(
                                "dia-1-ruido-visual",
                                1,
                                "Sair do quase bom",
                                "A presença cresce quando você identifica o detalhe que mais apaga o conjunto.",
                                "Hoje você não vai tentar mudar tudo. Vista ou separe uma combinação real, olhe cabelo, pele, roupa, perfume e acessórios, escolha o detalhe que mais apaga sua presença e ajuste apenas esse ponto.",
                                "Frase preenchida: eu me sinto arrumada, mas pouco marcante quando...",
                                "Compare a sensação antes/depois de remover ou ajustar um detalhe."),
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
                "Ao concluir os 7 dias, você pode continuar no Clube MUSA com novos desafios mensais de presença, estilo e autocuidado acessível.");
    }
}
