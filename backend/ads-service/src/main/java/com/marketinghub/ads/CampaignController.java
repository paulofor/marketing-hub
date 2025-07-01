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

    @GetMapping("/accounts/facebook/{id}/campaigns")
    public List<Campaign> listFbCampaigns(@PathVariable Long id) {
        return campaignRepo.findByFacebookAccountId(id);
    }

    @PostMapping("/accounts/facebook/{id}/campaigns")
    public Campaign createFbCampaign(@PathVariable Long id, @RequestBody Campaign campaign) {
        FacebookAccount account = fbRepo.findById(id).orElseThrow();
        campaign.setFacebookAccount(account);
        campaign.setInstagramAccount(null);
        return campaignRepo.save(campaign);
    }

    @GetMapping("/accounts/instagram/{id}/campaigns")
    public List<Campaign> listIgCampaigns(@PathVariable Long id) {
        return campaignRepo.findByInstagramAccountId(id);
    }

    @PostMapping("/accounts/instagram/{id}/campaigns")
    public Campaign createIgCampaign(@PathVariable Long id, @RequestBody Campaign campaign) {
        InstagramAccount account = igRepo.findById(id).orElseThrow();
        campaign.setInstagramAccount(account);
        campaign.setFacebookAccount(null);
        return campaignRepo.save(campaign);
    }
}
