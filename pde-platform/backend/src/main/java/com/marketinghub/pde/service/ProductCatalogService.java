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
                "Metodo MUSA - Experiencia Guiada de 7 Dias",
                "Monte em 7 dias uma presenca mais elegante, marcante e coerente sem depender de luxo caro.",
                "Mulheres que querem parecer mais alinhadas, sofisticadas e seguras usando escolhas acessiveis.",
                "R$47",
                new ThemeDto("#7a2444", "#d6a75c", "#fff8f3", "/assets/musa-cover.svg"),
                new DiagnosticDto(
                        "Diagnostico MUSA",
                        "Identifique os pontos que hoje mais quebram a percepcao de presenca elegante antes de iniciar as missoes.",
                        List.of(
                                "Qual detalhe mais incomoda quando voce se ve pronta?",
                                "Seu cabelo, pele, roupa, perfume e acessorios parecem conversar entre si?",
                                "Voce compra itens por impulso ou segue uma intencao clara?",
                                "Qual situacao dos proximos 7 dias merece uma presenca mais marcante?")),
                List.of(
                        new MissionDto(
                                "dia-1-ruido-visual",
                                1,
                                "Remover ruido visual",
                                "A percepcao de elegancia melhora quando os sinais competem menos entre si.",
                                "Escolha uma combinacao real e remova um excesso: cor, brilho, volume, estampa ou acessorio.",
                                "Foto antes/depois ou anotacao do item removido.",
                                "Compare uma composicao carregada com uma composicao mais limpa."),
                        new MissionDto(
                                "dia-2-assinatura",
                                2,
                                "Criar assinatura de presenca",
                                "Coerencia repetida cria reconhecimento e reduz esforco de decisao.",
                                "Defina 3 sinais que voce quer repetir: cabelo, cor-base, textura, perfume ou acessorio.",
                                "Lista dos 3 sinais escolhidos.",
                                "Monte um pequeno painel com seus sinais recorrentes."),
                        new MissionDto(
                                "dia-3-base-acessivel",
                                3,
                                "Montar base acessivel",
                                "A transformacao fica mais viavel quando comeca pelo que ja existe no armario.",
                                "Separe 5 pecas, 2 acessorios e 1 perfume que ja sustentam a presenca desejada.",
                                "Inventario simples dos itens reaproveitados.",
                                "Organize os itens em uma combinacao de saida real."),
                        new MissionDto(
                                "dia-4-checklist-12-minutos",
                                4,
                                "Aplicar checklist de 12 minutos",
                                "Rotinas curtas reduzem atrito e aumentam consistencia.",
                                "Passe pelo checklist cabelo, pele, roupa, perfume, acessorio e postura antes de sair.",
                                "Checklist marcado com o tempo gasto.",
                                "Use uma escala de 1 a 5 para medir coerencia final."),
                        new MissionDto(
                                "dia-5-compra-inteligente",
                                5,
                                "Evitar compra por impulso",
                                "Compra boa e aquela que completa o sistema, nao a que compensa inseguranca momentanea.",
                                "Antes de comprar algo, responda se o item combina com seus 3 sinais de presenca.",
                                "Decisao registrada: comprar, esperar ou descartar.",
                                "Compare desejo imediato com utilidade real na sua rotina."),
                        new MissionDto(
                                "dia-6-situacao-chave",
                                6,
                                "Preparar uma situacao-chave",
                                "A presenca fica mais forte quando e planejada para contextos reais.",
                                "Escolha uma ocasiao e monte uma composicao completa com intencao.",
                                "Plano da ocasiao com roupa, cabelo, pele, perfume e detalhe final.",
                                "Visualize a entrada no ambiente e ajuste o que estiver incoerente."),
                        new MissionDto(
                                "dia-7-plano-pessoal",
                                7,
                                "Fechar seu plano pessoal MUSA",
                                "Transformacao continua quando vira padrao simples de repeticao.",
                                "Monte seu plano de manutencao com sinais, checklist e regra anti-impulso.",
                                "Plano pessoal preenchido.",
                                "Escolha um ritual semanal de 15 minutos para manter sua presenca.")),
                List.of(
                        new SupportMaterialDto(
                                "E-book Metodo MUSA",
                                "PDF",
                                "Guia premium com os principios aplicados e exemplos visuais da experiencia.",
                                "/materials/metodo-musa-ebook.pdf"),
                        new SupportMaterialDto(
                                "Checklist de Presenca em 12 Minutos",
                                "Checklist",
                                "Sequencia curta para revisar cabelo, pele, roupa, perfume e acessorios.",
                                "/materials/checklist-presenca-12-minutos.pdf"),
                        new SupportMaterialDto(
                                "Lista Anti-Impulso",
                                "Template",
                                "Modelo para decidir se uma compra fortalece sua presenca ou apenas parece urgente.",
                                "/materials/lista-anti-impulso.csv"),
                        new SupportMaterialDto(
                                "Mapa Visual MUSA",
                                "Infografico",
                                "Resumo visual do mecanismo: coerencia, reducao de ruido e assinatura pessoal.",
                                "/materials/mapa-visual-musa.png")),
                "Ao concluir os 7 dias, voce pode continuar no Clube MUSA com novos desafios mensais de presenca, estilo e autocuidado acessivel.");
    }
}
