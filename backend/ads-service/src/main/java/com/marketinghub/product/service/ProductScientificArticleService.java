package com.marketinghub.product.service;

import com.marketinghub.product.Product;
import com.marketinghub.product.ProductScientificArticle;
import com.marketinghub.product.dto.ProductScientificArticleDto;
import com.marketinghub.product.dto.SaveProductScientificArticleRequest;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.product.ProductScientificArticleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar artigos científicos ligados ao mecanismo comercial de produtos. */
@Service
public class ProductScientificArticleService {
  private static final Logger log = LoggerFactory.getLogger(ProductScientificArticleService.class);

  private final ProductRepository productRepository;
  private final ProductScientificArticleRepository articleRepository;

  /** Inicializa o serviço com repositórios de produto e evidências científicas. */
  public ProductScientificArticleService(
      ProductRepository productRepository, ProductScientificArticleRepository articleRepository) {
    this.productRepository = productRepository;
    this.articleRepository = articleRepository;
  }

  /** Lista artigos científicos cadastrados para o produto informado. */
  @Transactional(readOnly = true)
  public List<ProductScientificArticleDto> listArticles(Long productId) {
    requireProduct(productId);
    return articleRepository.findByProductIdOrderByIdAsc(productId).stream()
        .map(this::toDto)
        .toList();
  }

  /** Cadastra um novo artigo científico para sustentar o mecanismo do produto. */
  @Transactional
  public ProductScientificArticleDto createArticle(
      Long productId, SaveProductScientificArticleRequest request) {
    Product product = requireProduct(productId);
    ProductScientificArticle article = new ProductScientificArticle();
    article.setProduct(product);
    applyRequest(article, request);
    return toDto(articleRepository.save(article));
  }

  /** Atualiza os dados editoriais e comerciais de um artigo científico do produto. */
  @Transactional
  public ProductScientificArticleDto updateArticle(
      Long productId, Long articleId, SaveProductScientificArticleRequest request) {
    requireProduct(productId);
    ProductScientificArticle article =
        articleRepository
            .findById(articleId)
            .filter(item -> item.getProduct().getId().equals(productId))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Artigo científico não encontrado."));
    applyRequest(article, request);
    return toDto(articleRepository.save(article));
  }

  /** Remove um artigo científico do cadastro de mecanismo do produto. */
  @Transactional
  public void deleteArticle(Long productId, Long articleId) {
    requireProduct(productId);
    ProductScientificArticle article =
        articleRepository
            .findById(articleId)
            .filter(item -> item.getProduct().getId().equals(productId))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Artigo científico não encontrado."));
    articleRepository.delete(article);
  }

  /** Busca o produto dono do cadastro científico ou falha com erro HTTP claro. */
  private Product requireProduct(Long productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
  }

  /** Aplica os campos validados da requisição ao artigo científico. */
  private void applyRequest(
      ProductScientificArticle article, SaveProductScientificArticleRequest request) {
    String link = normalizeRequired(request.link(), "Informe o link do artigo.");
    article.setLink(link);
    article.setLinkHash(hashLink(link));
    article.setOriginalTitle(
        normalizeRequired(request.originalTitle(), "Informe o título original do artigo."));
    article.setPortugueseTitle(
        normalizeRequired(request.portugueseTitle(), "Informe o título em português."));
    article.setSummary(normalizeRequired(request.summary(), "Informe o resumo do artigo."));
    article.setMechanismApplication(
        normalizeRequired(
            request.mechanismApplication(),
            "Informe como o artigo se aplica ao mecanismo do produto."));
  }

  /** Normaliza texto obrigatório removendo espaços laterais e bloqueando valor vazio. */
  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return value.trim();
  }

  /** Gera o hash SHA-256 do link para aplicar unicidade sem indexar URL longa. */
  private String hashLink(String link) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(link.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("Falha ao gerar hash SHA-256 de artigo científico de produto.", ex);
      throw new IllegalStateException("Algoritmo SHA-256 indisponível.", ex);
    }
  }

  /** Converte a entidade persistida para o contrato de leitura da API. */
  private ProductScientificArticleDto toDto(ProductScientificArticle article) {
    return new ProductScientificArticleDto(
        article.getId(),
        article.getProduct().getId(),
        article.getLink(),
        article.getOriginalTitle(),
        article.getPortugueseTitle(),
        article.getSummary(),
        article.getMechanismApplication(),
        article.getCreatedAt(),
        article.getUpdatedAt());
  }
}
