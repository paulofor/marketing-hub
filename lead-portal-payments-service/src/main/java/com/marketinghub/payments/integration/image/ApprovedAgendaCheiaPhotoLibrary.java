package com.marketinghub.payments.integration.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.marketinghub.payments.service.kit.CapellaKitCatalog;
import com.marketinghub.payments.service.kit.CapellaKitProfile;
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
    private static final String MANIFEST_NAME = "approved-manifest.tsv";
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
        return generate(executionId, variant, CapellaKitCatalog.byCode(CapellaKitCatalog.NAILS));
    }

    /** Seleciona somente o acervo da profissão solicitada, sem fallback para unhas. */
    @Override
    public BufferedImage generate(String executionId, int variant, CapellaKitProfile profile) {
        List<Path> assets = approvedAssets(profile);
        int offset = Math.floorMod(executionId.hashCode(), assets.size());
        Path selected = assets.get(Math.floorMod(offset + variant, assets.size()));
        try {
            BufferedImage image = ImageIO.read(selected.toFile());
            if (image == null || image.getWidth() < 1024 || image.getHeight() < 1024) {
                throw new IllegalStateException("Fotografia aprovada inválida ou abaixo de 1024px");
            }
            log.info("Fotografia aprovada selecionada. executionId={}, variant={}, profileCode={}, asset={}",
                    executionId, variant, profile.code(), selected.getFileName());
            return image;
        } catch (IOException ex) {
            log.error("Falha ao ler fotografia aprovada. executionId={}, variant={}, asset={}",
                    executionId, variant, selected.getFileName(), ex);
            throw new IllegalStateException("Não foi possível carregar a fotografia aprovada", ex);
        }
    }

    /** Lista apenas imagens do diretório aprovado e bloqueia acervo insuficiente. */
    private List<Path> approvedAssets(CapellaKitProfile profile) {
        Path profileRoot = approvedRoot.resolve(profile.libraryDirectory());
        try {
            if (!Files.isDirectory(profileRoot)) {
                throw new IllegalStateException("Biblioteca fotográfica aprovada não está disponível para " + profile.code());
            }
            List<Path> assets;
            try (var paths = Files.list(profileRoot)) {
                assets = paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedImage)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
            if (assets.size() < MINIMUM_LIBRARY_SIZE) {
                throw new IllegalStateException("Biblioteca fotográfica aprovada precisa de ao menos 10 imagens");
            }
            validateManifest(assets, profileRoot, profile);
            return assets;
        } catch (IOException ex) {
            log.error("Falha ao listar biblioteca fotográfica aprovada. root={}", approvedRoot, ex);
            throw new IllegalStateException("Não foi possível consultar a biblioteca fotográfica aprovada", ex);
        }
    }

    /** Confirma revisão auditável, hashes distintos e profissão declarada para o novo acervo. */
    private void validateManifest(List<Path> assets, Path profileRoot, CapellaKitProfile profile) throws IOException {
        Path manifest = profileRoot.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            throw new IllegalStateException("Biblioteca fotográfica aprovada não possui manifesto auditável");
        }
        List<String> lines = Files.readAllLines(manifest);
        if (!CapellaKitCatalog.NAILS.equals(profile.code())
                && !lines.contains("# profile=" + profile.code())) {
            throw new IllegalStateException("Manifesto fotográfico não comprova a profissão " + profile.code());
        }
        Map<String, String> approvedHashes = new HashMap<>();
        java.util.Set<String> uniqueHashes = new java.util.HashSet<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] columns = line.split("\\t", -1);
            if (columns.length < 6 || !"APPROVED".equals(columns[4]) || !validScore(columns[3])
                    || !"false".equals(columns[5])) continue;
            approvedHashes.put(columns[0], columns[1]);
        }
        for (Path asset : assets) {
            String expected = approvedHashes.get(asset.getFileName().toString());
            String actual = sha256(asset);
            if (!actual.equals(expected)) {
                throw new IllegalStateException("Fotografia sem aprovação auditável: " + asset.getFileName());
            }
            if (!uniqueHashes.add(actual)) {
                throw new IllegalStateException("Biblioteca fotográfica possui imagens duplicadas");
            }
        }
    }

    /** Rejeita notas inválidas ou não finitas sem promover o arquivo por acidente. */
    private boolean validScore(String value) {
        try {
            double score = Double.parseDouble(value);
            return Double.isFinite(score) && score >= 9.0 && score <= 10.0;
        } catch (NumberFormatException ex) {
            log.warn("Nota inválida no manifesto fotográfico. root={}", approvedRoot, ex);
            return false;
        }
    }

    /** Calcula a identidade imutável do arquivo aprovado. */
    private String sha256(Path asset) throws IOException {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(asset)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            log.error("Falha no hash da fotografia. asset={}", asset.getFileName(), ex);
            throw new IllegalStateException("SHA-256 indisponível no runtime", ex);
        }
    }

    /** Aceita somente formatos raster seguros usados pelo compositor. */
    private boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }
}
