# Reviewed evidence: Synthesia plugin in ChatGPT — 2026-11-03

## Date and novelty
- 2026-10-09: Synthesia first announced Syren in beta, a prompt-driven agent for enterprise videos, including motion graphics, avatars, narration, soundtrack and b-roll.
- 2026-11-03: Synthesia announced its ChatGPT plugin, available to connect on all Synthesia plans, including Basic. It can assemble videos from a conversation, a script, a document or material from other authorized connected applications.
- This is a significant workflow and distribution integration, not a newly released video foundation model, new benchmark leader or open-weight release.

## Verified modes and status
- Official announcement: ChatGPT plugin is live; Synthesia's support documentation explicitly labels the underlying MCP connection PUBLIC BETA, with features and pricing subject to change before general availability.
- Default Syren can generate and revise videos via chat using video, animation, motion graphics, avatars, voiceover, soundtrack and b-roll. Syren video cannot be opened in the traditional Synthesia Studio editor.
- The alternative Studio creates a video draft using an exact script, style, template or brand kit, with detailed scene edits completed later inside Synthesia's own editor.
- MCP can locate existing videos, restricted to the signed-in Synthesia workspace and its permissions.
- Creating a Studio draft does not use credits; rendering it and creating Syren videos consume credits. MCP itself has no extra connector charge during the beta.
- This is a proprietary SaaS integration. Synthesia did not announce weights for local/self-hosted video inference. Synthesia already had separate Video API endpoints.
- No authenticated test on an actual ChatGPT account was conducted; eligibility can vary by account, location and plugin availability.

## Pricing and licensing
- Official pricing checked 2026-11-03: Basic $0 has 10 minutes of Studio creation/dubbing plus 500 credits per month, with a limited offer of one Syren video. Basic outputs include watermark and may be limited to sharing links.
- Starter: $19/month (promotional first month $9), Pro: $89/month (promotional first month $49) on the monthly pricing view. Values are not a new price cut on November 3. Billing term, local taxes and media generation options can change costs.
- Usage for Syren, video rendering and other components is credit-based; precise cost of each video depends on what it includes. No cost or quality test was performed.
- Verify terms governing commercial avatars, voice/likeness, music, image rights, brands and document data before advertising deployment. The launch is not itself an unrestricted commercial license for every asset.

## Competitor context: checked 2026-11-03
- Synthesia (UK/Europe): Plugin LIVE according to vendor, MCP PUBLIC BETA. Document-to-explainer, avatar and motion-graphics agent.
- Runway Ads / App Studio (US): ACTIVE subscription products. Runway Ads includes ad performance inputs; not the same as Synthesia source-to-video.
- Genspark Media Studio (international): ACTIVE SaaS media workflow, with production agents.
- Kling 4.0 (China): ACTIVE via Runway Dev API, reference-guided videos of 3-30 seconds.
- Seedance 2.5 (China): ACTIVE via Runway Dev API, video generation and editing with references.
- Veo 3.1 GA (US): ACTIVE on Google Cloud; retired Veo Preview endpoints are separate and should not be recommended.
- OpenAI Sora 2 app/API: DISCONTINUED and unavailable as a live production option.
- ElevenLabs SFX v3 (US): SOUND EFFECTS API announced earlier on Oct 29; the Nov 2 developer guide is follow-up usage guidance, not a new model launch.

## Sources
- Synthesia release dated 2026-11-03: https://www.synthesia.io/post/synthesia-launches-in-chatgpt
- Official Synthesia MCP public-beta support: https://help.synthesia.io/en/articles/17504685-how-do-i-use-the-synthesia-mcp
- Official pricing: https://www.synthesia.io/pricing
- Syren initial launch, 2026-10-09: https://www.synthesia.io/post/syren-studio
- MCP documentation: https://developers.synthesia.io/docs/synthesia-mcp
- Runway API changelog: https://docs.dev.runwayml.com/api-details/api_changelog/
- ElevenLabs SFX developer update 2026-11-02: https://elevenlabs.io/blog/sfx-v3-for-developers

## Marketing Hub experiment
Produce a 30-60 second Brazilian Portuguese explainer from an authorized product brief in Syren, Studio and the existing Marketing Hub workflow. Compare factual accuracy, brand consistency, editing workload, total credits spent, elapsed production time, rights suitability and cost per approved video. Do not mistake the new plugin for a new universal video rendering API.
