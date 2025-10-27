package com.marketinghub.whatsapp.web;

import com.marketinghub.whatsapp.dto.WhatsAppAccountDto;
import com.marketinghub.whatsapp.dto.WhatsAppAccountRequest;
import com.marketinghub.whatsapp.mapper.WhatsAppAccountMapper;
import com.marketinghub.whatsapp.service.WhatsAppAccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for configuring WhatsApp accounts.
 */
@RestController
@RequestMapping("/api/whatsapp/accounts")
public class WhatsAppAccountController {
    private final WhatsAppAccountService accountService;
    private final WhatsAppAccountMapper accountMapper;

    public WhatsAppAccountController(WhatsAppAccountService accountService, WhatsAppAccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    public List<WhatsAppAccountDto> listAccounts() {
        return accountService.listAccounts().stream().map(accountMapper::toDto).toList();
    }

    @PostMapping
    public WhatsAppAccountDto createAccount(@RequestBody WhatsAppAccountRequest request) {
        return accountMapper.toDto(accountService.createAccount(request));
    }

    @PutMapping("/{id}")
    public WhatsAppAccountDto updateAccount(@PathVariable Long id, @RequestBody WhatsAppAccountRequest request) {
        return accountMapper.toDto(accountService.updateAccount(id, request));
    }
}
