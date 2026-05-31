package com.marketinghub.geralanding.presetdesign.provisorio;

import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlAssembler;
import org.springframework.stereotype.Component;

/** Responsável por expor o montador de HTML provisório dentro do pacote isolado presetdesign. */
@Component
public class PresetDesignProvisionalHtmlAssembler {

    private final DesignPresetProvisionalHtmlAssembler delegate;

    /** Inicializa o adaptador com o montador canônico existente da etapa design preset. */
    public PresetDesignProvisionalHtmlAssembler(DesignPresetProvisionalHtmlAssembler delegate) {
        this.delegate = delegate;
    }

    /** Monta o HTML provisório quando o retorno da etapa já contém o HTML canônico. */
    public String assemble(String designPresetOutput, String jobId) {
        return delegate.assemble(designPresetOutput, jobId);
    }

    /** Monta o HTML provisório consolidando wireframe, copy, image planning e presetdesign. */
    public String assemble(
            String wireframeJson,
            String copyJson,
            String imagePlanningJson,
            String designPresetOutputJson,
            String jobId) {
        return delegate.assemble(wireframeJson, copyJson, imagePlanningJson, designPresetOutputJson, jobId);
    }
}
