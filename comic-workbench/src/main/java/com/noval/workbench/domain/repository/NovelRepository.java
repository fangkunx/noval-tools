package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.Novel;
import java.util.List;
import java.util.Optional;

/**
 * 小说仓储接口
 */
public interface NovelRepository {
    List<Novel> findAll();
    Optional<Novel> findById(Long id);
    Novel save(Novel novel);
    void deleteById(Long id);
}
