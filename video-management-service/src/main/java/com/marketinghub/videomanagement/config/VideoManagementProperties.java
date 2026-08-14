package com.marketinghub.videomanagement.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component("videoManagementProperties")
@ConfigurationProperties(prefix = "video")
@Validated
public class VideoManagementProperties {

    @NotNull
    private URI backendBaseUrl = URI.create("http://backend:8000");

    private String authToken;

    /**
     * Identificador usado para registrar claim e heartbeat junto ao backend.
     */
    private String workerId = "video-management-service";

    @NotNull
    private Jobs jobs = new Jobs();

    @NotNull
    private Providers providers = new Providers();

    @NotNull
    private ApolloPlanner apolloPlanner = new ApolloPlanner();

    /** Configura o planejador criativo que antecede qualquer render pago de Apolo. */
    @Getter
    @Setter
    public static class ApolloPlanner {
        private boolean enabled = true;
        @NotNull
        private URI openAiBaseUrl = URI.create("https://api.openai.com/v1");
        private String apiKey;
        private String apiKeyFile;
        private String model = "gpt-5.6";
        @NotNull
        private CodexShadow codexShadow = new CodexShadow();
    }

    /** Configura a candidata Codex que participa apenas do replay sombra de Apolo. */
    @Getter
    @Setter
    public static class CodexShadow {
        private boolean enabled = false;
        private String command = "codex";
        private String model = "gpt-5.6-sol";
        private String reasoningEffort = "high";
        private String workingDirectory = "/app";
        @NotNull
        private Duration timeout = Duration.ofMinutes(10);
    }

    @Getter
    @Setter
    public static class Jobs {
        private boolean pollingEnabled = false;
        private boolean orphanRecoveryEnabled = true;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(30);

        @NotNull
        private Duration orphanThreshold = Duration.ofMinutes(10);

        @NotNull
        private Duration backendCallBackoff = Duration.ofSeconds(2);

        @Min(1)
        private int batchSize = 10;

        @Min(1)
        private int backendCallMaxAttempts = 3;
    }

    @Getter
    @Setter
    public static class Providers {
        @NotNull
        private Real real = new Real();

        @NotNull
        private Luma luma = new Luma();

        @NotNull
        private Kling kling = new Kling();

        @NotNull
        private Runway runway = new Runway();

        @NotNull
        private HeyGen heygen = new HeyGen();

        @NotNull
        private Veo veo = new Veo();

        @NotNull
        private PostProduction postProduction = new PostProduction();
    }

    @Getter
    @Setter
    public static class Luma {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs destinados ao adapter direto da Luma Agents.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("LUMA_RAY_3_2", "LUMA", "RAY_3_2"));

        /**
         * Base URL oficial da Luma Agents API.
         */
        @NotNull
        private URI baseUrl = URI.create("https://agents.lumalabs.ai");

        /**
         * Chave da Luma Agents API usada apenas pelo módulo executor de vídeo.
         */
        private String apiKey;

        /**
         * Caminho opcional para a chave Luma montada como secret em arquivo.
         */
        private String apiKeyFile;

        private String model = "ray-3.2";
        private String aspectRatio = "9:16";
        private String resolution = "720p";
        private String duration = "10s";
        private int sceneCount = 3;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(10);

        @Min(1)
        private int maxPollAttempts = 120;

        private String ffmpegPath = "ffmpeg";
        private boolean openAiReferenceImageEnabled = false;
        private URI openAiBaseUrl = URI.create("https://api.openai.com/v1");
        private String openAiApiKey;
        private String openAiApiKeyFile;
        private String openAiImageModel = "gpt-5.6";
        private String openAiImageToolModel = "gpt-image-2";
    }

    @Getter
    @Setter
    public static class Kling {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs destinados ao adapter direto do Kling.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("KLING_3_0", "KLING", "KLING_OMNI"));

        /**
         * Base URL oficial da API Kling.
         */
        @NotNull
        private URI baseUrl = URI.create("https://api.klingai.com");

        /**
         * Chave Kling usada apenas pelo módulo executor de vídeo.
         */
        private String apiKey;

        /**
         * Caminho opcional para a chave Kling montada como secret em arquivo.
         */
        private String apiKeyFile;

        private String model = "kling-v2-1-master";
        private String createPath = "/v1/videos/text2video";
        private String statusPathTemplate = "/v1/videos/text2video/{taskId}";
        private String imageCreatePath = "/v1/videos/image2video";
        private String imageStatusPathTemplate = "/v1/videos/image2video/{taskId}";
        private String aspectRatio = "9:16";
        private String mode = "std";
        private String duration = "5";
        private String negativePrompt = "sensualized woman, seductive pose, body focus, exposed body, luxury ostentation, dark haze, blur, distorted hands, embedded text, logo";

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(10);

        @Min(1)
        private int maxPollAttempts = 120;
    }

    @Getter
    @Setter
    public static class Runway {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs destinados ao adapter direto da Runway.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of(
                "RUNWAY", "RUNWAY_GEN_4_5", "RUNWAY_SEEDANCE_2", "RUNWAY_SEEDANCE_2_5", "RUNWAY_HAILUO_3",
                "RUNWAY_GROK_IMAGINE_1_5",
                "RUNWAY_GEN_4_TURBO", "RUNWAY_VEO_3_1", "RUNWAY_VEO_3_1_FAST", "RUNAWAY"));

        /**
         * Base URL oficial da API Runway.
         */
        @NotNull
        private URI baseUrl = URI.create("https://api.dev.runwayml.com");

        /**
         * Chave Runway usada apenas pelo módulo executor de vídeo.
         */
        private String apiKey;

        /**
         * Caminho opcional para a chave Runway montada como secret em arquivo.
         */
        private String apiKeyFile;

        private String apiVersion = "2024-11-06";
        private String model = "gen4.5";
        private String createPath = "/v1/image_to_video";
        private String textCreatePath = "/v1/text_to_video";
        private String statusPathTemplate = "/v1/tasks/{taskId}";
        private String ratio = "720:1280";
        private int durationSeconds = 10;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(10);

        @Min(1)
        private int maxPollAttempts = 120;
    }

    @Getter
    @Setter
    public static class HeyGen {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs destinados ao adapter direto da HeyGen.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("HEYGEN", "HEYGEN_AVATAR"));

        /**
         * Base URL oficial da API HeyGen.
         */
        @NotNull
        private URI baseUrl = URI.create("https://api.heygen.com");

        /**
         * Chave HeyGen usada apenas pelo módulo executor de vídeo.
         */
        private String apiKey;

        /**
         * Caminho opcional para a chave HeyGen montada como secret em arquivo.
         */
        private String apiKeyFile;

        private String createPath = "/v3/videos";
        private String statusPathTemplate = "/v3/videos/{videoId}";
        private String avatarId;
        private String voiceId;
        private String aspectRatio = "9:16";
        private String outputFormat = "mp4";
        private String engineType = "avatar_iv";
        private boolean captionEnabled = true;
        private String captionStyle = "default";
        private String backgroundValue = "#F8F0EA";
        private double voiceSpeed = 1.0;
        private double voicePitch = 0.0;
        private double voiceVolume = 1.0;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(10);

        @Min(1)
        private int maxPollAttempts = 120;
    }

    @Getter
    @Setter
    public static class Real {
        private boolean enabled = false;

        /**
         * Nomes que identificam o provider real dentro de providerName do job.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("REAL", "SYNTHESIA"));

        /**
         * Base URL da API do provider real.
         */
        private URI baseUrl;

        /**
         * Token opcional para autenticação bearer na API do provider.
         */
        private String authToken;

        private String createPath = "/v1/renders";
        private String statusPathTemplate = "/v1/renders/{providerJobId}";

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(5);

        @Min(1)
        private int maxPollAttempts = 120;
    }

    @Getter
    @Setter
    public static class Veo {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs destinados ao adapter direto do VEO.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("VEO", "VEO-3.1", "VEO_3_1", "REAL"));

        /**
         * Base URL oficial da Gemini API para geração de vídeos.
         */
        @NotNull
        private URI baseUrl = URI.create("https://generativelanguage.googleapis.com/v1beta");

        /**
         * Chave da Gemini API usada apenas pelo módulo executor de vídeo.
         */
        private String apiKey;

        /**
         * Caminho opcional para a chave Gemini montada como secret em arquivo.
         */
        private String apiKeyFile;

        private String model = "veo-3.1-generate-preview";
        private String aspectRatio = "9:16";
        private String resolution = "720p";
        private String personGeneration = "allow_all";
        private Integer durationSeconds = 8;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(10);

        @Min(1)
        private int maxPollAttempts = 120;
    }

    @Getter
    @Setter
    public static class PostProduction {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs de acabamento comercial local.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("MUSA_POST_PRODUCTION", "POST_PRODUCTION"));

        private String ffmpegPath = "ffmpeg";
        private String ffprobePath = "ffprobe";
        private String espeakPath = "espeak-ng";
        private String espeakVoice = "pt-br";
        private String fontFile = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf";
        private boolean openAiTtsEnabled = false;
        private URI openAiBaseUrl = URI.create("https://api.openai.com/v1");
        private String openAiApiKey;
        private String openAiApiKeyFile;
        private String openAiTtsModel = "gpt-4o-mini-tts";
        private String openAiTtsVoice = "nova";
        private String openAiTtsResponseFormat = "mp3";
        private String openAiTtsInstructions = "Fale em português do Brasil com voz feminina natural, elegante, acolhedora e confiante. Ritmo de anúncio mobile, sem soar robótica, sem dramatização exagerada e com CTA claro.";
    }
}
