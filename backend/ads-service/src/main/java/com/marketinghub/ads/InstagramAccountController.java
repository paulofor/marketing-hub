package com.marketinghub.ads;

import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/instagram")
public class InstagramAccountController {
    private final InstagramAccountRepository repository;

    public InstagramAccountController(InstagramAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<InstagramAccount> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public InstagramAccount create(@RequestBody InstagramAccount account) {
        validateRequiredFields(account);
        return repository.save(account);
    }

    @PutMapping("/{id}")
    public InstagramAccount update(@PathVariable Long id, @RequestBody InstagramAccount account) {
        InstagramAccount existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        validateRequiredFields(account);

        existing.setName(account.getName());
        existing.setHandle(account.getHandle());
        existing.setCode(account.getCode());

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private void validateRequiredFields(InstagramAccount account) {
        if (!StringUtils.hasText(account.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório");
        }
        if (!StringUtils.hasText(account.getHandle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário (@) é obrigatório");
        }
        if (!StringUtils.hasText(account.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código é obrigatório");
        }
    }
}
