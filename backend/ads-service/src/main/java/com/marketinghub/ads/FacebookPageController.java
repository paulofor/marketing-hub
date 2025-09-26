package com.marketinghub.ads;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/accounts/facebook/{accountId}/pages")
public class FacebookPageController {
    private final FacebookAccountRepository accountRepository;
    private final FacebookPageRepository pageRepository;

    public FacebookPageController(FacebookAccountRepository accountRepository,
                                  FacebookPageRepository pageRepository) {
        this.accountRepository = accountRepository;
        this.pageRepository = pageRepository;
    }

    @GetMapping
    public List<FacebookPageDto> list(@PathVariable Long accountId) {
        ensureAccountExists(accountId);
        return pageRepository.findByAccountId(accountId).stream()
                .map(FacebookPageController::toDto)
                .toList();
    }

    @PostMapping
    public FacebookPageDto create(@PathVariable Long accountId,
                                  @RequestBody UpsertFacebookPageRequest request) {
        FacebookAccount account = ensureAccountExists(accountId);
        FacebookPage page = FacebookPage.builder()
                .account(account)
                .name(request.name())
                .pageId(request.pageId())
                .build();
        return toDto(pageRepository.save(page));
    }

    @PutMapping("/{pageRecordId}")
    public FacebookPageDto update(@PathVariable Long accountId,
                                  @PathVariable Long pageRecordId,
                                  @RequestBody UpsertFacebookPageRequest request) {
        ensureAccountExists(accountId);
        FacebookPage page = pageRepository.findById(pageRecordId)
                .filter(existing -> existing.getAccount().getId().equals(accountId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        page.setName(request.name());
        page.setPageId(request.pageId());
        return toDto(pageRepository.save(page));
    }

    @DeleteMapping("/{pageRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long accountId, @PathVariable Long pageRecordId) {
        ensureAccountExists(accountId);
        FacebookPage page = pageRepository.findById(pageRecordId)
                .filter(existing -> existing.getAccount().getId().equals(accountId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        pageRepository.delete(page);
    }

    private FacebookAccount ensureAccountExists(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static FacebookPageDto toDto(FacebookPage page) {
        return new FacebookPageDto(page.getId(), page.getAccount().getId(), page.getPageId(), page.getName());
    }

    public record FacebookPageDto(Long id, Long accountId, String pageId, String name) {}

    public record UpsertFacebookPageRequest(String pageId, String name) {}
}
