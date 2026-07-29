package com.marketinghub.socialmediaworker.youtube;

import com.marketinghub.socialmediaworker.dto.YoutubePublicationAction;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationInput;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationOutput;
import com.marketinghub.socialmediaworker.pipeline.StageContext;
import com.marketinghub.socialmediaworker.pipeline.StageProcessor;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Executa a etapa concreta de publicacao e aquecimento no YouTube.
 */
@Component
public class YoutubePublicationProcessor implements StageProcessor<YoutubePublicationInput, YoutubePublicationOutput> {
    private final YoutubeApiClient youtubeApiClient;

    /**
     * Recebe o client responsavel por chamadas ao YouTube.
     */
    public YoutubePublicationProcessor(YoutubeApiClient youtubeApiClient) {
        this.youtubeApiClient = youtubeApiClient;
    }

    /**
     * Processa a acao YouTube solicitada pelo backend.
     */
    @Override
    public YoutubePublicationOutput process(StageContext context, YoutubePublicationInput input) {
        validateInput(input);
        return switch (input.action()) {
            case CONNECT_EXISTING_CHANNEL -> connectedChannel(input);
            case CREATE_CHANNEL_PLAN -> channelCreationPlan(input);
            case MARKET_WARMUP_PLAN -> marketWarmupPlan(input);
            case CREATE_PLAYLIST -> plannedStructure(input, "PLAYLIST_PLANNED", "Playlist planejada para organizar a narrativa do produto.");
            case CREATE_CHANNEL_SECTION -> plannedStructure(input, "CHANNEL_SECTION_PLANNED", "Secao planejada para destacar provas, aulas e casos.");
            case PUBLISH_VIDEO -> publishVideo(input);
        };
    }

    /**
     * Valida os campos minimos da entrada funcional.
     */
    private void validateInput(YoutubePublicationInput input) {
        if (input == null || input.publicationId() == null) {
            throw new IllegalArgumentException("Execucao YouTube sem publicationId.");
        }
        if (input.action() == null) {
            throw new IllegalArgumentException("Execucao YouTube sem action.");
        }
        if (input.action() == YoutubePublicationAction.PUBLISH_VIDEO && !StringUtils.hasText(input.title())) {
            throw new IllegalArgumentException("Publicacao de video exige titulo.");
        }
    }

    /**
     * Confirma que o canal existente pode ser usado pelo funil.
     */
    private YoutubePublicationOutput connectedChannel(YoutubePublicationInput input) {
        return output(
                input,
                "CHANNEL_CONNECTED",
                null,
                null,
                "Canal existente conectado como ativo de distribuicao organica.",
                List.of("Criar playlist por promessa principal do PDE.", "Publicar primeiro video como nao listado antes de liberar publico."));
    }

    /**
     * Gera plano de criacao manual de canal quando a plataforma nao permite criar por API.
     */
    private YoutubePublicationOutput channelCreationPlan(YoutubePublicationInput input) {
        return output(
                input,
                "CHANNEL_CREATION_MANUAL_REQUIRED",
                null,
                null,
                "YouTube exige criacao/conexao do canal fora da chamada de publicacao; o worker preparou o plano de onboarding.",
                List.of("Criar ou selecionar canal na conta Google.", "Conectar OAuth no Marketing Hub.", "Voltar para publicar videos pelo worker."));
    }

    /**
     * Gera a pauta inicial de aquecimento de mercado para o canal.
     */
    private YoutubePublicationOutput marketWarmupPlan(YoutubePublicationInput input) {
        return output(
                input,
                "MARKET_WARMUP_PLANNED",
                null,
                null,
                "Plano de aquecimento criado para educar dor, demonstrar mecanismo e preparar venda do PDE.",
                List.of("Video 1: dor cotidiana e custo de nao resolver.", "Video 2: mecanismo IA invisivel no dia a dia.", "Video 3: prova/demonstracao curta com CTA para lista ou pagina."));
    }

    /**
     * Registra estruturas editoriais planejadas para o canal.
     */
    private YoutubePublicationOutput plannedStructure(YoutubePublicationInput input, String status, String businessResult) {
        return output(input, status, null, null, businessResult, List.of("Aguardar conexao OAuth completa para materializar no YouTube."));
    }

    /**
     * Publica video ou simula publicacao conforme configuracao operacional.
     */
    private YoutubePublicationOutput publishVideo(YoutubePublicationInput input) {
        YoutubeApiClient.YoutubeUploadResponse upload = youtubeApiClient.uploadVideo(input);
        return output(
                input,
                "VIDEO_PUBLISHED",
                upload.videoId(),
                upload.videoUrl(),
                "Video publicado para aquecer o mercado e criar ativo organico reaproveitavel em campanhas.",
                List.of("Monitorar retencao inicial.", "Extrair comentarios com dor/objecao.", "Reaproveitar cortes em Shorts e criativos pagos."));
    }

    /**
     * Monta a saida funcional padronizada da etapa.
     */
    private YoutubePublicationOutput output(
            YoutubePublicationInput input,
            String status,
            String externalVideoId,
            String externalUrl,
            String businessResult,
            List<String> nextActions) {
        return new YoutubePublicationOutput(
                input.publicationId(),
                "YOUTUBE",
                input.action(),
                status,
                externalVideoId,
                externalUrl,
                businessResult,
                nextActions,
                Instant.now());
    }
}
