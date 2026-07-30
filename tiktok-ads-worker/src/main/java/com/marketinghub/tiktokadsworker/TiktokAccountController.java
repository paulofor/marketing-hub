package com.marketinghub.tiktokadsworker;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos HTTP do módulo TikTok Ads para o frontend administrativo. */
@RestController
@RequestMapping("/api/tiktok/accounts")
public class TiktokAccountController {
    private final TiktokAccountService service;

    /** Inicializa o controller com o serviço de contas TikTok. */
    public TiktokAccountController(TiktokAccountService service) {
        this.service = service;
    }

    /** Lista todas as contas TikTok cadastradas. */
    @GetMapping
    public List<TiktokAccountResponse> listAccounts() {
        return service.listAccounts();
    }

    /** Cadastra uma nova conta TikTok Ads. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TiktokAccountResponse createAccount(@Valid @RequestBody TiktokAccountRequest request) {
        return service.createAccount(request);
    }

    /** Atualiza uma conta TikTok Ads existente. */
    @PutMapping("/{id}")
    public TiktokAccountResponse updateAccount(@PathVariable Long id, @Valid @RequestBody TiktokAccountRequest request) {
        return service.updateAccount(id, request);
    }

    /** Remove uma conta TikTok Ads existente. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Long id) {
        service.deleteAccount(id);
    }

    /** Executa diagnóstico operacional da conta TikTok Ads. */
    @PostMapping("/{id}/diagnostics")
    public TiktokDiagnosticResponse diagnoseAccount(@PathVariable Long id) {
        return service.diagnoseAccount(id);
    }

    /** Converte ausência de conta em erro HTTP claro para a tela. */
    @ExceptionHandler(TiktokAccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(TiktokAccountNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    /** Contrato simples de erro para respostas do módulo. */
    public record ErrorResponse(String message) {
    }
}
