package com.marketinghub.tiktokadsworker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/** Persiste contas TikTok Ads em arquivo local do módulo independente. */
@Repository
public class TiktokAccountRepository {
    private static final Logger log = LoggerFactory.getLogger(TiktokAccountRepository.class);
    private static final TypeReference<List<TiktokAccount>> ACCOUNT_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path storageFile;

    /** Inicializa o repositório com o caminho de armazenamento configurado. */
    public TiktokAccountRepository(ObjectMapper objectMapper, @Value("${tiktok.storage.file}") String storageFile) {
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.storageFile = Path.of(storageFile);
    }

    /** Lista todas as contas cadastradas em ordem de identificador. */
    public synchronized List<TiktokAccount> findAll() {
        return readAccounts().stream()
                .sorted(Comparator.comparing(TiktokAccount::getId))
                .toList();
    }

    /** Busca uma conta pelo identificador interno. */
    public synchronized Optional<TiktokAccount> findById(Long id) {
        return readAccounts().stream().filter(account -> account.getId().equals(id)).findFirst();
    }

    /** Salva uma conta nova ou existente no arquivo de armazenamento. */
    public synchronized TiktokAccount save(TiktokAccount account) {
        List<TiktokAccount> accounts = new ArrayList<>(readAccounts());
        if (account.getId() == null) {
            long nextId = accounts.stream().map(TiktokAccount::getId).max(Long::compareTo).orElse(0L) + 1L;
            account.setId(nextId);
            accounts.add(account);
        } else {
            accounts.removeIf(item -> item.getId().equals(account.getId()));
            accounts.add(account);
        }
        writeAccounts(accounts);
        return account;
    }

    /** Remove uma conta cadastrada pelo identificador interno. */
    public synchronized void deleteById(Long id) {
        List<TiktokAccount> accounts = new ArrayList<>(readAccounts());
        accounts.removeIf(account -> account.getId().equals(id));
        writeAccounts(accounts);
    }

    /** Lê as contas do arquivo ou retorna uma lista vazia quando ainda não existe armazenamento. */
    private List<TiktokAccount> readAccounts() {
        if (!Files.exists(storageFile)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(storageFile.toFile(), ACCOUNT_LIST_TYPE);
        } catch (IOException ex) {
            log.error("Falha ao ler contas TikTok. modulo=tiktok-ads-worker operacao=readAccounts arquivo={}", storageFile, ex);
            throw new IllegalStateException("Não foi possível ler as contas TikTok.", ex);
        }
    }

    /** Grava as contas no arquivo configurado, criando diretórios quando necessário. */
    private void writeAccounts(List<TiktokAccount> accounts) {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), accounts);
        } catch (IOException ex) {
            log.error("Falha ao gravar contas TikTok. modulo=tiktok-ads-worker operacao=writeAccounts arquivo={}", storageFile, ex);
            throw new IllegalStateException("Não foi possível gravar as contas TikTok.", ex);
        }
    }
}
