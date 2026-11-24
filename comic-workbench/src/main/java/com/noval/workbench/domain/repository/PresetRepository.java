package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.Preset;
import java.util.Optional;

/**
 * 预设仓储接口
 */
public interface PresetRepository {
    Optional<Preset> findByNovelId(Long novelId);
    Preset save(Preset preset);
    void deleteByNovelId(Long novelId);
}
