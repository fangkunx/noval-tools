package com.noval.workbench.application;

import com.noval.workbench.domain.model.Novel;
import com.noval.workbench.domain.repository.NovelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 小说应用服务
 */
@Service
public class NovelService {

    private final NovelRepository novelRepository;

    public NovelService(NovelRepository novelRepository) {
        this.novelRepository = novelRepository;
    }

    public List<Novel> listAll() {
        return novelRepository.findAll();
    }

    public Optional<Novel> getById(Long id) {
        return novelRepository.findById(id);
    }

    public Novel create(Novel novel) {
        return novelRepository.save(novel);
    }

    public Novel update(Long id, Novel novel) {
        return novelRepository.save(Novel.builder()
                .id(id)
                .title(novel.getTitle())
                .content(novel.getContent())
                .source(novel.getSource())
                .vip(novel.isVip())
                .status(novel.getStatus())
                .build());
    }

    public void delete(Long id) {
        novelRepository.deleteById(id);
    }
}
