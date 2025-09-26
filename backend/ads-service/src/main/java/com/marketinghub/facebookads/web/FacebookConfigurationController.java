package com.marketinghub.facebookads.web;

import com.marketinghub.ads.FacebookPageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facebook")
public class FacebookConfigurationController {
    private final FacebookPageRepository pageRepository;

    public FacebookConfigurationController(FacebookPageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @GetMapping("/configuration-status")
    public FacebookConfigurationStatus configurationStatus() {
        boolean hasConfiguredPages = pageRepository.count() > 0;
        return new FacebookConfigurationStatus(hasConfiguredPages);
    }

    public record FacebookConfigurationStatus(boolean hasConfiguredPages) {}
}
