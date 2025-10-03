package com.marketinghub.ads;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/accounts/facebook/{accountId}/pages")
public class FacebookPageController {
    private final FacebookAccountRepository accountRepository;
    private final FacebookPageRepository pageRepository;
    private final com.marketinghub.experiment.repository.ExperimentRepository experimentRepository;

    public FacebookPageController(FacebookAccountRepository accountRepository,
                                  FacebookPageRepository pageRepository,
                                  com.marketinghub.experiment.repository.ExperimentRepository experimentRepository) {
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

        String previousPageId = page.getPageId();
        String normalizedPageId = normalize(request.pageId());
        page.setName(request.name());
        page.setPageId(normalizedPageId);
        FacebookPage saved = pageRepository.save(page);

        if (StringUtils.hasText(previousPageId) && !Objects.equals(previousPageId, normalizedPageId)) {
            if (!StringUtils.hasText(normalizedPageId)) {
                experimentRepository.clearPageIdByValue(previousPageId);
            } else {
                experimentRepository.updatePageIdByValue(previousPageId, normalizedPageId);
            }
        }

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
        String previousPageId = page.getPageId();
        pageRepository.delete(page);
        if (StringUtils.hasText(previousPageId)) {
            experimentRepository.clearPageIdByValue(previousPageId);
        }
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
