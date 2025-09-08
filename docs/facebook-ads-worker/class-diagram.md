# Facebook Ads Worker Class Diagram

```mermaid
classDiagram
    class FacebookAdsWorkerApplication
    class FacebookAdsService
    class InstagramCampaignService
    class InstagramCampaignScheduler
    class FacebookCampaignService
    class FacebookCampaignScheduler

    FacebookAdsWorkerApplication --> FacebookAdsService
    InstagramCampaignService --> FacebookAdsService
    InstagramCampaignScheduler --> InstagramCampaignService
    FacebookCampaignService --> FacebookAdsService
    FacebookCampaignScheduler --> FacebookCampaignService
```
