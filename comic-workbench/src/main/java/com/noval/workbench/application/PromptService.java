package com.noval.workbench.application;

import com.noval.workbench.domain.model.Prompt;
import com.noval.workbench.domain.repository.PromptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 提示词应用服务
 */
@Service
public class PromptService {

    private final PromptRepository promptRepository;

    public PromptService(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    public List<Prompt> listAll() {
        return promptRepository.findAll();
    }

    public List<Prompt> listByCategory(String category) {
        if (category == null || category.isEmpty() || "全部".equals(category)) {
            return promptRepository.findAll();
        }
        return promptRepository.findByCategory(category);
    }

    public Optional<Prompt> getById(Long id) {
        return promptRepository.findById(id);
    }

    public Prompt create(Prompt prompt) {
        return promptRepository.save(prompt);
    }

    public Prompt update(Long id, Prompt prompt) {
        return promptRepository.save(Prompt.builder()
                .id(id)
                .title(prompt.getTitle())
                .category(prompt.getCategory())
                .content(prompt.getContent())
                .build());
    }

    public void delete(Long id) {
        promptRepository.deleteById(id);
    }
}
