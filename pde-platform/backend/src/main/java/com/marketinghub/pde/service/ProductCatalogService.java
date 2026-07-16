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
                "Monte em 7 dias uma presenca mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformacao radical.",
                "Mulheres urbanas que querem se sentir mais marcantes, alinhadas e seguras usando escolhas acessiveis.",
                "R$47",
                new ThemeDto("#7a2444", "#d6a75c", "#fff8f3", "/assets/musa-cover.png"),
                new DiagnosticDto(
                        "Diagnostico MUSA",
                        "Comece pelo momento do espelho: quando voce esta pronta, mas sente que ainda falta presenca, acabamento ou intencao.",
                        List.of(
                                "Quando voce se ve pronta, o que faz pensar: esta ok, mas ainda nao esta marcante?",
                                "Seu cabelo, pele, roupa, perfume e acessorios parecem conversar entre si?",
                                "Qual compra voce esta quase fazendo para tentar compensar essa sensacao?",
                                "Em qual situacao dos proximos 7 dias voce quer entrar com mais presenca?")),
                List.of(
                        new MissionDto(
                                "dia-1-ruido-visual",
                                1,
                                "Sair do quase bom",
                                "A presenca cresce quando voce identifica o detalhe que mais apaga o conjunto.",
                                "Escolha uma combinacao real e marque o que hoje gera ruido: excesso, falta de acabamento, cor solta, peca sem intencao ou acessorio perdido.",
                                "Frase preenchida: eu me sinto arrumada, mas pouco marcante quando...",
                                "Compare a sensacao antes/depois de remover ou ajustar um detalhe."),
                        new MissionDto(
                                "dia-2-assinatura",
                                2,
                                "Criar sua assinatura simples",
                                "Coerencia repetida cria reconhecimento sem exigir roupa nova.",
                                "Defina 3 sinais que voce quer repetir: acabamento do cabelo, cor-base, textura, perfume, acessorio ou maquiagem leve.",
                                "Lista dos 3 sinais escolhidos.",
                                "Monte um pequeno painel com seus sinais recorrentes."),
                        new MissionDto(
                                "dia-3-base-acessivel",
                                3,
                                "Usar o que ja existe melhor",
                                "A mudanca fica mais viavel quando comeca pelo que ja esta no armario.",
                                "Separe 5 pecas, 2 acessorios e 1 perfume que ja podem sustentar a presenca desejada.",
                                "Inventario simples dos itens reaproveitados.",
                                "Organize os itens em uma combinacao para uma saida real."),
                        new MissionDto(
                                "dia-4-checklist-12-minutos",
                                4,
                                "Fazer o acabamento de 12 minutos",
                                "Rotinas curtas reduzem atrito e aumentam consistencia.",
                                "Passe pelo checklist cabelo, pele, roupa, perfume, acessorio e postura antes de sair.",
                                "Checklist marcado com o tempo gasto.",
                                "Use uma escala de 1 a 5 para medir coerencia final."),
                        new MissionDto(
                                "dia-5-compra-inteligente",
                                5,
                                "Segurar a compra que nao resolve",
                                "Compra boa e aquela que fortalece sua assinatura, nao a que compensa inseguranca momentanea.",
                                "Antes de comprar algo, responda se o item combina com seus 3 sinais de presenca.",
                                "Decisao registrada: comprar, esperar ou descartar.",
                                "Compare desejo imediato com utilidade real na sua rotina."),
                        new MissionDto(
                                "dia-6-situacao-chave",
                                6,
                                "Preparar sua entrada",
                                "A presenca fica mais forte quando e planejada para contextos reais.",
                                "Escolha uma ocasiao e monte uma composicao completa com intencao: roupa, cabelo, pele, perfume e detalhe final.",
                                "Plano da ocasiao com roupa, cabelo, pele, perfume e detalhe final.",
                                "Visualize a entrada no ambiente e ajuste o que estiver incoerente."),
                        new MissionDto(
                                "dia-7-plano-pessoal",
                                7,
                                "Fechar seu antes e depois",
                                "A transformacao continua quando vira padrao simples de repeticao.",
                                "Monte seu plano de manutencao com sinais, checklist e regra anti-impulso.",
                                "Plano pessoal preenchido.",
                                "Escolha um ritual semanal de 15 minutos para manter sua presenca.")),
                List.of(
                        new SupportMaterialDto(
                                "E-book Metodo MUSA",
                                "PDF",
                                "Guia de consulta para entender o metodo, ver exemplos e revisar sua semana.",
                                "/materials/metodo-musa-ebook.pdf"),
                        new SupportMaterialDto(
                                "Experiencia Guiada MUSA",
                                "HTML",
                                "Versao navegavel da experiencia para consultar a ordem, o diagnostico e as missoes de 7 dias.",
                                "/materials/experiencia-guiada-musa.html"),
                        new SupportMaterialDto(
                                "Plano, Checklists e Templates",
                                "CSV",
                                "Planilha com a ordem de aplicacao, criterios de conclusao e pontos de atencao de cada material.",
                                "/materials/plano-checklists-e-templates.csv"),
                        new SupportMaterialDto(
                                "Mapa Visual MUSA",
                                "Infografico",
                                "Resumo visual do metodo: coerencia, reducao de ruido e assinatura pessoal.",
                                "/materials/mapa-visual-musa.png")),
                "Ao concluir os 7 dias, voce pode continuar no Clube MUSA com novos desafios mensais de presenca, estilo e autocuidado acessivel.");
    }
}
