package com.marketinghub.ads;

import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.marketinghub.repository.jpa.ads.FacebookPageRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/facebook/{accountId}/pages")
public class FacebookPageController {
    private final FacebookAccountRepository accountRepository;
    private final FacebookPageRepository pageRepository;
    private final com.marketinghub.repository.jpa.experiment.ExperimentRepository experimentRepository;

    public FacebookPageController(FacebookAccountRepository accountRepository,
                                  FacebookPageRepository pageRepository,
                                  com.marketinghub.repository.jpa.experiment.ExperimentRepository experimentRepository) {
        this.accountRepository = accountRepository;
        this.pageRepository = pageRepository;
        this.experimentRepository = experimentRepository;
    }

    @GetMapping
    public List<FacebookPageDto> list(@PathVariable Long accountId) {
        ensureAccountExists(accountId);
        return pageRepository.findByAccountId(accountId).stream()
                .map(FacebookPageController::toDto)
                .toList();
    }

    @PostMapping
    @Transactional
    public FacebookPageDto create(@PathVariable Long accountId,
                                  @RequestBody UpsertFacebookPageRequest request) {
        FacebookAccount account = ensureAccountExists(accountId);
        FacebookPage page = FacebookPage.builder()
                .account(account)
                .name(request.name())
                .pageId(normalize(request.pageId()))
                .build();
        return toDto(pageRepository.save(page));
    }

    @PutMapping("/{pageRecordId}")
    @Transactional
    public FacebookPageDto update(@PathVariable Long accountId,
                                  @PathVariable Long pageRecordId,
                                  @RequestBody UpsertFacebookPageRequest request) {
        ensureAccountExists(accountId);
        FacebookPage page = pageRepository.findById(pageRecordId)
                .filter(existing -> existing.getAccount().getId().equals(accountId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String normalizedPageId = normalize(request.pageId());
        page.setName(request.name());
        page.setPageId(normalizedPageId);
        FacebookPage saved = pageRepository.save(page);

        return toDto(saved);
    }

    @DeleteMapping("/{pageRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long accountId, @PathVariable Long pageRecordId) {
        ensureAccountExists(accountId);
        FacebookPage page = pageRepository.findById(pageRecordId)
                .filter(existing -> existing.getAccount().getId().equals(accountId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        experimentRepository.clearFacebookPageById(page.getId());
        pageRepository.delete(page);
    }

    private FacebookAccount ensureAccountExists(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static FacebookPageDto toDto(FacebookPage page) {
        return new FacebookPageDto(page.getId(), page.getAccount().getId(), page.getPageId(), page.getName());
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record FacebookPageDto(Long id, Long accountId, String pageId, String name) {}

    public record UpsertFacebookPageRequest(String pageId, String name) {}
}
