package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider "stub" utilizado durante o desenvolvimento/local.
 */
@Component
public class StubVideoProvider implements VideoProvider {
    private static final MediaType VIDEO_MP4 = MediaType.valueOf("video/mp4");
    private static final MediaType IMAGE_PNG = MediaType.IMAGE_PNG;
    private static final MediaType TEXT_VTT = MediaType.valueOf("text/vtt");

    @Override
    public boolean supports(SalesVideoJob job) {
        boolean isRenderJob = job.jobType() == SalesVideoJobType.RENDER;
        String providerName = job.providerName();
        boolean matchesName = !StringUtils.hasText(providerName)
                || "STUB".equalsIgnoreCase(providerName)
                || "DEV-STUB".equalsIgnoreCase(providerName);
        return isRenderJob && matchesName;
    }

    @Override
    public ProviderArtifacts render(SalesVideoJob job,
                                    SalesVideoProfile profile,
                                    ProgressCallback progressCallback) {
        SalesVideoScript script = ensureScript(profile);
        progressCallback.onProgress(5, SalesVideoStatus.VIDEO_PROCESSING, "Preparando roteiro aprovado");
        sleepMillis(150);
        progressCallback.onProgress(35, SalesVideoStatus.VIDEO_PROCESSING, "Gerando cenas e narrador");
        sleepMillis(200);
        ProviderFile videoFile = new ProviderFile(
                "sales-video-" + job.id() + ".mp4",
                VIDEO_MP4,
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                buildVideoPayload(profile, script));
        progressCallback.onProgress(65, SalesVideoStatus.VIDEO_PROCESSING, "Renderizando poster e legendas");
        ProviderFile posterFile = new ProviderFile(
                "sales-video-" + job.id() + "-poster.png",
                IMAGE_PNG,
                AssetType.IMAGE,
                ProviderAssetRole.POSTER,
                buildPoster(profile, script));
        ProviderFile captionFile = new ProviderFile(
                "sales-video-" + job.id() + ".vtt",
                TEXT_VTT,
                AssetType.CAPTION,
                ProviderAssetRole.CAPTION,
                buildCaptions(script));
        progressCallback.onProgress(95, SalesVideoStatus.VIDEO_PROCESSING, "Finalizando assets gerados");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("generator", "stub");
        metadata.put("profile_title", profile.title());
        metadata.put("script_version", script.version());
        metadata.put("language", profile.language());
        metadata.put("generated_at", Instant.now().toString());
        return new ProviderArtifacts("stub-" + job.id(), videoFile, posterFile, captionFile, metadata);
    }

    private SalesVideoScript ensureScript(SalesVideoProfile profile) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || !StringUtils.hasText(script.scriptText())) {
            throw new VideoProviderException("Perfil não possui script aprovado para renderização");
        }
        return script;
    }

    private byte[] buildVideoPayload(SalesVideoProfile profile, SalesVideoScript script) {
        String persona = StringUtils.hasText(profile.personaName()) ? profile.personaName() : "Persona";
        String content = "Stub video for %s - %s\n\n%s".formatted(persona,
                profile.title(),
                script.scriptText());
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildPoster(SalesVideoProfile profile, SalesVideoScript script) {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(new Color(20, 24, 33));
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.drawString(profile.title(), 40, 80);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
            String hook = StringUtils.hasText(script.hookText()) ? script.hookText() : "Mensagem principal";
            g2d.drawString(hook, 40, 140);
            g2d.drawString("Stub provider", 40, 320);
        } finally {
            g2d.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new VideoProviderException("Falha ao gerar poster stub", ex);
        }
    }

    private byte[] buildCaptions(SalesVideoScript script) {
        String hook = StringUtils.hasText(script.hookText()) ? script.hookText() : "Hook";
        String body = StringUtils.hasText(script.scriptText()) ? script.scriptText() : "Mensagem";
        String cta = StringUtils.hasText(script.ctaText()) ? script.ctaText() : "Chamada para ação";
        String vtt = "WEBVTT\n\n" +
                "00:00:00.000 --> 00:00:04.000\n" + hook + "\n\n" +
                "00:00:04.000 --> 00:00:12.000\n" + body + "\n\n" +
                "00:00:12.000 --> 00:00:16.000\n" + cta + "\n";
        return vtt.getBytes(StandardCharsets.UTF_8);
    }

    private void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
