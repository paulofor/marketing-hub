package com.marketinghub.businessprocess.document.controller;

import com.marketinghub.businessprocess.document.service.BusinessProcessActivityDocumentService;
import com.marketinghub.businessprocess.document.service.recentDocuments.BusinessProcessActivityDocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor os documentos recentes produzidos pelas atividades BPM. */
@Tag(
    name = "Processos — documentos das atividades",
    description =
        "Consulta auditável dos últimos documentos gerados por tarefas de cada atividade.")
@RestController
@RequestMapping("/api/business-processes/{processDefinitionId}")
public class BusinessProcessActivityDocumentController {
  private final BusinessProcessActivityDocumentService service;

  /** Configura o serviço de leitura segregada dos documentos BPM. */
  public BusinessProcessActivityDocumentController(BusinessProcessActivityDocumentService service) {
    this.service = service;
  }

  /** Lista as atividades que já produziram documentos concluídos. */
  @Operation(summary = "Lista atividades com documentos gerados")
  @GetMapping("/document-activities")
  public List<String> documentActivityIds(@PathVariable Long processDefinitionId) {
    return service.documentActivityIds(processDefinitionId);
  }

  /** Retorna somente os dez documentos mais recentes do processo inteiro. */
  @Operation(summary = "Lista os dez documentos mais recentes do processo")
  @GetMapping("/documents")
  public List<BusinessProcessActivityDocumentResponse> recentProcessDocuments(
      @PathVariable Long processDefinitionId) {
    return service.recentProcessDocuments(processDefinitionId);
  }

  /** Retorna somente os dez documentos mais recentes de uma atividade. */
  @Operation(summary = "Lista os dez documentos mais recentes de uma atividade")
  @GetMapping("/activities/{activityId}/documents")
  public List<BusinessProcessActivityDocumentResponse> recentDocuments(
      @PathVariable Long processDefinitionId, @PathVariable String activityId) {
    return service.recentDocuments(processDefinitionId, activityId);
  }
}
