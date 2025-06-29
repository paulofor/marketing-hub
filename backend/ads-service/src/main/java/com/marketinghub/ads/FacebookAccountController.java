package com.marketinghub.ads;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
