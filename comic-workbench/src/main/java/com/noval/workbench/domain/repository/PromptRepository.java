package com.noval.workbench.domain.repository;

import com.noval.workbench.domain.model.Prompt;
import java.util.List;
import java.util.Optional;

/**
 * 提示词仓储接口
 */
public interface PromptRepository {
    List<Prompt> findAll();
    List<Prompt> findByCategory(String category);
    Optional<Prompt> findById(Long id);
    Prompt save(Prompt prompt);
    void deleteById(Long id);
}
