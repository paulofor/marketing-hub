package com.marketinghub.imagedeliverable.service;

import com.marketinghub.imagedeliverable.ImageDeliverableAccessType;
import com.marketinghub.imagedeliverable.ImageDeliverableItem;
import com.marketinghub.imagedeliverable.ImageDeliverablePackage;
import com.marketinghub.imagedeliverable.ImageDeliverableStatus;
import com.marketinghub.imagedeliverable.dto.CreateImageDeliverablePackageRequest;
import com.marketinghub.imagedeliverable.dto.ImageDeliverableItemRequest;
import com.marketinghub.imagedeliverable.dto.UpdateImageDeliverablePackageRequest;
import com.marketinghub.imagedeliverable.repository.ImageDeliverablePackageRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.model.Lead;
import com.marketinghub.repository.LeadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Handles validation and persistence for image deliverable packages.
 */
@Service
public class ImageDeliverablePackageService {
    private final ImageDeliverablePackageRepository repository;
    private final LeadRepository leadRepository;
    private final AssetRepository assetRepository;

    public ImageDeliverablePackageService(ImageDeliverablePackageRepository repository,
                                          LeadRepository leadRepository,
                                          AssetRepository assetRepository) {
        this.repository = repository;
        this.leadRepository = leadRepository;
        this.assetRepository = assetRepository;
    }

    public List<ImageDeliverablePackage> listAll() {
        return repository.findAllLatestFirst();
    }

    public ImageDeliverablePackage get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Image deliverable package not found: " + id));
    }

    public List<ImageDeliverablePackage> listByLead(UUID leadId) {
        ensureLeadExists(leadId);
        return repository.findByLeadIdOrderByCreatedAtDesc(leadId);
    }

    @Transactional
    public ImageDeliverablePackage create(CreateImageDeliverablePackageRequest request) {
        Lead lead = resolveLead(request.getLeadId());
        Asset inputAsset = resolveAsset(request.getInputAssetId());
        String prompt = normalizePrompt(request.getPrompt());
        ImageDeliverableStatus status = Optional.ofNullable(request.getStatus())
                .orElse(ImageDeliverableStatus.RECEIVED);
        Integer plannedOutputs = normalizePlannedOutputs(request.getPlannedOutputs());
        int freeImages = normalizeFreeImages(request.getFreeImages());

        ImageDeliverablePackage pack = ImageDeliverablePackage.builder()
                .lead(lead)
                .inputAsset(inputAsset)
                .status(status)
                .plannedOutputs(plannedOutputs)
                .freeImages(freeImages)
                .model(StringUtils.hasText(request.getModel()) ? request.getModel() : null)
                .prompt(prompt)
                .build();
        pack.getItems().addAll(buildItems(request.getItems(), pack));
        return repository.save(pack);
    }

    @Transactional
    public ImageDeliverablePackage update(Long id, UpdateImageDeliverablePackageRequest request) {
        ImageDeliverablePackage pack = get(id);
        if (request.getLeadId() != null && !Objects.equals(pack.getLead().getId(), request.getLeadId())) {
            pack.setLead(resolveLead(request.getLeadId()));
        }
        if (request.getInputAssetId() != null) {
            pack.setInputAsset(resolveAsset(request.getInputAssetId()));
        }
        if (request.getStatus() != null) {
            pack.setStatus(request.getStatus());
        }
        if (request.getPlannedOutputs() != null) {
            pack.setPlannedOutputs(normalizePlannedOutputs(request.getPlannedOutputs()));
        }
        if (request.getFreeImages() != null) {
            pack.setFreeImages(normalizeFreeImages(request.getFreeImages()));
        }
        pack.setModel(StringUtils.hasText(request.getModel()) ? request.getModel() : null);
        pack.setPrompt(normalizePrompt(request.getPrompt()));
        if (request.getItems() != null) {
            pack.getItems().clear();
            pack.getItems().addAll(buildItems(request.getItems(), pack));
        }
        return repository.save(pack);
    }

    private Lead resolveLead(UUID leadId) {
        if (leadId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leadId is required");
        }
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lead not found: " + leadId));
    }

    private Asset resolveAsset(Long assetId) {
        if (assetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId is required");
        }
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Asset not found: " + assetId));
    }

    private String normalizePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        return prompt.trim();
    }

    private Integer normalizePlannedOutputs(Integer plannedOutputs) {
        if (plannedOutputs == null) {
            return null;
        }
        if (plannedOutputs <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "plannedOutputs must be greater than zero");
        }
        return plannedOutputs;
    }

    private int normalizeFreeImages(Integer freeImages) {
        if (freeImages == null) {
            return 0;
        }
        if (freeImages < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freeImages cannot be negative");
        }
        return freeImages;
    }

    private List<ImageDeliverableItem> buildItems(List<ImageDeliverableItemRequest> itemRequests,
                                                  ImageDeliverablePackage pack) {
        if (CollectionUtils.isEmpty(itemRequests)) {
            return new ArrayList<>();
        }
        Map<Long, Integer> order = new LinkedHashMap<>();
        Map<Long, ImageDeliverableAccessType> requestedTypes = new HashMap<>();
        for (int i = 0; i < itemRequests.size(); i++) {
            ImageDeliverableItemRequest item = itemRequests.get(i);
            if (item.getAssetId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId is required for each item");
            }
            order.put(item.getAssetId(), i);
            requestedTypes.putIfAbsent(item.getAssetId(),
                    Optional.ofNullable(item.getAccessType()).orElse(ImageDeliverableAccessType.PREMIUM));
        }
        List<Asset> assets = assetRepository.findAllById(order.keySet());
        if (assets.size() != order.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more assets were not found");
        }
        assets.sort(Comparator.comparingInt(a -> order.get(a.getId())));
        List<ImageDeliverableItem> items = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            ImageDeliverableAccessType accessType = requestedTypes.get(asset.getId());
            items.add(ImageDeliverableItem.builder()
                    .packageRef(pack)
                    .asset(asset)
                    .accessType(accessType)
                    .position(i)
                    .build());
        }
        return items;
    }

    private void ensureLeadExists(UUID leadId) {
        if (leadId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leadId is required");
        }
        if (!leadRepository.existsById(leadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId);
        }
    }
}
