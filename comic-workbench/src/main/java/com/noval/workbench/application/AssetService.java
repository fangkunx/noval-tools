package com.noval.workbench.application;

import com.noval.workbench.domain.model.Asset;
import com.noval.workbench.domain.model.AssetType;
import com.noval.workbench.domain.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 资产应用服务
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<Asset> listAll() {
        return assetRepository.findAll();
    }

    public List<Asset> listByType(AssetType type) {
        if (type == null) {
            return assetRepository.findAll();
        }
        return assetRepository.findByType(type);
    }

    public List<Asset> listByNovelId(Long novelId) {
        return assetRepository.findByNovelId(novelId);
    }

    public Optional<Asset> getById(Long id) {
        return assetRepository.findById(id);
    }

    public Asset create(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset update(Long id, Asset asset) {
        return assetRepository.save(Asset.builder()
                .id(id)
                .novelId(asset.getNovelId())
                .name(asset.getName())
                .type(asset.getType())
                .style(asset.getStyle())
                .description(asset.getDescription())
                .imageUrl(asset.getImageUrl())
                .build());
    }

    public void delete(Long id) {
        assetRepository.deleteById(id);
    }
}
