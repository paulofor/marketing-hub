package com.marketinghub.ads;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.InstagramAccount;

@RestController
public class CampaignController {
    private final FacebookAccountRepository fbRepo;
    private final InstagramAccountRepository igRepo;
    private final CampaignRepository campaignRepo;

    public CampaignController(FacebookAccountRepository fbRepo, InstagramAccountRepository igRepo, CampaignRepository campaignRepo) {
        this.fbRepo = fbRepo;
        this.igRepo = igRepo;
        this.campaignRepo = campaignRepo;
    }

    @GetMapping("/api/accounts/{id}/campaigns")
    public List<Campaign> listCampaigns(@PathVariable Long id) {
        return campaignRepo.findByFacebookAccountIdOrInstagramAccountId(id, id);
    }

    @PostMapping("/api/accounts/{id}/campaigns")
    public Campaign createCampaign(@PathVariable Long id,
                                  @RequestParam("platform") String platform,
                                  @RequestBody Campaign campaign) {
        if ("facebook".equalsIgnoreCase(platform)) {
            FacebookAccount account = fbRepo.findById(id).orElseThrow();
            campaign.setFacebookAccount(account);
            campaign.setInstagramAccount(null);
        } else if ("instagram".equalsIgnoreCase(platform)) {
            InstagramAccount account = igRepo.findById(id).orElseThrow();
            campaign.setInstagramAccount(account);
            campaign.setFacebookAccount(null);
        } else {
            throw new IllegalArgumentException("platform must be facebook or instagram");
        }
        return campaignRepo.save(campaign);
    }
}
