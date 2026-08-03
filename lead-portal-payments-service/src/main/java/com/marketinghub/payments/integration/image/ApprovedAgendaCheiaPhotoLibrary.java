package com.marketinghub.payments.integration.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Seleciona somente fotografias previamente aprovadas para os kits Agenda Cheia. */
@Component
public class ApprovedAgendaCheiaPhotoLibrary implements AgendaCheiaPhotoGenerator {
    private static final Logger log = LoggerFactory.getLogger(ApprovedAgendaCheiaPhotoLibrary.class);
    private static final int MINIMUM_LIBRARY_SIZE = 10;
    private final Path approvedRoot;

    /** Configura o diretório persistente que contém exclusivamente fotografias aprovadas. */
    public ApprovedAgendaCheiaPhotoLibrary(
            @Value("${agenda-cheia.production.approved-photo-root:/data/agenda-cheia/photo-library/approved}")
                    String approvedRoot) {
        this.approvedRoot = Path.of(approvedRoot).toAbsolutePath().normalize();
    }

    /** Retorna uma fotografia distinta e deterministicamente distribuída para a execução. */
    @Override
    public BufferedImage generate(String executionId, int variant) {
        List<Path> assets = approvedAssets();
        int offset = Math.floorMod(executionId.hashCode(), assets.size());
        Path selected = assets.get(Math.floorMod(offset + variant, assets.size()));
        try {
            BufferedImage image = ImageIO.read(selected.toFile());
            if (image == null || image.getWidth() < 1024 || image.getHeight() < 1024) {
                throw new IllegalStateException("Fotografia aprovada inválida ou abaixo de 1024px");
            }
            log.info("Fotografia aprovada selecionada. executionId={}, variant={}, asset={}",
                    executionId, variant, selected.getFileName());
            return image;
        } catch (IOException ex) {
            log.error("Falha ao ler fotografia aprovada. executionId={}, variant={}, asset={}",
                    executionId, variant, selected.getFileName(), ex);
            throw new IllegalStateException("Não foi possível carregar a fotografia aprovada", ex);
        }
    }

    /** Lista apenas imagens do diretório aprovado e bloqueia acervo insuficiente. */
    private List<Path> approvedAssets() {
        try {
            if (!Files.isDirectory(approvedRoot)) {
                throw new IllegalStateException("Biblioteca fotográfica aprovada não está disponível");
            }
            List<Path> assets;
            try (var paths = Files.list(approvedRoot)) {
                assets = paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedImage)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
            if (assets.size() < MINIMUM_LIBRARY_SIZE) {
                throw new IllegalStateException("Biblioteca fotográfica aprovada precisa de ao menos 10 imagens");
            }
            return assets;
        } catch (IOException ex) {
            log.error("Falha ao listar biblioteca fotográfica aprovada. root={}", approvedRoot, ex);
            throw new IllegalStateException("Não foi possível consultar a biblioteca fotográfica aprovada", ex);
        }
    }

    /** Aceita somente formatos raster seguros usados pelo compositor. */
    private boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }
}
