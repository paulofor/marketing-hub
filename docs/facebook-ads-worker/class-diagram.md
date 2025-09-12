# Facebook Ads Worker Class Diagram

Only the classes that contain campaign data sent to the Facebook API are represented.

```mermaid
classDiagram
    class FacebookAdsService
    class InstagramCampaignService
    class FacebookCampaignService

    InstagramCampaignService --> FacebookAdsService
    FacebookCampaignService --> FacebookAdsService
```
