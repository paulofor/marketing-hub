package com.marketinghub.geralanding.publiclanding.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.publiclanding.service.approveEndPublish.PublicLandingLeadPortalPublishRequest;
import com.marketinghub.geralanding.publiclanding.service.approveEndPublish.PublicLandingPublicationResponse;
import com.marketinghub.geralanding.publiclanding.service.pending.RecordPublicLandingPending;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/** Orquestra a aprovação e publicação da landing pública gerada pelo GeraLanding. */
@Service
public class BackendPublicLandingService {
    private static final Logger log = LoggerFactory.getLogger(BackendPublicLandingService.class);

    private final ExperimentRepository experimentRepository;
    private final RestTemplate restTemplate;
    private final String leadPortalBaseUrl;

    /** Cria o service com repositório de experimentos, cliente HTTP e URL base do Lead Portal. */
    public BackendPublicLandingService(
            ExperimentRepository experimentRepository,
            RestTemplate restTemplate,
            @Value("${integrations.lead-portal.base-url:}") String leadPortalBaseUrl) {
        this.experimentRepository = experimentRepository;
        this.restTemplate = restTemplate;
        this.leadPortalBaseUrl = leadPortalBaseUrl;
    }

    /** Aprova a landing do experimento, publica no Lead Portal e grava a URL pública oficial. */
    @Transactional
    public PublicLandingPublicationResponse approveEndPublish(Long experimentId) {
        log.info("GeraLanding public landing publish approval started (experimentId={})", experimentId);
        try {
            Experiment experiment = experimentRepository.findById(experimentId)
                    .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
            log.info("GeraLanding public landing publish approval loaded experiment (experimentId={}, experimentName={})",
                    experimentId, experiment.getName());

            String landingPageHtml = resolveLandingPageHtml(experiment, experimentId);
            String slug = "exp-" + experimentId + "-landing-geralanding";
            log.info("GeraLanding public landing publish approval resolved slug (experimentId={}, slug={})", experimentId, slug);

            String finalHtml = buildFinalPublicHtml(experiment, experimentId, slug, landingPageHtml);
            publishToLeadPortal(slug, "Landing GeraLanding - Experimento " + experimentId, finalHtml.trim());
            log.info("GeraLanding public landing publish approval sent flow to Lead Portal successfully (experimentId={}, slug={})",
                    experimentId, slug);

            String iframeUrl = resolveIframeUrl(slug);
            String standaloneUrl = resolveStandaloneLandingUrl(iframeUrl);
            log.info("GeraLanding public landing publish approval resolved publication URLs (experimentId={}, iframeUrl={}, standaloneUrl={})",
                    experimentId, iframeUrl, standaloneUrl);

            experiment.setLandingPageHtml(finalHtml.trim());
            experiment.setFollowUpActionUrl(standaloneUrl);
            experimentRepository.save(experiment);
            log.info("GeraLanding public landing publish approval saved follow-up URL (experimentId={}, followUpActionUrl={})",
                    experimentId, standaloneUrl);
            return new PublicLandingPublicationResponse(
                    experimentId,
                    null,
                    iframeUrl,
                    standaloneUrl,
                    "Landing publicada com sucesso pelo GeraLanding.");
        } catch (RuntimeException ex) {
            log.error(
                    "GeraLanding public landing publish approval failed (experimentId={}, errorClass={}, message={})",
                    experimentId,
                    ex.getClass().getName(),
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    /** Método canônico de etapa reutilizado para publicar a landing pública pelo endpoint approve-end-publish. */
    public PublicLandingPublicationResponse start(Long experimentId) {
        return approveEndPublish(experimentId);
    }

    /** Bloqueia listagem de execuções porque landing pública não possui fila própria. */
    public Object listStageExecutions(Long experimentId) {
        throw new ResponseStatusException(HttpStatus.GONE, "Landing pública não possui stage-executions próprias");
    }

    /** Retorna lista vazia porque a landing pública publica de forma síncrona e não possui pendências internas. */
    public List<RecordPublicLandingPending> pending() {
        return List.of();
    }

    /** Bloqueia recebimento de prompt porque landing pública não aciona IA diretamente. */
    public void recebePrompt(String idJob, Object payload) {
        throw new ResponseStatusException(HttpStatus.GONE, "Landing pública não recebe prompt de IA");
    }

    /** Bloqueia recebimento de resposta porque landing pública não aciona IA diretamente. */
    public void recebeResposta(String idJob, Object payload) {
        throw new ResponseStatusException(HttpStatus.GONE, "Landing pública não recebe resposta de IA");
    }

    /** Bloqueia detalhe de execução porque landing pública não possui job assíncrono próprio. */
    public Object detailStageExecution(Long experimentId, String idJob) {
        throw new ResponseStatusException(HttpStatus.GONE, "Landing pública não possui detalhe de execução próprio");
    }

    /** Seleciona o HTML canônico da landing, priorizando html_geralanding e usando landing_page_html como fallback. */
    private String resolveLandingPageHtml(Experiment experiment, Long experimentId) {
        String landingPageHtml = experiment.getHtmlGeraLanding();
        if (!StringUtils.hasText(landingPageHtml)) {
            landingPageHtml = experiment.getLandingPageHtml();
        }
        if (!StringUtils.hasText(landingPageHtml)) {
            log.warn("GeraLanding public landing publish approval blocked because landing HTML is missing (experimentId={})",
                    experimentId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Landing HTML ainda não foi gerado para este experimento");
        }
        log.info("GeraLanding public landing publish approval found landing HTML (experimentId={}, htmlLength={})",
                experimentId, landingPageHtml.length());
        return landingPageHtml;
    }

    /** Monta o HTML final publicável com envio de lead, tracking, controles de funil e pixel do Facebook. */
    private String buildFinalPublicHtml(Experiment experiment, Long experimentId, String slug, String landingPageHtml) {
        String htmlWithSubmission = injectLeadSubmissionScript(landingPageHtml, slug);
        String htmlWithTracking = injectBehaviorTrackingAttributesAndScript(htmlWithSubmission);
        String htmlWithFunnelControls = injectFunnelControls(htmlWithTracking);
        log.info("GeraLanding public landing publish approval injected funnel controls (experimentId={}, htmlLengthBefore={}, htmlLengthAfter={})",
                experimentId, landingPageHtml.length(), htmlWithFunnelControls.length());

        String facebookPixelId = resolveFacebookPixelId(experiment);
        log.info("GeraLanding public landing publish approval resolved Facebook Pixel (experimentId={}, hasPixelId={})",
                experimentId, StringUtils.hasText(facebookPixelId));
        String htmlWithFacebookPixel = injectFacebookPixel(htmlWithFunnelControls, facebookPixelId);
        log.info("GeraLanding public landing publish approval injected Facebook Pixel (experimentId={}, htmlLengthBefore={}, htmlLengthAfter={})",
                experimentId, htmlWithFunnelControls.length(), htmlWithFacebookPixel.length());
        return htmlWithFacebookPixel;
    }

    /** Publica o HTML final no Lead Portal usando o contrato oficial de flow público. */
    private void publishToLeadPortal(String slug, String name, String html) {
        if (!StringUtils.hasText(leadPortalBaseUrl)) {
            log.warn("GeraLanding public landing Lead Portal publication blocked because base URL is not configured (slug={})", slug);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lead Portal base URL não configurada");
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(leadPortalBaseUrl).path("/api/flows/{slug}").buildAndExpand(slug).toUri();
        log.info("GeraLanding public landing Lead Portal publication request prepared (slug={}, uri={}, name={}, htmlLength={})",
                slug, uri, name, html == null ? 0 : html.length());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        PublicLandingLeadPortalPublishRequest payload = new PublicLandingLeadPortalPublishRequest(
                slug, name, "Fluxo publicado pelo módulo GeraLanding", html);
        log.info("GeraLanding public landing Lead Portal publication request parameters (slug={}, uri={}, headers={}, payload={})",
                slug, uri, headers, payload);
        try {
            restTemplate.put(uri, new HttpEntity<>(payload, headers));
            log.info("GeraLanding public landing Lead Portal publication request completed (slug={}, uri={})", slug, uri);
        } catch (RestClientException ex) {
            log.error("GeraLanding public landing Lead Portal publication request failed (slug={}, uri={}, endpoint={}, errorClass={}, message={})",
                    slug, uri, uri, ex.getClass().getName(), ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao publicar landing no Lead Portal", ex);
        }
    }

    /** Resolve a URL iframe pública retornada ao frontend após publicação. */
    private String resolveIframeUrl(String slug) {
        return UriComponentsBuilder.fromHttpUrl(leadPortalBaseUrl)
                .path("/api/public/flows/{slug}")
                .buildAndExpand(slug)
                .toUriString();
    }

    /** Injeta o script interno de controle de funil quando ainda não está presente. */
    private String injectFunnelControls(String html) {
        String controls = """
                <script data-mh-funnel-controls=\"true\">
                  window.__MH_FUNNEL_CONTROLS__ = { enabled: true, source: 'geralanding' };
                </script>
                """;
        if (html.toLowerCase(Locale.ROOT).contains("data-mh-funnel-controls")) {
            return html;
        }
        if (html.toLowerCase(Locale.ROOT).contains("</head>")) {
            return html.replaceFirst("(?i)</head>", controls + "\n</head>");
        }
        return controls + "\n" + html;
    }

    /** Injeta envio canônico do formulário quando a landing possui campos de lead sem contrato de submissão. */
    private String injectLeadSubmissionScript(String html, String slug) {
        if (!StringUtils.hasText(html) || !StringUtils.hasText(slug) || hasLeadSubmissionContract(html)) {
            return html;
        }
        Document document = Jsoup.parse(html, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);
        if (!hasLeadCaptureControls(document)) {
            return html;
        }
        String escapedSlug = escapeJavaScriptString(slug.trim());
        String script = """
                <script>
                (function(){
                  if (window.__mhLeadSubmissionInstalled) return;
                  window.__mhLeadSubmissionInstalled = true;
                  var slug = '%s';
                  var endpoint = '/api/public/lead-portal/flows/' + encodeURIComponent(slug) + '/submission';
                  var contractVersion = 'lead-portal-submission-engagement.v1';

                  function findField(selectors){
                    for (var i = 0; i < selectors.length; i++) {
                      var field = document.querySelector(selectors[i]);
                      if (field) return field;
                    }
                    return null;
                  }

                  function uniqueId(){
                    if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID();
                    return Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
                  }

                  function setMessage(text, isError){
                    var message = document.getElementById('mh-lead-submission-message');
                    if (!message) {
                      message = document.createElement('p');
                      message.id = 'mh-lead-submission-message';
                      message.setAttribute('role', 'status');
                      message.setAttribute('aria-live', 'polite');
                      var button = document.getElementById('form-submit') || document.querySelector('button[type="submit"], button');
                      if (button && button.parentNode) button.parentNode.insertBefore(message, button.nextSibling);
                    }
                    message.textContent = text;
                    message.style.color = isError ? '#B91C1C' : '#047857';
                  }

                  function normalize(value){
                    return value == null ? '' : String(value).trim();
                  }

                  function submitLead(event){
                    if (event && event.preventDefault) event.preventDefault();
                    var nameField = findField(['#input-nome', '[name="nome"]', '[autocomplete="name"]']);
                    var emailField = findField(['#input-email', '[name="email"]', '[type="email"]', '[autocomplete="email"]']);
                    var phoneField = findField(['#input-telefone', '[name="telefone"]', '[type="tel"]', '[autocomplete="tel"]']);
                    var nome = normalize(nameField && nameField.value);
                    var email = normalize(emailField && emailField.value);
                    var telefone = normalize(phoneField && phoneField.value);
                    if (!nome || !email) {
                      setMessage('Informe seu nome e e-mail para receber a prévia.', true);
                      return;
                    }
                    var submissionId = uniqueId();
                    var payload = {
                      contractVersion: contractVersion,
                      slug: slug,
                      submissionId: submissionId,
                      submittedAt: new Date().toISOString(),
                      contato: {nome: nome, email: email},
                      idempotencyKey: submissionId
                    };
                    if (telefone) payload.contato.telefone = telefone;
                    var button = document.getElementById('form-submit') || document.querySelector('button[type="submit"], button');
                    if (button) button.disabled = true;
                    fetch(endpoint, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload), keepalive: true})
                      .then(function(response){
                        if (!response.ok) throw new Error('HTTP ' + response.status);
                        setMessage('Pronto. Sua prévia foi solicitada com sucesso.', false);
                      })
                      .catch(function(){
                        setMessage('Não foi possível enviar agora. Tente novamente em instantes.', true);
                        if (button) button.disabled = false;
                      });
                  }

                  function init(){
                    var button = document.getElementById('form-submit') || document.querySelector('button[type="submit"], button');
                    if (button) button.addEventListener('click', submitLead);
                    var form = button ? button.closest('form') : document.querySelector('form');
                    if (form) form.addEventListener('submit', submitLead);
                  }

                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', init);
                  } else {
                    init();
                  }
                })();
                </script>
                """.formatted(escapedSlug);
        if (document.body() != null) {
            document.body().append(script);
        } else {
            document.append(script);
        }
        return document.outerHtml();
    }

    /** Verifica se o HTML já possui o contrato público de submissão de leads. */
    private boolean hasLeadSubmissionContract(String html) {
        String lowered = html.toLowerCase(Locale.ROOT);
        return lowered.contains("lead-portal-submission-engagement.v1")
                || (lowered.contains("/api/public/lead-portal/flows/") && lowered.contains("/submission"));
    }

    /** Verifica se a landing possui controles mínimos para captura de nome, e-mail e envio. */
    private boolean hasLeadCaptureControls(Document document) {
        boolean hasName = !document.select("#input-nome, [name=nome], [autocomplete=name]").isEmpty();
        boolean hasEmail = !document.select("#input-email, [name=email], input[type=email], [autocomplete=email]").isEmpty();
        boolean hasSubmit = !document.select("#form-submit, button[type=submit], button").isEmpty();
        return hasName && hasEmail && hasSubmit;
    }

    /** Escapa valor textual para uso seguro dentro de string JavaScript delimitada por aspas simples. */
    private String escapeJavaScriptString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("</script>", "<\\/script>");
    }

    /** Obtém por reflexão o identificador do Facebook Pixel configurado no nicho do experimento. */
    private String resolveFacebookPixelId(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        try {
            Object niche = Experiment.class.getMethod("getNiche").invoke(experiment);
            if (niche == null) {
                return null;
            }
            Object pixelId = niche.getClass().getMethod("getFacebookPixelId").invoke(niche);
            return pixelId instanceof String value && StringUtils.hasText(value) ? value.trim() : null;
        } catch (ReflectiveOperationException ex) {
            log.warn("GeraLanding public landing Facebook Pixel resolution failed (experimentId={})", experiment.getId(), ex);
            return null;
        }
    }

    /** Injeta o snippet do Facebook Pixel quando há pixel configurado e ele ainda não existe no HTML. */
    private String injectFacebookPixel(String html, String facebookPixelId) {
        if (!StringUtils.hasText(html) || !StringUtils.hasText(facebookPixelId)) {
            return html;
        }
        if (html.contains("data-mh-facebook-pixel")) {
            return html;
        }
        String pixelSnippet = """
                <script data-mh-facebook-pixel="true">
                  !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
                  n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
                  n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
                  t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window, document,'script',
                  'https://connect.facebook.net/en_US/fbevents.js');
                  fbq('init', '%s');
                  fbq('track', 'PageView');
                </script>
                <noscript><img height="1" width="1" style="display:none"
                  src="https://www.facebook.com/tr?id=%s&ev=PageView&noscript=1"
                /></noscript>
                """.formatted(facebookPixelId, facebookPixelId).trim();
        if (html.toLowerCase(Locale.ROOT).contains("</head>")) {
            return html.replaceFirst("(?i)</head>", pixelSnippet + "\n</head>");
        }
        if (html.toLowerCase(Locale.ROOT).contains("<body")) {
            return html.replaceFirst("(?i)<body", pixelSnippet + "\n<body");
        }
        return pixelSnippet + "\n" + html;
    }

    /** Converte a URL iframe do Lead Portal na URL standalone usada como destino oficial da campanha. */
    private String resolveStandaloneLandingUrl(String iframeUrl) {
        if (!StringUtils.hasText(iframeUrl)) {
            return null;
        }
        try {
            URI parsed = URI.create(iframeUrl);
            String[] segments = parsed.getPath().split("/");
            String slug = segments.length == 0 ? "" : segments[segments.length - 1];
            if (!StringUtils.hasText(slug)) {
                return null;
            }
            return UriComponentsBuilder.newInstance()
                    .scheme(parsed.getScheme())
                    .host(parsed.getHost())
                    .port(parsed.getPort())
                    .path("/api/flows/{slug}/page")
                    .buildAndExpand(slug)
                    .toUriString();
        } catch (RuntimeException ex) {
            log.warn("GeraLanding public landing standalone URL resolution failed (iframeUrl={})", iframeUrl, ex);
            return null;
        }
    }

    /** Injeta atributos e script de telemetria de comportamento nas seções da landing. */
    private String injectBehaviorTrackingAttributesAndScript(String html) {
        if (!StringUtils.hasText(html) || html.contains("data-mh-funnel-tracking")) {
            return html;
        }
        Document document = Jsoup.parse(html, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);

        for (Element section : document.select("section[data-section-id], section[id], [data-section-id]")) {
            String sectionId = section.hasAttr("data-section-id") ? section.attr("data-section-id") : section.id();
            if (!StringUtils.hasText(sectionId)) {
                continue;
            }
            section.attr("data-track-section", sectionId.trim());
        }

        String script = """
                <script data-mh-funnel-tracking="true">
                (function(){
                  if (window.__mhFunnelTrackingInstalled) return;
                  window.__mhFunnelTrackingInstalled = true;
                  var debugPrefix = '[MH funnel tracking]';
                  window.dataLayer = window.dataLayer || [];

                  function emit(name, payload){
                    var eventPayload = Object.assign({event:name, source:'landing-page-design-preset'}, payload||{});
                    console.debug(debugPrefix, 'emit', eventPayload);
                    window.dataLayer.push(eventPayload);
                  }

                  function initTracking(){
                    console.debug(debugPrefix, 'bootstrap');
                    emit('page_view', {ts: Date.now()});
                    var sections = Array.prototype.slice.call(document.querySelectorAll('[data-track-section]'));
                    console.debug(debugPrefix, 'sections-found', {count: sections.length});
                    var stats = {};
                    sections.forEach(function(node){
                      var id = node.getAttribute('data-track-section');
                      stats[id] = {visibleSince:null, elapsedMs:0};
                    });
                    function flushSection(id, reason){
                      var s = stats[id];
                      if (!s || s.visibleSince === null) return;
                      s.elapsedMs += Date.now() - s.visibleSince;
                      s.visibleSince = null;
                      console.debug(debugPrefix, 'section-flush', {sectionId:id, elapsedMs:s.elapsedMs, reason: reason || 'hidden'});
                      emit('section_view_time', {sectionId:id, elapsedMs:s.elapsedMs, reason: reason || 'hidden'});
                    }
                    var observer = new IntersectionObserver(function(entries){
                      entries.forEach(function(entry){
                        var id = entry.target.getAttribute('data-track-section');
                        if (!id || !stats[id]) return;
                        if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                          if (stats[id].visibleSince === null) {
                            stats[id].visibleSince = Date.now();
                            console.debug(debugPrefix, 'section-visible', {sectionId:id, ts:stats[id].visibleSince});
                            emit('section_view_start', {sectionId:id});
                          }
                        } else {
                          flushSection(id, 'intersection-change');
                        }
                      });
                    }, {threshold:[0,0.5,1]});
                    sections.forEach(function(node){ observer.observe(node); });
                    document.addEventListener('visibilitychange', function(){
                      if (document.hidden) {
                        console.debug(debugPrefix, 'visibility-hidden');
                        Object.keys(stats).forEach(function(id){ flushSection(id, 'tab-hidden'); });
                      }
                    });
                    window.addEventListener('beforeunload', function(){
                      console.debug(debugPrefix, 'beforeunload-flush');
                      Object.keys(stats).forEach(function(id){ flushSection(id, 'before-unload'); });
                    });
                  }

                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', initTracking);
                  } else {
                    initTracking();
                  }
                })();
                </script>
                """;
        if (document.head() != null) {
            document.head().append(script);
        } else {
            document.prepend(script);
        }
        return document.outerHtml();
    }
}
