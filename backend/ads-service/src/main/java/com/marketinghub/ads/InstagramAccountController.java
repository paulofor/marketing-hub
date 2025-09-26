package com.marketinghub.ads;

import org.springframework.web.bind.annotation.*;

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
        return repository.save(account);
    }

    @PutMapping("/{id}")
    public InstagramAccount update(@PathVariable Long id, @RequestBody InstagramAccount account) {
        account.setId(id);
        return repository.save(account);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
