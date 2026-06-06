package com.marketinghub.facebookads.controller;

import com.marketinghub.ads.FacebookInterest;
import com.marketinghub.ads.FacebookInterestStatus;

import com.marketinghub.repository.jpa.ads.FacebookInterestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Agrupa endpoints de interesses Facebook pendentes e normalizados para segmentação.
 */
@RestController
@RequestMapping("/api/facebook-interests")
public class FacebookInterestController {
    private final FacebookInterestRepository repository;

    // Executa a operação FacebookInterestController da integração Facebook Ads.
    public FacebookInterestController(FacebookInterestRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/pending")
    // Executa a operação findPendingInterests da integração Facebook Ads.
    public List<PendingInterestResponse> findPendingInterests() {
        return repository
            .findByStatusAndFacebookInterestIdIsNull(FacebookInterestStatus.PENDING)
            .stream()
            .map(interest -> new PendingInterestResponse(interest.getId(), interest.getName()))
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // Executa a operação create da integração Facebook Ads.
    public FacebookInterest create(@RequestBody CreateFacebookInterestRequest request) {
        if (!StringUtils.hasText(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }

        FacebookInterest interest = FacebookInterest
            .builder()
            .name(request.name().trim())
            .status(FacebookInterestStatus.PENDING)
            .model(request.model())
            .prompt(request.prompt())
            .build();

        return repository.save(interest);
    }

    @PatchMapping("/{id}")
    public FacebookInterest update(
        @PathVariable Long id,
        @RequestBody FacebookInterestUpdateRequest request
    ) {
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        FacebookInterest interest = repository.findById(id).orElseThrow();

        if (request.status() == FacebookInterestStatus.VALID && !StringUtils.hasText(request.facebookInterestId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "facebookInterestId is required when status is VALID"
            );
        }

        interest.setStatus(request.status());

        if (request.status() == FacebookInterestStatus.INVALID) {
            interest.setFacebookInterestId(null);
        } else if (request.facebookInterestId() != null) {
            interest.setFacebookInterestId(StringUtils.trimWhitespace(request.facebookInterestId()));
        }

        if (request.name() != null) {
            String normalizedName = request.name().trim();
            if (normalizedName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
            }
            interest.setName(normalizedName);
        }

        return repository.save(interest);
    }

    public record PendingInterestResponse(Long id, String name) {}

    public record FacebookInterestUpdateRequest(
        FacebookInterestStatus status,
        String facebookInterestId,
        String name
    ) {}

    public record CreateFacebookInterestRequest(String name, String model, String prompt) {}
}
