# Facebook Ads Worker Class Diagram

```mermaid
classDiagram
    class FacebookAdsWorkerApplication
    class FacebookAdsService
    class InstagramCampaignService
    class InstagramCampaignScheduler

    FacebookAdsWorkerApplication --> FacebookAdsService
    InstagramCampaignService --> FacebookAdsService
    InstagramCampaignScheduler --> InstagramCampaignService
```
