package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.ArtStyle;
import java.util.List;
import java.util.Optional;

/**
 * 画风仓储接口
 */
public interface ArtStyleRepository {
    List<ArtStyle> findAll();
    Optional<ArtStyle> findById(Long id);
    ArtStyle save(ArtStyle artStyle);
    void deleteById(Long id);
}
