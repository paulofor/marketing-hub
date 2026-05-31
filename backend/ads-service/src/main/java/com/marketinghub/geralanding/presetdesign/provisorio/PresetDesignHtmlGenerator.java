package com.marketinghub.geralanding.presetdesign.provisorio;

import org.springframework.util.StringUtils;

/** Responsável por preservar o HTML canônico já consolidado para a etapa presetdesign. */
public class PresetDesignHtmlGenerator {

    /** Retorna o HTML recebido quando ele contém texto útil. */
    public String generateFromHtml(String html) {
        return StringUtils.hasText(html) ? html : null;
    }
}
