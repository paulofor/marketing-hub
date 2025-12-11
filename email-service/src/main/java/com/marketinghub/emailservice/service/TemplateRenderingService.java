package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.exception.EmailServiceException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;

@Service
public class TemplateRenderingService {

    public String render(String templateBody, Map<String, Object> variables) {
        if (templateBody == null) {
            throw new EmailServiceException("Corpo do template não pode ser nulo");
        }

        Map<String, Object> safeVariables = variables != null ? new HashMap<>(variables) : Map.of();
        StringSubstitutor curlyBracesSubstitutor = new StringSubstitutor(safeVariables, "{{", "}}", '\\');
        curlyBracesSubstitutor.setEnableSubstitutionInVariables(true);

        String intermediate = curlyBracesSubstitutor.replace(templateBody);

        StringSubstitutor defaultSubstitutor = new StringSubstitutor(safeVariables);
        defaultSubstitutor.setEnableSubstitutionInVariables(true);
        return defaultSubstitutor.replace(intermediate);
    }
}
