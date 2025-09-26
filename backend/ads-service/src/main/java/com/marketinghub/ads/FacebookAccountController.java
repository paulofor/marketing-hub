package com.marketinghub.ads;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/facebook")
public class FacebookAccountController {
    private final FacebookAccountRepository repository;

    public FacebookAccountController(FacebookAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FacebookAccount> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public FacebookAccount create(@RequestBody FacebookAccount account) {
        return repository.save(account);
    }

    @PutMapping("/{id}")
    public FacebookAccount update(@PathVariable Long id, @RequestBody FacebookAccount account) {
        account.setId(id);
        return repository.save(account);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
