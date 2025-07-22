package com.marketinghub.experiment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Gera textos de anúncio utilizando IA.
 */
@Service
public class AdGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(AdGeneratorService.class);

    /** Resultado da geração do criativo. */
    public record AdCreative(String primaryText, String headline, String imageUrl) {}

    /**
     * Gera um criativo simples em pt-BR.
     */
    public AdCreative generate(String angle, String proof, String emotion) {
        log.debug("Gerando criativo para angle={}, proof={}, emotion={}", angle, proof, emotion);
        String headline = "Descubra " + angle;
        if (headline.length() > 40) headline = headline.substring(0, 40);
        String text = String.format(Locale.forLanguageTag("pt-BR"),
                "%s e %s despertam %s. Clique para saber mais!", angle, proof, emotion);
        if (text.length() > 125) text = text.substring(0, 125);
        String img = "https://img.example.com/" + angle.hashCode();
        return new AdCreative(text, headline, img);
    }
}
