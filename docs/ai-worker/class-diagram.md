# AI Worker Class Diagram

```mermaid
classDiagram
    class AiWorkerApplication
    class SuccessProductAnalyzer
    class SuccessProductScheduler
    class ChatGptClient {
        +enrich(SuccessProduct) SuccessProduct
    }
    ChatGptClient <|.. DummyChatGptClient
    ChatGptClient <|.. OpenAiChatGptClient
    SuccessProductAnalyzer --> ChatGptClient
    WorkerSuccessProductRepository --> SuccessProductAnalyzer
    SuccessProductScheduler --> SuccessProductAnalyzer

    class SuccessProductNicheHypothesisService
    class SuccessProductNicheHypothesisScheduler
    class SPChatGptClient {
        +extract(SuccessProduct) NicheHypothesis
    }
    SuccessProductNicheHypothesisService --> SPChatGptClient
    WorkerSuccessProductRepository --> SuccessProductNicheHypothesisService
    SuccessProductNicheHypothesisScheduler --> SuccessProductNicheHypothesisService

    class NicheHypothesisService
    class NicheHypothesisScheduler
    class ExperimentCreativeService
    class ExperimentCreativeScheduler
    class NicheAudienceService
    class NicheAudienceScheduler
    class AudienceChatGptClient

    NicheHypothesisScheduler --> NicheHypothesisService
    ExperimentCreativeScheduler --> ExperimentCreativeService
    NicheHypothesisService --> NicheChatGptClient
    ExperimentCreativeService --> CreativeChatGptClient
    ExperimentCreativeService --> CreativeImageClient
    NicheAudienceScheduler --> NicheAudienceService
    NicheAudienceService --> AudienceChatGptClient
```
