package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.Asset;
import com.noval.workbench.domain.model.AssetType;
import java.util.List;
import java.util.Optional;

/**
 * 资产仓储接口
 */
public interface AssetRepository {
    List<Asset> findAll();
    List<Asset> findByType(AssetType type);
    List<Asset> findByNovelId(Long novelId);
    Optional<Asset> findById(Long id);
    Asset save(Asset asset);
    void deleteById(Long id);
}
