package com.marketinghub.ads;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FacebookAccountController {
    private final FacebookAccountRepository repository;

    public FacebookAccountController(FacebookAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/accounts/facebook")
    public List<FacebookAccount> findAll() {
        return repository.findAll();
    }

    @PostMapping("/accounts/facebook")
    public FacebookAccount create(@RequestBody FacebookAccount account) {
        return repository.save(account);
    }

    @PutMapping("/accounts/facebook/{id}")
    public FacebookAccount update(@PathVariable Long id, @RequestBody FacebookAccount account) {
        account.setId(id);
        return repository.save(account);
    }

    @DeleteMapping("/accounts/facebook/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
