package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.Shot;
import java.util.List;
import java.util.Optional;

/**
 * 分镜仓储接口
 */
public interface ShotRepository {
    List<Shot> findByNovelId(Long novelId);
    Optional<Shot> findById(Long id);
    Shot save(Shot shot);
    void deleteById(Long id);
    void deleteByNovelId(Long novelId);
}
