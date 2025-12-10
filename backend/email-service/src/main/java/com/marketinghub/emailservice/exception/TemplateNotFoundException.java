package com.marketinghub.emailservice.exception;

public class TemplateNotFoundException extends EmailServiceException {
    public TemplateNotFoundException(String templateId) {
        super("Template não encontrado no Marketing Hub: " + templateId);
    }
}
