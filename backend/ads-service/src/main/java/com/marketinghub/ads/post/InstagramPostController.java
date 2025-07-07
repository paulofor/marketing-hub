package com.marketinghub.ads.post;

import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.InstagramAccountRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts/instagram/{accountId}/posts")
public class InstagramPostController {
    private final InstagramAccountRepository accountRepository;
    private final InstagramPostRepository repository;

    public InstagramPostController(InstagramAccountRepository accountRepository, InstagramPostRepository repository) {
        this.accountRepository = accountRepository;
        this.repository = repository;
    }

    @GetMapping
    public List<InstagramPost> list(@PathVariable Long accountId) {
        return repository.findByAccountId(accountId);
    }

    @PostMapping
    public InstagramPost create(@PathVariable Long accountId, @RequestBody InstagramPost post) {
        InstagramAccount account = accountRepository.findById(accountId).orElseThrow();
        post.setAccount(account);
        return repository.save(post);
    }

    @PutMapping("/{postId}")
    public InstagramPost update(@PathVariable Long accountId, @PathVariable Long postId, @RequestBody InstagramPost post) {
        InstagramAccount account = accountRepository.findById(accountId).orElseThrow();
        post.setId(postId);
        post.setAccount(account);
        return repository.save(post);
    }

    @DeleteMapping("/{postId}")
    public void delete(@PathVariable Long postId) {
        repository.deleteById(postId);
    }
}
