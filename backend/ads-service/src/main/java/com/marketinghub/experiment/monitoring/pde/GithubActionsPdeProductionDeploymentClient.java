package com.marketinghub.experiment.monitoring.pde;

import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployResponseDto;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Solicita ao GitHub Actions o deploy produtivo do PDE sem usar SSH direto no servidor. */
@Component
@Slf4j
public class GithubActionsPdeProductionDeploymentClient implements PdeProductionDeploymentClient {

    private final String token;
    private final String repository;
    private final String workflowFile;
    private final String ref;
    private final RestClient restClient;

    /** Inicializa o cliente com repositório, workflow e token administrativo configuráveis. */
    public GithubActionsPdeProductionDeploymentClient(
            @Value("${integrations.pde-platform.github-actions.token:${GITHUB_TOKEN:}}") String token,
            @Value("${integrations.pde-platform.github-actions.repository:paulofor/marketing-hub}") String repository,
            @Value("${integrations.pde-platform.github-actions.workflow-file:pde-platform-metodo-musa-ci.yml}") String workflowFile,
            @Value("${integrations.pde-platform.github-actions.ref:main}") String ref) {
        this.token = token;
        this.repository = repository;
        this.workflowFile = workflowFile;
        this.ref = ref;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .requestFactory(requestFactory)
                .build();
    }

    /** Informa se o token e os identificadores do workflow estão presentes. */
    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(token)
                && StringUtils.hasText(repository)
                && StringUtils.hasText(repositoryOwner())
                && StringUtils.hasText(repositoryName())
                && StringUtils.hasText(workflowFile)
                && StringUtils.hasText(ref);
    }

    /** Dispara o workflow com target_environment=production e registra contexto operacional. */
    @Override
    public PostDeployPdeProductionDeployResponseDto dispatchProductionDeploy(
            Long experimentId,
            String requestedBy,
            String sourceCommitSha) {
        if (!isConfigured()) {
            return new PostDeployPdeProductionDeployResponseDto(
                    false,
                    "NOT_CONFIGURED",
                    "Token ou workflow do GitHub Actions não configurado no backend.",
                    "production",
                    workflowFile,
                    sourceCommitSha,
                    Instant.now());
        }
        try {
            restClient.post()
                    .uri(
                            "/repos/{owner}/{repo}/actions/workflows/{workflowFile}/dispatches",
                            repositoryOwner(),
                            repositoryName(),
                            workflowFile)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "ref", ref,
                            "inputs", Map.of("target_environment", "production")))
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Deploy produtivo PDE solicitado pelo Marketing Hub; experimentId={}, requestedBy={}, sourceCommitSha={}, workflowFile={}, ref={}",
                    experimentId,
                    requestedBy,
                    sourceCommitSha,
                    workflowFile,
                    ref);
            return new PostDeployPdeProductionDeployResponseDto(
                    true,
                    "DISPATCHED",
                    "Workflow de produção solicitado. Acompanhe o painel até produção mostrar o mesmo commit da homologação.",
                    "production",
                    workflowFile,
                    sourceCommitSha,
                    Instant.now());
        } catch (Exception ex) {
            log.error(
                    "Falha ao solicitar deploy produtivo PDE; experimentId={}, requestedBy={}, sourceCommitSha={}, repository={}, workflowFile={}, ref={}",
                    experimentId,
                    requestedBy,
                    sourceCommitSha,
                    repository,
                    workflowFile,
                    ref,
                    ex);
            throw new IllegalStateException("Não foi possível solicitar o deploy produtivo do PDE", ex);
        }
    }

    /** Extrai o owner do repositório GitHub configurado. */
    private String repositoryOwner() {
        return repository.split("/", 2)[0];
    }

    /** Extrai o nome do repositório GitHub configurado. */
    private String repositoryName() {
        String[] parts = repository.split("/", 2);
        return parts.length > 1 ? parts[1] : "";
    }
}
