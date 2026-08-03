package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.dto.SalesVideoStudioCaptionPresetDto;
import com.marketinghub.salesvideo.dto.SalesVideoStudioCatalogDto;
import com.marketinghub.salesvideo.dto.SalesVideoStudioCharacterDto;
import java.util.List;

/**
 * Responsabilidade: fornecer catalogos comerciais internos usados pelo Estudio de Audio e Video.
 */
public class SalesVideoStudioCatalogService {

  /** Lista personagens e estilos de legenda aprovados para decisao operacional no estudio. */
  public SalesVideoStudioCatalogDto getCatalog() {
    return new SalesVideoStudioCatalogDto(listCharacters(), listCaptionPresets());
  }

  /** Lista personagens com status visual definidos pelo produto MUSA. */
  private List<SalesVideoStudioCharacterDto> listCharacters() {
    return List.of(
        new SalesVideoStudioCharacterDto(
            "musa-natural-editorial",
            "Mulher urbana natural",
            "Aprovado",
            "/assets/musa-editorial-presenca.png",
            "Boa para a v7: elegante, acessivel, cotidiana e coerente com espelho,"
                + " guarda-roupa e microacoes.",
            "Usar quando o video precisa vender presenca elegante sem parecer avatar ou luxo"
                + " inacessivel.",
            "Personagem aprovada: mulher urbana brasileira adulta, natural, elegante sem"
                + " ostentacao, roupa simples com acabamento bonito, expressao leve e postura"
                + " confiante. Priorizar cenas no espelho, detalhes de ajuste e gestos"
                + " cotidianos. Evitar objetos fixos estranhos, marcas de luxo, pose travada e"
                + " aparencia de apresentadora artificial."),
        new SalesVideoStudioCharacterDto(
            "musa-diagnostic-soft",
            "Presenca MUSA diagnostico",
            "Aprovado",
            "/assets/musa-cover.png",
            "Boa para planos de capa, diagnostico e transicao para o Plano MUSA de 7 dias.",
            "Usar como apoio visual quando a cena precisa lembrar a experiencia do produto.",
            "Personagem/visual de apoio aprovado: presenca feminina MUSA com acabamento"
                + " editorial, paleta vinho, creme e blush, energia intima e aspiracional. Usar"
                + " como referencia de capa, diagnostico e CTA; nao substituir a cena principal"
                + " da mulher real diante do espelho."),
        new SalesVideoStudioCharacterDto(
            "sofia-cabides-rejected",
            "Sofia com cabides",
            "Reprovado",
            "/assets/musa-diagnostic-slide-2.png",
            "Nao usar na v7: a pose fixa com cabides deixa o video artificial e enfraquece a"
                + " cena do espelho.",
            "Asset removido da lista reutilizavel porque segurava dois cabides o tempo todo.",
            "Personagem reprovada para novos videos MUSA: Sofia/apresentadora com cabides ou"
                + " pose fixa de closet. Motivo: parece artificial, nao entrega a cena da mulher"
                + " urbana diante do espelho e reduz naturalidade comercial. Nao reutilizar como"
                + " asset do produto."));
  }

  /** Lista presets de legenda para leitura e conversao em mobile. */
  private List<SalesVideoStudioCaptionPresetDto> listCaptionPresets() {
    return List.of(
        new SalesVideoStudioCaptionPresetDto(
            "mobile-high-conversion",
            "Legenda alta conversao mobile",
            "Texto grande, 2 linhas, contraste alto",
            "Melhor para Reels, TikTok, Shorts e hero mobile quando o video precisa vender sem"
                + " audio.",
            "Preset de legenda: alta conversao mobile. Usar texto grande em ate 2 linhas, area"
                + " segura central-baixa, fundo translucido vinho/grafite, palavras-chave em"
                + " dourado acessivel, contraste alto e leitura confortavel em tela pequena."
                + " Queimar legendas na montagem final."),
        new SalesVideoStudioCaptionPresetDto(
            "musa-editorial",
            "Legenda editorial MUSA",
            "Elegante, premium e legivel",
            "Boa para hero do PDE: sofisticada sem ficar pequena ou decorativa demais.",
            "Preset de legenda: editorial MUSA. Usar tipografia limpa, tamanho medio-grande,"
                + " cor creme, destaque vinho/dourado para palavras-chave, margem segura mobile e"
                + " ritmo de frases curtas. Evitar legenda pequena, fina ou simples demais."),
        new SalesVideoStudioCaptionPresetDto(
            "reels-highlight",
            "Legenda Reels com destaque",
            "Palavras-chave maiores",
            "Boa para criativos de trafego frio, com destaque visual em dor, mecanismo e CTA.",
            "Preset de legenda: Reels com destaque. Usar blocos curtos com palavras-chave"
                + " maiores para espelho, 7 dias, ruido visual, peca-sinal e diagnostico. Manter"
                + " leitura rapida e preservar area do rosto."));
  }
}
